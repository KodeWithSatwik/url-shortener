package com.urlshortener.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppConfig {

    private String baseUrl = "http://localhost:8080";
    private int defaultTtlDays = 30;
    private ShortCode shortCode = new ShortCode();
    private RateLimit rateLimit = new RateLimit();

    @Getter
    @Setter
    public static class ShortCode {
        private int length = 6;
        private int maxRetries = 5;
    }

    @Getter
    @Setter
    public static class RateLimit {
        private long capacity = 20;
        private long refillTokens = 20;
        private long refillSeconds = 60;
    }
}
