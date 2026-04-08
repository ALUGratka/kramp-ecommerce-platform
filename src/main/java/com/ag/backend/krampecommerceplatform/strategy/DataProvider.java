package com.ag.backend.krampecommerceplatform.strategy;

import com.ag.backend.krampecommerceplatform.config.ServiceType;

public interface DataProvider<T> {
    ServiceType name();
    boolean isRequired();
    T provideData(String productId, String market, String customerId);
}
