package com.ag.backend.krampecommerceplatform.wiremock;


import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseIntegrationTest {

    protected static WireMockServer catalogService;
    protected static WireMockServer pricingService;
    protected static WireMockServer availabilityService;
    protected static WireMockServer customerService;

    @BeforeAll
    static void startWireMock() {
        // Start 4 separate WireMock servers
        catalogService = new WireMockServer(
                WireMockConfiguration.options()
                        .port(8081)
                        .enableBrowserProxying(false)
        );
        catalogService.start();

        pricingService = new WireMockServer(
                WireMockConfiguration.options()
                        .port(8082)
        );
        pricingService.start();

        availabilityService = new WireMockServer(
                WireMockConfiguration.options()
                        .port(8083)
        );
        availabilityService.start();

        customerService = new WireMockServer(
                WireMockConfiguration.options()
                        .port(8084)
        );
        customerService.start();

        System.out.println("✅ All WireMock servers started");
    }

    @AfterAll
    static void stopWireMock() {
        if (catalogService != null && catalogService.isRunning()) {
            catalogService.stop();
        }
        if (pricingService != null && pricingService.isRunning()) {
            pricingService.stop();
        }
        if (availabilityService != null && availabilityService.isRunning()) {
            availabilityService.stop();
        }
        if (customerService != null && customerService.isRunning()) {
            customerService.stop();
        }
        System.out.println("✅ All WireMock servers stopped");
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Override application.yaml properties to point to WireMock
        registry.add("app.services.catalog.url", () -> "http://localhost:8081");
        registry.add("app.services.pricing.url", () -> "http://localhost:8082");
        registry.add("app.services.availability.url", () -> "http://localhost:8083");
        registry.add("app.services.customer.url", () -> "http://localhost:8084");
    }
}
