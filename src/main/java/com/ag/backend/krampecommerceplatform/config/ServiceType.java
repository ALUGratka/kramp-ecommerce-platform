package com.ag.backend.krampecommerceplatform.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ServiceType {

    AVAILABILITY("availability"),
    CATALOG("catalog"),
    CUSTOMER("customer"),
    PRICING("pricing");

    private final String name;
}
