package com.ag.backend.krampecommerceplatform.service;

import com.ag.backend.krampecommerceplatform.config.ServiceType;
import com.ag.backend.krampecommerceplatform.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AggregateService Unit Tests")
class AggregateServiceTest {

    @Mock
    private AggregationOrchestrator aggregationOrchestrator;

    @Mock
    private MetricsService metricsService;

    private AggregateService aggregateService;

    private static final String PRODUCT_ID = "TEST-001";
    private static final String MARKET = "pl-PL";
    private static final String CUSTOMER_ID = "CUST-123";

    @BeforeEach
    void setUp() {
        aggregateService = new AggregateService(aggregationOrchestrator, metricsService);

        lenient().when(metricsService.recordAggregation(any())).thenAnswer(invocation -> {
            Supplier<Product> supplier = invocation.getArgument(0);
            return supplier.get();
        });
    }

    @Test
    @DisplayName("Should build Product from orchestrator outcome")
    void shouldBuildProductFromOrchestratorOutcome() {
        AggregationOutcome outcome = new AggregationOutcome(
                Map.of(
                        ServiceType.CATALOG, new CatalogInfo("Product", "Desc", Map.of(), List.of()),
                        ServiceType.PRICING, new PricingInfo(100.0, 10, 90.0)
                ),
                List.of("PRICING service unavailable")
        );

        when(aggregationOrchestrator.aggregate(PRODUCT_ID, MARKET, CUSTOMER_ID)).thenReturn(outcome);

        Product result = aggregateService.aggregateProduct(PRODUCT_ID, MARKET, CUSTOMER_ID);

        assertThat(result).isNotNull();
        assertThat(result.isPartial()).isTrue();
        assertThat(result.warnings()).containsExactly("PRICING service unavailable");
        verify(aggregationOrchestrator).aggregate(PRODUCT_ID, MARKET, CUSTOMER_ID);
        verify(metricsService).recordPartialResponse();
    }

    @Test
    @DisplayName("Should not record partial response when outcome is complete")
    void shouldNotRecordPartialResponseWhenOutcomeIsComplete() {
        AggregationOutcome outcome = new AggregationOutcome(
                Map.of(
                        ServiceType.CATALOG, new CatalogInfo("Product", "Desc", Map.of(), List.of()),
                        ServiceType.PRICING, new PricingInfo(100.0, 10, 90.0),
                        ServiceType.AVAILABILITY, new AvailabilityInfo(true, "Warsaw-DC", java.time.LocalDate.now())
                ),
                List.of()
        );

        when(aggregationOrchestrator.aggregate(PRODUCT_ID, MARKET, CUSTOMER_ID)).thenReturn(outcome);

        Product result = aggregateService.aggregateProduct(PRODUCT_ID, MARKET, CUSTOMER_ID);

        assertThat(result).isNotNull();
        assertThat(result.isPartial()).isFalse();
        assertThat(result.warnings()).isEmpty();
        verify(metricsService, never()).recordPartialResponse();
    }
}