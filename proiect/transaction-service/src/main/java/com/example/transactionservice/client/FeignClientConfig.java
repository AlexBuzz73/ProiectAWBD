package com.example.transactionservice.client;

import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignClientConfig {

    @Bean
    public RequestInterceptor feignAuthInterceptor() {
        return new FeignAuthInterceptor();
    }

    @Bean
    public ErrorDecoder feignErrorDecoder() {
        return new CustomFeignErrorDecoder();
    }
}
