package com.ag.backend.krampecommerceplatform.model;

import com.ag.backend.krampecommerceplatform.config.ServiceType;

import java.util.List;
import java.util.Map;

public record AggregationOutcome(
        Map<ServiceType, Object> aggregatedData,
        List<String> warnings
) {
    public boolean partial() {
        return !warnings.isEmpty();
    }
}
