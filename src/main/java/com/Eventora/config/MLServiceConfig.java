package com.Eventora.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "ml.service")
@Data
public class MLServiceConfig {
    private String url;
    private Integer connectionTimeout;
    private Integer readTimeout;
    private Boolean enabled;
    private String modelName;
    private String modelVersion;
    private Integer cacheDurationHours;
    @Bean
    public RestTemplate mlRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectionTimeout);
        factory.setReadTimeout(readTimeout);
        return new RestTemplate(factory);
    }


    @Configuration
    public class MongoConfig {

        @Bean
        public MongoTemplate mongoTemplate(MongoDatabaseFactory factory) {
            return new MongoTemplate(factory);
        }
    }

}