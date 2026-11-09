package com.clinic.gateway.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

// P4-8: giới hạn số request theo thuật toán token bucket, mỗi tài khoản một thùng (chưa đăng nhập thì theo IP).
// Lưu trong bộ nhớ vì chỉ chạy một Gateway; chạy nhiều Gateway thì phải chuyển sang Redis để các bản dùng chung thùng
@Component
public class RateLimitFilter implements WebFilter, Ordered {

    static final String LIMIT_HEADER = "X-RateLimit-Limit";
    static final String REMAINING_HEADER = "X-RateLimit-Remaining";
    private static final long SWEEP_EVERY = TimeUnit.MINUTES.toNanos(1);
    private static final long IDLE_AFTER = TimeUnit.MINUTES.toNanos(5);

    private final int capacity;
    private final double tokensPerNano;
    private final LongSupplier clock;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final AtomicLong lastSweep;

    @Autowired
    public RateLimitFilter(@Value("${clinic.rate-limit.capacity}") int capacity,
                           @Value("${clinic.rate-limit.refill-per-second}") double refillPerSecond) {
        this(capacity, refillPerSecond, System::nanoTime);
    }

    RateLimitFilter(int capacity, double refillPerSecond, LongSupplier clock) {
        this.capacity = capacity;
        this.tokensPerNano = refillPerSecond / TimeUnit.SECONDS.toNanos(1);
        this.clock = clock;
        this.lastSweep = new AtomicLong(clock.getAsLong());
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (capacity <= 0 || tokensPerNano <= 0 || HttpMethod.OPTIONS.equals(request.getMethod())
            || request.getPath().pathWithinApplication().value().startsWith("/actuator")) {
            return chain.filter(exchange);
        }
        long now = clock.getAsLong();
        sweepIdle(now);
        Bucket bucket = buckets.computeIfAbsent(keyOf(request), k -> new Bucket(capacity, now));
        long waitNanos;
        int remaining;
        synchronized (bucket) {
            waitNanos = bucket.tryTake(now, capacity, tokensPerNano);
            remaining = (int) bucket.tokens;
        }
        HttpHeaders headers = exchange.getResponse().getHeaders();
        headers.set(LIMIT_HEADER, String.valueOf(capacity));
        headers.set(REMAINING_HEADER, String.valueOf(remaining));
        if (waitNanos > 0) {
            long seconds = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(waitNanos + TimeUnit.SECONDS.toNanos(1) - 1));
            headers.set(HttpHeaders.RETRY_AFTER, String.valueOf(seconds));
            return Mono.error(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                "Gửi quá nhiều yêu cầu, vui lòng thử lại sau " + seconds + " giây"));
        }
        return chain.filter(exchange);
    }

    // Chạy sau TokenAuthFilter nên X-User-Id lúc này là mã lấy từ token, client không tự đặt được
    private static String keyOf(ServerHttpRequest request) {
        String accountId = request.getHeaders().getFirst(TokenAuthFilter.USER_ID);
        if (accountId != null) {
            return "account:" + accountId;
        }
        InetSocketAddress remote = request.getRemoteAddress();
        return "ip:" + (remote != null && remote.getAddress() != null ? remote.getAddress().getHostAddress() : "unknown");
    }

    // Thùng không dùng quá 5 phút đã đầy lại, bỏ đi để bộ nhớ không tăng mãi theo số tài khoản / IP
    private void sweepIdle(long now) {
        long last = lastSweep.get();
        if (now - last < SWEEP_EVERY || !lastSweep.compareAndSet(last, now)) {
            return;
        }
        buckets.values().removeIf(b -> {
            synchronized (b) {
                return now - b.updatedAt > IDLE_AFTER;
            }
        });
    }

    int trackedKeys() {
        return buckets.size();
    }

    // Sau TokenAuthFilter (cần mã tài khoản đã xác minh) và sau CORS (để 429 vẫn kèm header CORS)
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 3;
    }

    private static final class Bucket {
        private double tokens;
        private long updatedAt;

        Bucket(int capacity, long now) {
            this.tokens = capacity;
            this.updatedAt = now;
        }

        // Trả 0 nếu lấy được một lượt; ngược lại trả số nano giây phải chờ tới lượt kế tiếp
        long tryTake(long now, int capacity, double tokensPerNano) {
            tokens = Math.min(capacity, tokens + (now - updatedAt) * tokensPerNano);
            updatedAt = now;
            if (tokens >= 1) {
                tokens -= 1;
                return 0;
            }
            return (long) Math.ceil((1 - tokens) / tokensPerNano);
        }
    }
}
