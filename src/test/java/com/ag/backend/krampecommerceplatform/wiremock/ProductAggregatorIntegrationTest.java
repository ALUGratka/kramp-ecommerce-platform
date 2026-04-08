package com.ag.backend.krampecommerceplatform.wiremock;

import com.ag.backend.krampecommerceplatform.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import static com.ag.backend.krampecommerceplatform.wiremock.MockServiceHelper.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Product Aggregator Integration Tests - Mock Services")
class ProductAggregatorIntegrationTest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    private RestTemplate restTemplate;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        baseUrl = "http://localhost:" + port + "/api/v1/products";

        // Reset all WireMock servers
        catalogService.resetAll();
        pricingService.resetAll();
        availabilityService.resetAll();
        customerService.resetAll();
    }

    @Test
    @DisplayName("Scenario 1: Polish customer requests product - all services healthy")
    void testHappyPath_PolishMarket() {
        // Given
        String productId = "BRG-001";
        String market = "pl-PL";
        String customerId = "CUST-123";

        setupCatalogService(catalogService, productId, market);
        setupPricingService(pricingService, productId, market);
        setupAvailabilityService(availabilityService, productId, market);
        setupCustomerService(customerService, customerId);

        // When
        long startTime = System.currentTimeMillis();
        ResponseEntity<Product> response = restTemplate.getForEntity(
                baseUrl + "/{productId}/aggregate?market={market}&customerId={customerId}",
                Product.class,
                productId, market, customerId
        );
        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        Product product = response.getBody();
        assertThat(product.isPartial()).isFalse();
        assertThat(product.warnings()).isEmpty();
        assertThat(product.catalog()).isNotNull();
        assertThat(product.catalog().name()).contains("BRG-001");
        assertThat(product.catalog().name()).contains("Część Rolnicza"); // Polish
        assertThat(product.pricing()).isNotNull();
        assertThat(product.availability()).isNotNull();
        assertThat(product.availability().warehouse()).isEqualTo("Warsaw-DC");
        assertThat(product.customer()).isNotNull();

        // Verify parallel execution (should be ~100ms, not 290ms)
        assertThat(duration).isLessThan(250);
        System.out.println("✅ Total aggregation time: " + duration + "ms (expected ~100ms)");
    }

    @Test
    @DisplayName("Scenario 2: German market - Pricing service fails (partial response)")
    void testPartialResponse_PricingFailure() {
        // Given
        String productId = "PUMP-XL";
        String market = "de-DE";

        setupCatalogService(catalogService, productId, market);
        setupPricingServiceFailure(pricingService, productId, market);
        setupAvailabilityService(availabilityService, productId, market);

        // When
        ResponseEntity<Product> response = restTemplate.getForEntity(
                baseUrl + "/{productId}/aggregate?market={market}",
                Product.class,
                productId, market
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Product product = response.getBody();

        assertThat(product.isPartial()).isTrue();
        assertThat(product.warnings()).contains("PRICING service unavailable");
        assertThat(product.catalog()).isNotNull();
        assertThat(product.catalog().name()).contains("Landwirtschaftliches Teil"); // German
        assertThat(product.pricing()).isNull(); // Missing!
        assertThat(product.availability()).isNotNull();
        assertThat(product.availability().warehouse()).isEqualTo("Munich-DC");

        System.out.println("✅ Partial response returned with warnings: " + product.warnings());
    }

    @Test
    @DisplayName("Scenario 3: Catalog service fails - complete failure (502 Bad Gateway)")
    void testCompleteFailure_CatalogDown() {
        // Given
        String productId = "FILTER-42";
        String market = "nl-NL";

        setupCatalogServiceFailure(catalogService, productId, market); // REQUIRED SERVICE FAILS!
        setupPricingService(pricingService, productId, market);
        setupAvailabilityService(availabilityService, productId, market);

        // When/Then
        assertThatThrownBy(() -> restTemplate.getForEntity(
                baseUrl + "/{productId}/aggregate?market={market}",
                Product.class,
                productId, market
        ))
        .isInstanceOf(HttpServerErrorException.class)
        .satisfies(ex -> {
            HttpServerErrorException serverError = (HttpServerErrorException) ex;
            assertThat(serverError.getStatusCode().value()).isEqualTo(502);
            assertThat(serverError.getResponseBodyAsString()).contains("CATALOG");
            System.out.println("✅ Request failed as expected when required service is down");
        });
    }

    @Test
    @DisplayName("Scenario 4: Multiple optional services fail - still returns partial data")
    void testMultipleOptionalFailures() {
        // Given
        String productId = "PART-999";
        String market = "pl-PL";
        String customerId = "CUST-456";

        setupCatalogService(catalogService, productId, market); // OK
        setupPricingServiceFailure(pricingService, productId, market); // FAIL
        setupAvailabilityServiceFailure(availabilityService, productId, market); // FAIL
        setupCustomerServiceFailure(customerService, customerId); // FAIL

        // When
        ResponseEntity<Product> response = restTemplate.getForEntity(
                baseUrl + "/{productId}/aggregate?market={market}&customerId={customerId}",
                Product.class,
                productId, market, customerId
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Product product = response.getBody();

        assertThat(product.isPartial()).isTrue();
        assertThat(product.warnings()).hasSize(3);
        assertThat(product.warnings()).contains(
                "PRICING service unavailable",
                "AVAILABILITY service unavailable",
                "CUSTOMER service unavailable"
        );
        assertThat(product.catalog()).isNotNull(); // Only required service returned data
        assertThat(product.pricing()).isNull();
        assertThat(product.availability()).isNull();
        assertThat(product.customer()).isNull();

        System.out.println("✅ Graceful degradation: " + product.warnings().size() + " services failed");
    }

    @Test
    @DisplayName("Scenario 5: No customer ID provided - still works")
    void testNoCustomerId() {
        // Given
        String productId = "TOOL-123";
        String market = "de-DE";

        setupCatalogService(catalogService, productId, market);
        setupPricingService(pricingService, productId, market);
        setupAvailabilityService(availabilityService, productId, market);
        // No customer service setup - customerId not provided

        // When
        ResponseEntity<Product> response = restTemplate.getForEntity(
                baseUrl + "/{productId}/aggregate?market={market}",
                Product.class,
                productId, market
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Product product = response.getBody();

        assertThat(product.isPartial()).isFalse(); // Customer is optional
        assertThat(product.catalog()).isNotNull();
        assertThat(product.pricing()).isNotNull();
        assertThat(product.availability()).isNotNull();
        assertThat(product.customer()).isNull(); // Expected - no customerId provided

        System.out.println("✅ Product returned without customer data (optional)");
    }

    @Test
    @DisplayName("Scenario 6: Verify realistic latencies are simulated")
    void testRealisticLatencies() {
        // Given
        String productId = "PERF-TEST";
        String market = "pl-PL";

        setupCatalogService(catalogService, productId, market);     // 50ms
        setupPricingService(pricingService, productId, market);     // 80ms
        setupAvailabilityService(availabilityService, productId, market); // 100ms

        // When - run multiple times to verify consistency
        long totalDuration = 0;
        int iterations = 5;

        for (int i = 0; i < iterations; i++) {
            long start = System.currentTimeMillis();
            restTemplate.getForEntity(
                    baseUrl + "/{productId}/aggregate?market={market}",
                    Product.class,
                    productId, market
            );
            totalDuration += (System.currentTimeMillis() - start);
        }

        long avgDuration = totalDuration / iterations;

        // Then - average should be around 100ms (parallel execution)
        // Not 230ms (50+80+100 sequential)
        assertThat(avgDuration).isBetween(80L, 200L);

        System.out.println("✅ Average aggregation time: " + avgDuration + "ms (parallel execution verified)");
    }
}
