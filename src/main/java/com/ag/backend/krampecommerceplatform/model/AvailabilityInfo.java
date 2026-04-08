package com.ag.backend.krampecommerceplatform.model;

import java.time.LocalDate;

public record AvailabilityInfo(boolean inStock, String warehouse, LocalDate estimatedDelivery) { }
