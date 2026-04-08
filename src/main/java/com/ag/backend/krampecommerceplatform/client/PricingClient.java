package com.ag.backend.krampecommerceplatform.client;

import com.ag.backend.krampecommerceplatform.config.ServiceType;
import com.ag.backend.krampecommerceplatform.model.PricingInfo;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class PricingClient extends BaseRestClient<PricingInfo> {

    public PricingClient(Map<ServiceType, RestClient> restClients) {
        super(restClients);
    }

    @Override
    protected ServiceType getServiceType() {
        return ServiceType.PRICING;
    }

    public PricingInfo getPricingInfo(String productId, String market) {
        return getClient().get()
                .uri(uriBuilder -> uriBuilder.path("/pricing/{productId}/{market}").build(productId, market))
                .retrieve()
                .body(PricingInfo.class);
    }
}
