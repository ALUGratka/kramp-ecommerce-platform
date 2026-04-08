package com.ag.backend.krampecommerceplatform.service;

import com.ag.backend.krampecommerceplatform.config.AppConfig;
import com.ag.backend.krampecommerceplatform.config.ServiceType;
import com.ag.backend.krampecommerceplatform.exception.AggregationTimeoutException;
import com.ag.backend.krampecommerceplatform.exception.ExternalServiceUnavailableException;
import com.ag.backend.krampecommerceplatform.model.AggregationOutcome;
import com.ag.backend.krampecommerceplatform.model.ExternalResult;
import com.ag.backend.krampecommerceplatform.strategy.DataProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AggregationOrchestrator {

    private final List<DataProvider<?>> dataProviders;
    private final ExecutorService executorService;
    private final MetricsService metricsService;
    private final AppConfig config;

    public AggregationOutcome aggregate(String productId, String market, String customerId) {
        long deadlineNanos = System.nanoTime() + config.aggregation().timeout().toNanos();

        ExecutorCompletionService<ExternalResult<?>> completionService = new ExecutorCompletionService<>(executorService);

        List<Future<ExternalResult<?>>> futures = submitAllProviderTasks(completionService, productId, market, customerId);

        List<ExternalResult<?>> results = collectAllResults(completionService, deadlineNanos, productId, futures);

        return buildAggregationOutcome(results);
    }

    private List<Future<ExternalResult<?>>> submitAllProviderTasks(ExecutorCompletionService<ExternalResult<?>> completionService,
                                                                   String productId, String market, String customerId) {
        return dataProviders.stream()
                .map(provider -> completionService.submit(() -> executeProviderCall(provider, productId, market, customerId)))
                .collect(Collectors.toList());
    }

    private ExternalResult<?> executeProviderCall(DataProvider<?> dataProvider, String productId, String market, String customerId) {
        long startTime = metricsService.startTimer();

        try {
            log.debug("Calling {}", dataProvider.name());
            Object data = dataProvider.provideData(productId, market, customerId);
            long duration = metricsService.calculateDuration(startTime);

            metricsService.recordExternalServiceCall(dataProvider.name(), true, duration);
            log.info("{} completed in {}ms", dataProvider.name(), duration);

            return new ExternalResult<>(dataProvider.name(), data, dataProvider.isRequired(), null);
        } catch (Exception e) {
            long duration = metricsService.calculateDuration(startTime);

            metricsService.recordExternalServiceCall(dataProvider.name(), false, duration);
            metricsService.recordExternalServiceFailure(dataProvider.name(), dataProvider.isRequired());

            log.error("{} failed after {}ms: {}", dataProvider.name(), duration, e.getMessage());

            if (dataProvider.isRequired()) {
                throw new ExternalServiceUnavailableException(dataProvider.name(), productId, e);
            }

            return new ExternalResult<>(dataProvider.name(), null, false, dataProvider.name() + " service unavailable");
        }
    }

    private List<ExternalResult<?>> collectAllResults(ExecutorCompletionService<ExternalResult<?>> completionService,
                                                      long deadlineNanos, String productId, List<Future<ExternalResult<?>>> submittedFutures) {
        return dataProviders.stream()
                .map(provider -> collectNextCompletedResult(completionService, deadlineNanos, productId, submittedFutures))
                .collect(Collectors.toList());
    }

    private ExternalResult<?> collectNextCompletedResult(ExecutorCompletionService<ExternalResult<?>> completionService,
                                                         long deadlineNanos, String productId,
                                                         List<Future<ExternalResult<?>>> futures) {
        try {
            Future<ExternalResult<?>> completedFuture = pollCompletedFutureWithTimeout(completionService, deadlineNanos, productId);
            return completedFuture.get();
        } catch (ExecutionException e) {
            cancelAllFutures(futures);
            if (e.getCause() instanceof ExternalServiceUnavailableException externalFailure) {
                throw externalFailure;
            }
            throw new RuntimeException("Aggregation orchestration failed for product " + productId, e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            cancelAllFutures(futures);
            throw new AggregationTimeoutException(productId, config.aggregation().timeout().toMillis());
        }
    }

    private Future<ExternalResult<?>> pollCompletedFutureWithTimeout(ExecutorCompletionService<ExternalResult<?>> completionService, long deadlineNanos, String productId) throws InterruptedException {
        long remainingNanos = deadlineNanos - System.nanoTime();
        if (remainingNanos <= 0) {
            throw new AggregationTimeoutException(productId, config.aggregation().timeout().toMillis());
        }

        Future<ExternalResult<?>> completedFuture = completionService.poll(remainingNanos, TimeUnit.NANOSECONDS);
        if (completedFuture == null) {
            throw new AggregationTimeoutException(productId, config.aggregation().timeout().toMillis());
        }
        return completedFuture;
    }

    private AggregationOutcome buildAggregationOutcome(List<ExternalResult<?>> results) {
        Map<ServiceType, Object> aggregatedData = new EnumMap<>(ServiceType.class);
        List<String> warnings = new ArrayList<>();

        for (ExternalResult<?> result : results) {
            if (result.hasData()) {
                aggregatedData.put(result.serviceType(), result.data());
            } else if (result.hasWarning()) {
                warnings.add(result.warningMessage());
            }
        }

        return new AggregationOutcome(aggregatedData, List.copyOf(warnings));
    }

    private void cancelAllFutures(List<Future<ExternalResult<?>>> futures) {
        futures.forEach(future -> future.cancel(true));
    }
}
