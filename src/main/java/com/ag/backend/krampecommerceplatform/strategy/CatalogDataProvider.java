package com.ag.backend.krampecommerceplatform.strategy;

import com.ag.backend.krampecommerceplatform.client.CatalogClient;
import com.ag.backend.krampecommerceplatform.config.ServiceType;
import com.ag.backend.krampecommerceplatform.model.CatalogInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CatalogDataProvider implements DataProvider<CatalogInfo> {

    private final CatalogClient client;

    @Override
    public ServiceType name() {
        return ServiceType.CATALOG;
    }

    @Override
    public boolean isRequired() {
        return true;
    }

    @Override
    public CatalogInfo provideData(String productId, String market, String customerId) {
        return client.getCatalogInfo(productId, market);
    }
}
