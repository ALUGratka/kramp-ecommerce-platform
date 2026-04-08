package com.ag.backend.krampecommerceplatform.model;

import lombok.Builder;

import java.util.List;

@Builder
public record Product(
        AvailabilityInfo availability,
        CatalogInfo catalog,
        CustomerInfo customer,
        PricingInfo pricing,
        boolean isPartial,
        List<String> warnings
) {
}
