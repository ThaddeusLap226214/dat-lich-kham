package com.clinic.scheduling.client;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

@Configuration
public class ClientConfig {

    // Bản thường làm mặc định: Eureka client tự lấy builder mặc định để gọi discovery-server,
    // nếu lấy nhầm bản @LoadBalanced sẽ vòng lặp (muốn tìm Eureka lại phải hỏi Eureka)
    @Bean
    @Primary
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    // Bản gọi theo tên service (http://provider-service); lớp client xin đích danh bằng @LoadBalanced
    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }
}
