package com.ag.backend.krampecommerceplatform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app")
public record AppConfig(
        Aggregation aggregation,
        Services services,
        Async async
) {

    public record Aggregation(
            Duration timeout
    ) {}

    public record Services(
            Service availability,
            Service catalog,
            Service customer,
            Service pricing
    ) {}

    public record Service(
            String url,
            Duration connectTimeout,
            Duration readTimeout
    ) {}

    public record Async(
            ExecutorService executorService
    ) {}

    public record ExecutorService(
            boolean virtualThreads,
            Integer parallelism,
            Integer shutdownAwaitSeconds
    ) {}
}
