package com.ag.backend.krampecommerceplatform;

import com.ag.backend.krampecommerceplatform.config.AppConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppConfig.class)
public class KrampEcommercePlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(KrampEcommercePlatformApplication.class, args);
    }
}
