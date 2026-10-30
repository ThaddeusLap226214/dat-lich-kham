package com.clinic.scheduling.client;

import com.clinic.scheduling.common.CorrelationIdFilter;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class ClientConfig {

    // Bản thường làm mặc định: Eureka client tự lấy builder mặc định để gọi discovery-server,
    // nếu lấy nhầm bản @LoadBalanced sẽ vòng lặp (muốn tìm Eureka lại phải hỏi Eureka)
    @Bean
    @Primary
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    // Bản gọi theo tên service (http://provider-service); lớp client xin đích danh bằng @LoadBalanced.
    // Chờ tối đa clinic.client-timeout (dùng chung cả nhóm) và chuyển tiếp mã truy vết của request đang xử lý
    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder(@Value("${clinic.client-timeout}") Duration timeout) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(timeout)
            .build());
        requestFactory.setReadTimeout(timeout);
        return RestClient.builder()
            .requestFactory(requestFactory)
            .requestInterceptor((request, body, execution) -> {
                String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
                if (correlationId != null) {
                    request.getHeaders().set(CorrelationIdFilter.HEADER, correlationId);
                }
                return execution.execute(request, body);
            });
    }
}
