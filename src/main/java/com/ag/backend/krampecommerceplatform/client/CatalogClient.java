package com.ag.backend.krampecommerceplatform.client;

import com.ag.backend.krampecommerceplatform.config.ServiceType;
import com.ag.backend.krampecommerceplatform.model.CatalogInfo;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class CatalogClient extends BaseRestClient<CatalogInfo> {

    public CatalogClient(Map<ServiceType, RestClient> restClients) {
        super(restClients);
    }

    @Override
    protected ServiceType getServiceType() {
        return ServiceType.CATALOG;
    }

    public CatalogInfo getCatalogInfo(String productId, String market) {
        return getClient().get()
                .uri(uriBuilder -> uriBuilder.path("/catalog/{productId}/{market}").build(productId, market))
                .retrieve()
                .body(CatalogInfo.class);
    }
}
