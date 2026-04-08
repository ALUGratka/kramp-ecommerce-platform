package com.ag.backend.krampecommerceplatform.service;

import com.ag.backend.krampecommerceplatform.config.ServiceType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MetricsService Unit Tests")
class MetricsServiceTest {

    private MeterRegistry meterRegistry;
    private MetricsService metricsService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricsService = new MetricsService(meterRegistry);
    }

    @Nested
    @DisplayName("recordAggregation() tests")
    class RecordAggregationTests {

        @Test
        @DisplayName("Should record successful aggregation with timer")
        void shouldRecordSuccessfulAggregation() {
            // Given
            String expectedResult = "test-product";

            // When
            String result = metricsService.recordAggregation(() -> {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return expectedResult;
            });

            // Then
            assertThat(result).isEqualTo(expectedResult);

            Timer timer = meterRegistry.find("product.aggregation.time")
                    .tag("service", "aggregator")
                    .timer();

            assertThat(timer).isNotNull();
            assertThat(timer.count()).isEqualTo(1);
            assertThat(timer.totalTime(TimeUnit.MILLISECONDS)).isGreaterThanOrEqualTo(50);
        }

        @Test
        @DisplayName("Should record multiple aggregations")
        void shouldRecordMultipleAggregations() {
            // When
            metricsService.recordAggregation(() -> "result1");
            metricsService.recordAggregation(() -> "result2");
            metricsService.recordAggregation(() -> "result3");

            // Then
            Timer timer = meterRegistry.find("product.aggregation.time").timer();
            assertThat(timer).isNotNull();
            assertThat(timer.count()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should propagate exceptions from aggregation operation")
        void shouldPropagateExceptions() {
            // When/Then
            assertThatThrownBy(() ->
                    metricsService.recordAggregation(() -> {
                        throw new RuntimeException("Test exception");
                    })
            )
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Test exception");

            // Timer should still record the failed attempt
            Timer timer = meterRegistry.find("product.aggregation.time").timer();
            assertThat(timer).isNotNull();
            assertThat(timer.count()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("recordPartialResponse() tests")
    class RecordPartialResponseTests {

        @Test
        @DisplayName("Should increment partial response counter")
        void shouldIncrementPartialResponseCounter() {
            // When
            metricsService.recordPartialResponse();

            // Then
            Counter counter = meterRegistry.find("product.aggregation.partial")
                    .tag("service", "aggregator")
                    .counter();

            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should increment counter multiple times")
        void shouldIncrementCounterMultipleTimes() {
            // When
            metricsService.recordPartialResponse();
            metricsService.recordPartialResponse();
            metricsService.recordPartialResponse();

            // Then
            // FIX: Use find() with proper tags
            Counter counter = meterRegistry.find("product.aggregation.partial")
                    .tag("service", "aggregator")
                    .counter();

            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(3.0);
        }
    }

    @Nested
    @DisplayName("recordExternalServiceCall() tests")
    class RecordExternalServiceCallTests {

        @Test
        @DisplayName("Should record successful service call")
        void shouldRecordSuccessfulServiceCall() {
            // When
            metricsService.recordExternalServiceCall(ServiceType.CATALOG, true, 150);

            // Then
            Timer timer = meterRegistry.find("external.service.call")
                    .tag("service", "catalog")
                    .tag("status", "success")
                    .timer();

            assertThat(timer).isNotNull();
            assertThat(timer.count()).isEqualTo(1);
            assertThat(timer.totalTime(TimeUnit.MILLISECONDS)).isGreaterThanOrEqualTo(150);
        }

        @Test
        @DisplayName("Should record failed service call and increment failure counter")
        void shouldRecordFailedServiceCall() {
            // When
            metricsService.recordExternalServiceCall(ServiceType.PRICING, false, 200);

            // Then
            Timer timer = meterRegistry.find("external.service.call")
                    .tag("service", "pricing")
                    .tag("status", "failure")
                    .timer();

            assertThat(timer).isNotNull();
            assertThat(timer.count()).isEqualTo(1);
            assertThat(timer.totalTime(TimeUnit.MILLISECONDS)).isGreaterThanOrEqualTo(200);

            // Should also increment failure counter
            Counter failureCounter = meterRegistry.find("external.service.failures")
                    .tag("service", "pricing")
                    .tag("required", "false")  // FIX: Added required tag
                    .counter();

            assertThat(failureCounter).isNotNull();
            assertThat(failureCounter.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should record calls for different services separately")
        void shouldRecordDifferentServicesSeparately() {
            // When
            metricsService.recordExternalServiceCall(ServiceType.CATALOG, true, 50);
            metricsService.recordExternalServiceCall(ServiceType.PRICING, true, 80);
            metricsService.recordExternalServiceCall(ServiceType.AVAILABILITY, true, 100);

            // Then
            Timer catalogTimer = meterRegistry.find("external.service.call")
                    .tag("service", "catalog")
                    .timer();
            Timer pricingTimer = meterRegistry.find("external.service.call")
                    .tag("service", "pricing")
                    .timer();
            Timer availabilityTimer = meterRegistry.find("external.service.call")
                    .tag("service", "availability")
                    .timer();

            assertThat(catalogTimer.count()).isEqualTo(1);
            assertThat(pricingTimer.count()).isEqualTo(1);
            assertThat(availabilityTimer.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should track success and failure separately for same service")
        void shouldTrackSuccessAndFailureSeparately() {
            // When
            metricsService.recordExternalServiceCall(ServiceType.PRICING, true, 50);
            metricsService.recordExternalServiceCall(ServiceType.PRICING, true, 60);
            metricsService.recordExternalServiceCall(ServiceType.PRICING, false, 70);

            // Then
            Timer successTimer = meterRegistry.find("external.service.call")
                    .tag("service", "pricing")
                    .tag("status", "success")
                    .timer();

            Timer failureTimer = meterRegistry.find("external.service.call")
                    .tag("service", "pricing")
                    .tag("status", "failure")
                    .timer();

            assertThat(successTimer.count()).isEqualTo(2);
            assertThat(failureTimer.count()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("recordExternalServiceFailure() tests")
    class RecordExternalServiceFailureTests {

        @Test
        @DisplayName("Should record failure for required service")
        void shouldRecordFailureForRequiredService() {
            // When
            metricsService.recordExternalServiceFailure(ServiceType.CATALOG, true);

            // Then
            Counter counter = meterRegistry.find("external.service.failures")
                    .tag("service", "catalog")
                    .tag("required", "true")
                    .counter();

            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should record failure for optional service")
        void shouldRecordFailureForOptionalService() {
            // When
            metricsService.recordExternalServiceFailure(ServiceType.PRICING, false);

            // Then
            Counter counter = meterRegistry.find("external.service.failures")
                    .tag("service", "pricing")
                    .tag("required", "false")
                    .counter();

            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should track multiple failures for different services")
        void shouldTrackMultipleFailuresForDifferentServices() {
            // When
            metricsService.recordExternalServiceFailure(ServiceType.CATALOG, true);
            metricsService.recordExternalServiceFailure(ServiceType.PRICING, false);
            metricsService.recordExternalServiceFailure(ServiceType.AVAILABILITY, false);
            metricsService.recordExternalServiceFailure(ServiceType.PRICING, false); // Second pricing failure

            // Then
            double catalogFailures = meterRegistry.counter("external.service.failures",
                    "service", "catalog", "required", "true").count();
            double pricingFailures = meterRegistry.counter("external.service.failures",
                    "service", "pricing", "required", "false").count();
            double availabilityFailures = meterRegistry.counter("external.service.failures",
                    "service", "availability", "required", "false").count();

            assertThat(catalogFailures).isEqualTo(1.0);
            assertThat(pricingFailures).isEqualTo(2.0);
            assertThat(availabilityFailures).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should distinguish between required and optional failures for same service")
        void shouldDistinguishRequiredAndOptionalFailures() {
            // When
            metricsService.recordExternalServiceFailure(ServiceType.PRICING, true);
            metricsService.recordExternalServiceFailure(ServiceType.PRICING, false);

            // Then
            double requiredFailures = meterRegistry.counter("external.service.failures",
                    "service", "pricing", "required", "true").count();
            double optionalFailures = meterRegistry.counter("external.service.failures",
                    "service", "pricing", "required", "false").count();

            assertThat(requiredFailures).isEqualTo(1.0);
            assertThat(optionalFailures).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("Timer utility methods tests")
    class TimerUtilityTests {

        @Test
        @DisplayName("Should start timer and return current time")
        void shouldStartTimer() {
            // Given
            long beforeStart = System.currentTimeMillis();

            // When
            long startTime = metricsService.startTimer();

            // Then
            long afterStart = System.currentTimeMillis();
            assertThat(startTime).isBetween(beforeStart, afterStart);
        }

        @Test
        @DisplayName("Should calculate duration correctly")
        void shouldCalculateDurationCorrectly() {
            // Given
            long startTime = metricsService.startTimer();

            // When
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            long duration = metricsService.calculateDuration(startTime);

            // Then
            assertThat(duration).isGreaterThanOrEqualTo(100).isLessThan(200);
        }

        @Test
        @DisplayName("Should return zero duration for same time")
        void shouldReturnZeroDurationForSameTime() {
            // Given
            long startTime = System.currentTimeMillis();

            // When
            long duration = metricsService.calculateDuration(startTime);

            // Then
            assertThat(duration).isGreaterThanOrEqualTo(0).isLessThan(10);
        }

        @Test
        @DisplayName("Should calculate duration for longer operations")
        void shouldCalculateDurationForLongerOperations() {
            // Given
            long startTime = metricsService.startTimer();

            // When
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            long duration = metricsService.calculateDuration(startTime);

            // Then
            assertThat(duration).isGreaterThanOrEqualTo(250).isLessThan(350);
        }
    }

    @Nested
    @DisplayName("Integration scenarios")
    class IntegrationScenarios {

        @Test
        @DisplayName("Should record complete aggregation flow with mixed success/failure")
        void shouldRecordCompleteAggregationFlow() {
            // Simulate real aggregation scenario
            metricsService.recordAggregation(() -> {
                // Simulate calls to different services
                long start1 = metricsService.startTimer();
                metricsService.recordExternalServiceCall(ServiceType.CATALOG, true,
                        metricsService.calculateDuration(start1));

                long start2 = metricsService.startTimer();
                metricsService.recordExternalServiceCall(ServiceType.PRICING, false,
                        metricsService.calculateDuration(start2));

                long start3 = metricsService.startTimer();
                metricsService.recordExternalServiceCall(ServiceType.AVAILABILITY, true,
                        metricsService.calculateDuration(start3));

                metricsService.recordPartialResponse();

                return "aggregated-product";
            });

            // Then - verify all metrics were recorded
            assertThat(meterRegistry.find("product.aggregation.time").timer()).isNotNull();
            assertThat(meterRegistry.find("product.aggregation.partial").counter()).isNotNull();
            assertThat(meterRegistry.find("external.service.call")
                    .tag("status", "success").timers()).hasSize(2);
            assertThat(meterRegistry.find("external.service.call")
                    .tag("status", "failure").timers()).hasSize(1);
        }

        @Test
        @DisplayName("Should handle all services failing")
        void shouldHandleAllServicesFailing() {
            // When
            metricsService.recordExternalServiceCall(ServiceType.CATALOG, false, 100);
            metricsService.recordExternalServiceCall(ServiceType.PRICING, false, 100);
            metricsService.recordExternalServiceCall(ServiceType.AVAILABILITY, false, 100);
            metricsService.recordExternalServiceCall(ServiceType.CUSTOMER, false, 100);
            metricsService.recordPartialResponse();

            // Then
            assertThat(meterRegistry.find("external.service.call")
                    .tag("status", "failure").timers()).hasSize(4);
            assertThat(meterRegistry.find("external.service.failures").counters()).hasSize(4);

            // FIX: Use find() with tags
            Counter partialCounter = meterRegistry.find("product.aggregation.partial")
                    .tag("service", "aggregator")
                    .counter();

            assertThat(partialCounter).isNotNull();
            assertThat(partialCounter.count()).isEqualTo(1.0);
        }
    }
}