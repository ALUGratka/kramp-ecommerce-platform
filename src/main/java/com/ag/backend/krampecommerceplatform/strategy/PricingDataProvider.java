package com.ag.backend.krampecommerceplatform.strategy;

import com.ag.backend.krampecommerceplatform.client.PricingClient;
import com.ag.backend.krampecommerceplatform.config.ServiceType;
import com.ag.backend.krampecommerceplatform.model.PricingInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PricingDataProvider implements DataProvider<PricingInfo> {

    private final PricingClient client;

    @Override
    public ServiceType name() {
        return ServiceType.PRICING;
    }

    @Override
    public boolean isRequired() {
        return false;
    }

    @Override
    public PricingInfo provideData(String productId, String market, String customerId) {
        return client.getPricingInfo(productId, market);
    }
}
