package com.ag.backend.krampecommerceplatform.model;

import java.util.List;

public record CustomerInfo(String customerId, String segment, List<String> preferences) { }
