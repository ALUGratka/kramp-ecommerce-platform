package com.ag.backend.krampecommerceplatform.service;

import com.ag.backend.krampecommerceplatform.config.ServiceType;
import com.ag.backend.krampecommerceplatform.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AggregateService {

    private final AggregationOrchestrator aggregationOrchestrator;
    private final MetricsService metricsService;

    public Product aggregateProduct(String productId, String market, String customerId) {
        log.info("Aggregating product info: productId={}, market={}, customerId={}", productId, market, customerId);

        return metricsService.recordAggregation(() -> {
            AggregationOutcome outcome = aggregationOrchestrator.aggregate(productId, market, customerId);

            if (outcome.partial()) {
                metricsService.recordPartialResponse();
            }

            log.info("Aggregation complete for product {}. Partial: {}, Warnings: {}",
                    productId, outcome.partial(), outcome.warnings().size());

            return Product.builder()
                    .availability((AvailabilityInfo) outcome.aggregatedData().get(ServiceType.AVAILABILITY))
                    .catalog((CatalogInfo) outcome.aggregatedData().get(ServiceType.CATALOG))
                    .customer((CustomerInfo) outcome.aggregatedData().get(ServiceType.CUSTOMER))
                    .pricing((PricingInfo) outcome.aggregatedData().get(ServiceType.PRICING))
                    .isPartial(outcome.partial())
                    .warnings(outcome.warnings())
                    .build();
        });
    }
}
