package com.ag.backend.krampecommerceplatform.model;

import com.ag.backend.krampecommerceplatform.config.ServiceType;

public record ExternalResult<T>(
        ServiceType serviceType,
        T data,
        boolean required,
        String warningMessage
) {

    public boolean hasData() {
        return data != null;
    }

    public boolean hasWarning() {
        return warningMessage != null;
    }
}
