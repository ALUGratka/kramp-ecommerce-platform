package com.ag.backend.krampecommerceplatform.service;

import com.ag.backend.krampecommerceplatform.config.ServiceType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class MetricsService {

    private final MeterRegistry meterRegistry;

    public <T> T recordAggregation(Supplier<T> operation) {
        return Timer.builder("product.aggregation.time")
                .description("Time taken to aggregate product information")
                .tag("service", "aggregator")
                .register(meterRegistry)
                .record(operation);
    }

    public void recordPartialResponse() {
        Counter.builder("product.aggregation.partial")
                .description("Number of partial responses returned")
                .tag("service", "aggregator")
                .register(meterRegistry)
                .increment();
    }

    public void recordExternalServiceCall(ServiceType serviceType, boolean success, long durationMs) {
        Timer.builder("external.service.call")
                .description("External service call duration")
                .tag("service", serviceType.name().toLowerCase())
                .tag("status", success ? "success" : "failure")
                .register(meterRegistry)
                .record(java.time.Duration.ofMillis(durationMs));

        if (!success) {
            recordExternalServiceFailure(serviceType, false);
        }
    }

    public void recordExternalServiceFailure(ServiceType serviceType, boolean required) {
        Counter.builder("external.service.failures")
                .description("Number of external service failures")
                .tag("service", serviceType.name().toLowerCase())
                .tag("required", String.valueOf(required))
                .register(meterRegistry)
                .increment();
    }

    public long startTimer() {
        return System.currentTimeMillis();
    }

    public long calculateDuration(long startTime) {
        return System.currentTimeMillis() - startTime;
    }
}
