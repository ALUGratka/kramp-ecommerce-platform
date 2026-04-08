package com.ag.backend.krampecommerceplatform.service;

import com.ag.backend.krampecommerceplatform.config.AppConfig;
import com.ag.backend.krampecommerceplatform.config.ServiceType;
import com.ag.backend.krampecommerceplatform.exception.AggregationTimeoutException;
import com.ag.backend.krampecommerceplatform.exception.ExternalServiceUnavailableException;
import com.ag.backend.krampecommerceplatform.model.AggregationOutcome;
import com.ag.backend.krampecommerceplatform.model.AvailabilityInfo;
import com.ag.backend.krampecommerceplatform.model.CatalogInfo;
import com.ag.backend.krampecommerceplatform.model.CustomerInfo;
import com.ag.backend.krampecommerceplatform.model.PricingInfo;
import com.ag.backend.krampecommerceplatform.strategy.DataProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AggregationOrchestrator Unit Tests")
class AggregationOrchestratorTest {

    @Mock private DataProvider<CatalogInfo> catalogProvider;
    @Mock private DataProvider<PricingInfo> pricingProvider;
    @Mock private DataProvider<AvailabilityInfo> availabilityProvider;
    @Mock private DataProvider<CustomerInfo> customerProvider;
    @Mock private MetricsService metricsService;

    private AggregationOrchestrator orchestrator;
    private ExecutorService asyncExecutor;

    private static final String PRODUCT_ID = "TEST-001";
    private static final String MARKET = "pl-PL";
    private static final String CUSTOMER_ID = "CUST-123";

    @BeforeEach
    void setUp() {
        asyncExecutor = Executors.newFixedThreadPool(4);

        AppConfig config = new AppConfig(
                new AppConfig.Aggregation(Duration.ofSeconds(1)),
                null,
                null
        );

        List<DataProvider<?>> providers = List.of(
                catalogProvider,
                pricingProvider,
                availabilityProvider,
                customerProvider
        );

        orchestrator = new AggregationOrchestrator(providers, asyncExecutor, metricsService, config);

        lenient().when(catalogProvider.name()).thenReturn(ServiceType.CATALOG);
        lenient().when(pricingProvider.name()).thenReturn(ServiceType.PRICING);
        lenient().when(availabilityProvider.name()).thenReturn(ServiceType.AVAILABILITY);
        lenient().when(customerProvider.name()).thenReturn(ServiceType.CUSTOMER);

        lenient().when(catalogProvider.isRequired()).thenReturn(true);
        lenient().when(pricingProvider.isRequired()).thenReturn(false);
        lenient().when(availabilityProvider.isRequired()).thenReturn(false);
        lenient().when(customerProvider.isRequired()).thenReturn(false);

        lenient().when(metricsService.startTimer()).thenReturn(System.currentTimeMillis());
        lenient().when(metricsService.calculateDuration(org.mockito.ArgumentMatchers.anyLong())).thenReturn(50L);
    }

    @AfterEach
    void tearDown() {
        if (asyncExecutor != null) {
            asyncExecutor.shutdownNow();
        }
    }

    @Test
    @DisplayName("Should aggregate all service results successfully")
    void shouldAggregateAllServiceResultsSuccessfully() {
        when(catalogProvider.provideData(PRODUCT_ID, MARKET, CUSTOMER_ID))
                .thenReturn(new CatalogInfo("Product", "Desc", Map.of(), List.of()));
        when(pricingProvider.provideData(PRODUCT_ID, MARKET, CUSTOMER_ID))
                .thenReturn(new PricingInfo(100.0, 10, 90.0));
        when(availabilityProvider.provideData(PRODUCT_ID, MARKET, CUSTOMER_ID))
                .thenReturn(new AvailabilityInfo(true, "Warsaw-DC", LocalDate.now().plusDays(3)));
        when(customerProvider.provideData(PRODUCT_ID, MARKET, CUSTOMER_ID))
                .thenReturn(new CustomerInfo("CUST-123", "PREMIUM", List.of("Agricultural")));

        AggregationOutcome outcome = orchestrator.aggregate(PRODUCT_ID, MARKET, CUSTOMER_ID);

        assertThat(outcome.partial()).isFalse();
        assertThat(outcome.warnings()).isEmpty();
        assertThat(outcome.aggregatedData()).containsKeys(
                ServiceType.CATALOG,
                ServiceType.PRICING,
                ServiceType.AVAILABILITY,
                ServiceType.CUSTOMER
        );
    }

    @Test
    @DisplayName("Should return partial outcome when optional service fails")
    void shouldReturnPartialOutcomeWhenOptionalServiceFails() {
        when(catalogProvider.provideData(PRODUCT_ID, MARKET, CUSTOMER_ID))
                .thenReturn(new CatalogInfo("Product", "Desc", Map.of(), List.of()));
        when(pricingProvider.provideData(PRODUCT_ID, MARKET, CUSTOMER_ID))
                .thenThrow(new RuntimeException("Pricing down"));
        when(availabilityProvider.provideData(PRODUCT_ID, MARKET, CUSTOMER_ID))
                .thenReturn(new AvailabilityInfo(true, "Warsaw-DC", LocalDate.now().plusDays(3)));
        when(customerProvider.provideData(PRODUCT_ID, MARKET, CUSTOMER_ID))
                .thenReturn(new CustomerInfo("CUST-123", "PREMIUM", List.of("Agricultural")));

        AggregationOutcome outcome = orchestrator.aggregate(PRODUCT_ID, MARKET, CUSTOMER_ID);

        assertThat(outcome.partial()).isTrue();
        assertThat(outcome.warnings()).containsExactly("PRICING service unavailable");
        assertThat(outcome.aggregatedData()).doesNotContainKey(ServiceType.PRICING);
    }

    @Test
    @DisplayName("Should fail fast when required Catalog service fails")
    void shouldFailFastWhenRequiredCatalogFails() {
        when(catalogProvider.provideData(PRODUCT_ID, MARKET, CUSTOMER_ID))
                .thenThrow(new RuntimeException("Catalog service down"));

        assertThatThrownBy(() -> orchestrator.aggregate(PRODUCT_ID, MARKET, CUSTOMER_ID))
                .isInstanceOf(ExternalServiceUnavailableException.class)
                .hasMessageContaining("CATALOG")
                .hasMessageContaining(PRODUCT_ID);
    }

    @Test
    @DisplayName("Should fail with timeout when aggregation exceeds global timeout")
    void shouldFailWithTimeoutWhenAggregationExceedsGlobalTimeout() {
        AppConfig timeoutConfig = new AppConfig(
                new AppConfig.Aggregation(Duration.ofMillis(100)),
                null,
                null
        );

        ExecutorService slowExecutor = Executors.newFixedThreadPool(1);
        AggregationOrchestrator timeoutOrchestrator = new AggregationOrchestrator(
                List.of(catalogProvider),
                slowExecutor,
                metricsService,
                timeoutConfig
        );

        lenient().when(catalogProvider.name()).thenReturn(ServiceType.CATALOG);
        lenient().when(catalogProvider.isRequired()).thenReturn(true);

        when(catalogProvider.provideData(PRODUCT_ID, MARKET, CUSTOMER_ID))
                .thenAnswer(invocation -> {
                    Thread.sleep(300);
                    return new CatalogInfo("Product", "Desc", Map.of(), List.of());
                });

        try {
            assertThatThrownBy(() -> timeoutOrchestrator.aggregate(PRODUCT_ID, MARKET, CUSTOMER_ID))
                    .isInstanceOf(AggregationTimeoutException.class)
                    .hasMessageContaining(PRODUCT_ID);
        } finally {
            slowExecutor.shutdownNow();
        }
    }

    @Test
    @DisplayName("Should handle null customerId as optional and return partial/complete outcome accordingly")
    void shouldHandleNullCustomerId() {
        when(catalogProvider.provideData(PRODUCT_ID, MARKET, null))
                .thenReturn(new CatalogInfo("Product", "Desc", Map.of(), List.of()));
        when(pricingProvider.provideData(PRODUCT_ID, MARKET, null))
                .thenReturn(new PricingInfo(100.0, 10, 90.0));
        when(availabilityProvider.provideData(PRODUCT_ID, MARKET, null))
                .thenReturn(new AvailabilityInfo(true, "Warsaw-DC", LocalDate.now().plusDays(3)));
        when(customerProvider.provideData(PRODUCT_ID, MARKET, null))
                .thenReturn(null);

        AggregationOutcome outcome = orchestrator.aggregate(PRODUCT_ID, MARKET, null);

        assertThat(outcome.partial()).isFalse();
        assertThat(outcome.warnings()).isEmpty();
        assertThat(outcome.aggregatedData()).doesNotContainKey(ServiceType.CUSTOMER);
    }
}