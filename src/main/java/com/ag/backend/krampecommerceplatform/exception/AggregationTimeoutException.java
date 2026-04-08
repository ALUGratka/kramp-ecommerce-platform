package com.ag.backend.krampecommerceplatform.exception;

public class AggregationTimeoutException extends RuntimeException {
    public AggregationTimeoutException(String productId, long timeoutMs) {
        super("Aggregation timed out for product " + productId + " after " + timeoutMs + "ms");
    }
}
