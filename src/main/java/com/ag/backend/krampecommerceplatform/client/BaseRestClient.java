package com.ag.backend.krampecommerceplatform.client;

import com.ag.backend.krampecommerceplatform.config.ServiceType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestClient;

import java.util.Map;

@RequiredArgsConstructor
public abstract class BaseRestClient<T> {

    protected final Map<ServiceType, RestClient> restClients;

    protected abstract ServiceType getServiceType();

    protected RestClient getClient() {
        return restClients.get(getServiceType());
    }
}
