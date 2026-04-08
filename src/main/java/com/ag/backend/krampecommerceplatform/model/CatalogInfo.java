package com.ag.backend.krampecommerceplatform.model;

import java.util.List;
import java.util.Map;

public record CatalogInfo(String name, String description, Map<String, String> specifications, List<String> images) { }
