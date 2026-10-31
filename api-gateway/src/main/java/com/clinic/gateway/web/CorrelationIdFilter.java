package com.clinic.gateway.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.UUID;

// Mã truy vết request (P4-7): mỗi request vào Gateway nhận một mã mới (bỏ mã client tự gửi), gắn vào header
// chuyển xuống service, trả lại trong response và ghi vào log. WebFilter chạy cho cả route chuyển tiếp lẫn API ghép
@Component
public class CorrelationIdFilter implements WebFilter, Ordered {

    public static final String HEADER = "X-Correlation-Id";
    private static final String CONTEXT_KEY = "correlationId";
    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String id = UUID.randomUUID().toString();
        ServerWebExchange mutated = exchange.mutate()
            .request(exchange.getRequest().mutate().headers(h -> h.set(HEADER, id)).build())
            .build();
        // Đặt lúc sắp gửi response để ghi đè mã service phía sau trả về (cùng giá trị), tránh header bị lặp
        mutated.getResponse().beforeCommit(() -> {
            mutated.getResponse().getHeaders().set(HEADER, id);
            return Mono.empty();
        });
        String path = exchange.getRequest().getPath().value();
        return chain.filter(mutated)
            .doFinally(signal -> {
                if (!path.startsWith("/actuator")) {
                    log.info("[{}] {} {} -> {}", id, exchange.getRequest().getMethod(), path,
                        mutated.getResponse().getStatusCode());
                }
            })
            .contextWrite(Context.of(CONTEXT_KEY, id));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    // P4-14: WebClient của API ghép lấy mã từ Reactor Context của request đang xử lý và gửi tiếp cho service
    public static ExchangeFilterFunction forwardToDownstream() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> Mono.deferContextual(ctx ->
            Mono.just(ctx.hasKey(CONTEXT_KEY)
                ? ClientRequest.from(request).header(HEADER, ctx.<String>get(CONTEXT_KEY)).build()
                : request)));
    }
}
