package com.ag.backend.krampecommerceplatform.strategy;

import com.ag.backend.krampecommerceplatform.client.CustomerClient;
import com.ag.backend.krampecommerceplatform.config.ServiceType;
import com.ag.backend.krampecommerceplatform.model.CustomerInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomerDataProvider implements DataProvider<CustomerInfo> {

    private final CustomerClient client;

    @Override
    public ServiceType name() {
        return ServiceType.CUSTOMER;
    }

    @Override
    public boolean isRequired() {
        return false;
    }

    @Override
    public CustomerInfo provideData(String productId, String market, String customerId) {
        if (customerId == null) {
            return null;
        }
        return client.getCustomerInfo(customerId);
    }
}
