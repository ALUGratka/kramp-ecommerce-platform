package com.ag.backend.krampecommerceplatform.strategy;

import com.ag.backend.krampecommerceplatform.client.AvailabilityClient;
import com.ag.backend.krampecommerceplatform.config.ServiceType;
import com.ag.backend.krampecommerceplatform.model.AvailabilityInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AvailabilityDataProvider implements DataProvider<AvailabilityInfo> {

    private final AvailabilityClient client;

    @Override
    public ServiceType name() {
        return ServiceType.AVAILABILITY;
    }

    @Override
    public boolean isRequired() {
        return false;
    }

    @Override
    public AvailabilityInfo provideData(String productId, String market, String customerId) {
        return client.getAvailability(productId, market);
    }
}
