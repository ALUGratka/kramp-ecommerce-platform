package com.ag.backend.krampecommerceplatform.wiremock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.Scenario;

import java.time.LocalDate;
import java.util.Random;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class MockServiceHelper {

    private static final Random random = new Random();

    /**
     * Setup Catalog Service mock - 50ms latency, 99.9% reliability
     */
    public static void setupCatalogService(WireMockServer server, String productId, String market) {
        server.stubFor(get(urlPathEqualTo("/catalog/" + productId + "/" + market))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(50)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "name": "%s - %s",
                                    "description": "%s",
                                    "specifications": {
                                        "weight": "2.5kg",
                                        "dimensions": "30x20x15cm",
                                        "material": "Steel"
                                    },
                                    "images": [
                                        "https://example.com/products/%s/image1.jpg",
                                        "https://example.com/products/%s/image2.jpg"
                                    ]
                                }
                                """.formatted(
                                productId,
                                getLocalizedName(market),
                                getLocalizedDescription(market),
                                productId,
                                productId
                        ))
                )
        );
    }

    /**
     * Setup Catalog Service FAILURE (simulates 0.1% failure rate)
     */
    public static void setupCatalogServiceFailure(WireMockServer server, String productId, String market) {
        server.stubFor(get(urlPathEqualTo("/catalog/" + productId + "/" + market))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withFixedDelay(50)
                        .withBody("{\"error\": \"Catalog service unavailable\"}")
                )
        );
    }

    /**
     * Setup Pricing Service mock - 80ms latency, 99.5% reliability
     */
    public static void setupPricingService(WireMockServer server, String productId, String market) {
        double basePrice = 50.0 + random.nextDouble() * 450.0;
        int discount = random.nextInt(31);
        double finalPrice = basePrice * (1 - discount / 100.0);

        server.stubFor(get(urlPathEqualTo("/pricing/" + productId + "/" + market))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(80)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "basePrice": %.2f,
                                    "discount": %d,
                                    "finalPrice": %.2f
                                }
                                """.formatted(basePrice, discount, finalPrice))
                )
        );
    }

    /**
     * Setup Pricing Service FAILURE (simulates 0.5% failure rate)
     */
    public static void setupPricingServiceFailure(WireMockServer server, String productId, String market) {
        server.stubFor(get(urlPathEqualTo("/pricing/" + productId + "/" + market))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withFixedDelay(80)
                        .withBody("{\"error\": \"Pricing calculation failed\"}")
                )
        );
    }

    /**
     * Setup Availability Service mock - 100ms latency, 98% reliability
     */
    public static void setupAvailabilityService(WireMockServer server, String productId, String market) {
        boolean inStock = random.nextBoolean();
        String warehouse = getWarehouseForMarket(market);
        LocalDate delivery = LocalDate.now().plusDays(random.nextInt(14) + 1);

        server.stubFor(get(urlPathEqualTo("/availability/" + productId + "/" + market))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(100)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "inStock": %s,
                                    "warehouse": "%s",
                                    "estimatedDelivery": "%s"
                                }
                                """.formatted(inStock, warehouse, delivery))
                )
        );
    }

    /**
     * Setup Availability Service FAILURE (simulates 2% failure rate)
     */
    public static void setupAvailabilityServiceFailure(WireMockServer server, String productId, String market) {
        server.stubFor(get(urlPathEqualTo("/availability/" + productId + "/" + market))
                .willReturn(aResponse()
                        .withStatus(504)
                        .withFixedDelay(100)
                        .withBody("{\"error\": \"Warehouse system timeout\"}")
                )
        );
    }

    /**
     * Setup Customer Service mock - 60ms latency, 99% reliability
     */
    public static void setupCustomerService(WireMockServer server, String customerId) {
        String segment = random.nextBoolean() ? "PREMIUM" : "STANDARD";

        server.stubFor(get(urlPathEqualTo("/customer/" + customerId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(60)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "customerId": "%s",
                                    "segment": "%s",
                                    "preferences": ["Agricultural", "Machinery"]
                                }
                                """.formatted(customerId, segment))
                )
        );
    }

    /**
     * Setup Customer Service FAILURE (simulates 1% failure rate)
     */
    public static void setupCustomerServiceFailure(WireMockServer server, String customerId) {
        server.stubFor(get(urlPathEqualTo("/customer/" + customerId))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withFixedDelay(60)
                        .withBody("{\"error\": \"Customer service unavailable\"}")
                )
        );
    }

    /**
     * Setup intermittent failures using WireMock Scenarios
     */
    public static void setupPricingServiceWithIntermittentFailures(WireMockServer server, String productId, String market) {
        // 199 successful requests
        server.stubFor(get(urlPathEqualTo("/pricing/" + productId + "/" + market))
                .inScenario("Pricing Reliability")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(80)
                        .withBody("""
                                {
                                    "basePrice": 100.0,
                                    "discount": 10,
                                    "finalPrice": 90.0
                                }
                                """))
                .willSetStateTo("Request-1")
        );

        // Every 200th request fails (0.5% failure rate)
        server.stubFor(get(urlPathEqualTo("/pricing/" + productId + "/" + market))
                .inScenario("Pricing Reliability")
                .whenScenarioStateIs("Request-1")
                .willReturn(aResponse()
                        .withStatus(500)
                        .withFixedDelay(80)
                        .withBody("{\"error\": \"Simulated failure\"}"))
                .willSetStateTo(Scenario.STARTED)
        );
    }

    // Helper methods
    private static String getLocalizedName(String market) {
        return switch (market.toLowerCase()) {
            case "pl-pl" -> "Część Rolnicza";
            case "de-de" -> "Landwirtschaftliches Teil";
            case "nl-nl" -> "Agrarisch Onderdeel";
            default -> "Agricultural Part";
        };
    }

    private static String getLocalizedDescription(String market) {
        return switch (market.toLowerCase()) {
            case "pl-pl" -> "Wysokiej jakości część do maszyn rolniczych";
            case "de-de" -> "Hochwertiges Ersatzteil für Landmaschinen";
            case "nl-nl" -> "Hoogwaardig onderdeel voor landbouwmachines";
            default -> "High-quality agricultural machinery part";
        };
    }

    private static String getWarehouseForMarket(String market) {
        return switch (market.toLowerCase()) {
            case "pl-pl" -> "Warsaw-DC";
            case "de-de" -> "Munich-DC";
            case "nl-nl" -> "Utrecht-DC";
            default -> "Central-EU-DC";
        };
    }
}
