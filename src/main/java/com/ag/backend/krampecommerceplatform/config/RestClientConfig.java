package com.ag.backend.krampecommerceplatform.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RestClientConfig {

    @Bean
    public Map<ServiceType, RestClient> restClients(AppConfig config) {
        Map<ServiceType, RestClient> clients = new HashMap<>();

        clients.put(ServiceType.AVAILABILITY, createRestClient(config.services().availability()));
        clients.put(ServiceType.CATALOG, createRestClient(config.services().catalog()));
        clients.put(ServiceType.CUSTOMER, createRestClient(config.services().customer()));
        clients.put(ServiceType.PRICING, createRestClient(config.services().pricing()));

        return clients;
    }

    public RestClient createRestClient(AppConfig.Service service) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(service.connectTimeout());
        factory.setReadTimeout(service.readTimeout());

        return RestClient.builder()
                .baseUrl(service.url())
                .requestFactory(factory)
                .build();
    }
}
