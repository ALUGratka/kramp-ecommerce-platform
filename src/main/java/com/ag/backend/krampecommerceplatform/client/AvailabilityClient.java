package com.ag.backend.krampecommerceplatform.client;

import com.ag.backend.krampecommerceplatform.config.ServiceType;
import com.ag.backend.krampecommerceplatform.model.AvailabilityInfo;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class AvailabilityClient extends BaseRestClient<AvailabilityInfo> {

    public AvailabilityClient(Map<ServiceType, RestClient> restClients) {
        super(restClients);
    }

    @Override
    protected ServiceType getServiceType() {
        return ServiceType.AVAILABILITY;
    }

    public AvailabilityInfo getAvailability(String productId, String market) {
        return getClient().get()
                .uri(uriBuilder -> uriBuilder.path("/availability/{productId}/{market}").build(productId, market))
                .retrieve()
                .body(AvailabilityInfo.class);
    }
}
