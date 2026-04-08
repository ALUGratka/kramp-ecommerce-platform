package com.ag.backend.krampecommerceplatform.exception;

import com.ag.backend.krampecommerceplatform.config.ServiceType;

public class ExternalServiceUnavailableException extends RuntimeException {

    public ExternalServiceUnavailableException(ServiceType serviceType, String productId, Exception e) {
        super("Required service [" + serviceType + "] failed for product " + productId, e);
    }
}
