package com.ag.backend.krampecommerceplatform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class AsyncConfig {

    @Bean(destroyMethod = "close")
    public ExecutorService executorService(AppConfig config) {
        AppConfig.ExecutorService executorConfig = config.async().executorService();

        if (executorConfig.virtualThreads()) {
            return Executors.newVirtualThreadPerTaskExecutor();
        }
        return Executors.newFixedThreadPool(executorConfig.parallelism());
    }
}
