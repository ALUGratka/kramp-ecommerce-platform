package com.ag.backend.krampecommerceplatform.client;

import com.ag.backend.krampecommerceplatform.config.ServiceType;
import com.ag.backend.krampecommerceplatform.model.CustomerInfo;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class CustomerClient extends BaseRestClient<CustomerInfo> {

    public CustomerClient(Map<ServiceType, RestClient> restClients) {
        super(restClients);
    }

    @Override
    protected ServiceType getServiceType() {
        return ServiceType.CUSTOMER;
    }

    public CustomerInfo getCustomerInfo(String customerId) {
        return getClient().get()
                .uri(uriBuilder -> uriBuilder.path("/customer/{customerId}").build(customerId))
                .retrieve()
                .body(CustomerInfo.class);
    }
}
