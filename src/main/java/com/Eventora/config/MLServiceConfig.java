package com.Eventora.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "ml.service")
@Data
public class MLServiceConfig {
    private String url = "http://localhost:5000";
    private Integer connectionTimeout = 10000; // 10 seconds
    private Integer readTimeout = 30000; // 30 seconds
    private Boolean enabled = true;
    private String modelName = "event_success_v2";
    private String modelVersion = "2.0.0";
    private Integer cacheDurationHours = 24; // Cache predictions for 24 hours

    @Bean
    public RestTemplate mlRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectionTimeout);
        factory.setReadTimeout(readTimeout);
        return new RestTemplate(factory);
    }
}