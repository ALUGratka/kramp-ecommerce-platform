# Kramp E-Commerce Platform - Product Aggregator

Spring Boot backend service that combines data from multiple internal services into a single, market-aware response.

## 📋 Table of Contents

- [Overview](#-overview)
- [Architecture](#-architecture)
- [Getting Started](#-getting-started)
- [API Documentation](#-api-documentation)
- [Design Decisions](#-design-decisions)
- [What I Would Do Differently](#-what-i-would-do-differently)
- [Design Question Answer](#-design-question-answer)

## 🎯 Overview

This service aggregates product data from 4 upstream services:
- **Catalog Service** (required) - product details, specs, images
- **Pricing Service** (optional) - prices, discounts
- **Availability Service** (optional) - stock levels, delivery estimates
- **Customer Service** (optional) - customer segment, preferences

**Key capabilities:**
- ✅ Parallel aggregation (reduces latency)
- ✅ Graceful degradation (partial responses when optional services fail)
- ✅ Market-aware response (mock combines data from multiple markets)
- ✅ Production-ready monitoring (micrometer metrics)

## 🏗 Architecture

### Key Components

| Component | Responsibility                                                                              |
|-----------|---------------------------------------------------------------------------------------------|
| `AggregateService` | Use-case facade that delegates aggregation and builds the final response                     |
| `AggregationOrchestrator` | Handles parallel execution, fail-fast for required services, timeout, and result collection |
| `DataProvider` | Strategy pattern - abstracts service calls                                                  |
| `MetricsService` | Records latency, failures, partial responses                                                |
| `GlobalExceptionHandler` | Centralized error handling (502 for required failures)                                      |


## 🚀 Getting Started

### Prerequisites

- **Java 21**
- **Maven 3.8+**

### Running the Service

```bash 
mvn clean install
mvn spring-boot:run
```

Service starts on: **http://localhost:8080**

### Running tests

All tests
```bash 
mvn test
```
Unit tests only
```bash 
mvn test -Dtest=*ServiceTest
```
Integration tests only
```bash 
mvn test -Dtest=*IntegrationTest
```
WireMock servers userd in ``ProductAggregatorIntegrationTest`` start on 8081-8084 ports.

## 📚 API Documentation

### Endpoint

**GET** `/api/v1/products/{productId}/aggregate`

### Parameters

| Parameter | Type | Required | Example | Description |
|-----------|------|----------|---------|-------------|
| `productId` | path | ✅ Yes | `BRG-001` | Product identifier |
| `market` | query | ✅ Yes | `pl-PL` | Market code (format: `xx-XX`) |
| `customerId` | query | ❌ No | `CUST-123` | Customer identifier for personalization |

### Swagger UI

Available at: **http://localhost:8080/swagger-ui.html**

## 🧠 Design Decisions

### 1. Parallel Execution with ExecutorService

**Why:** Sequential calls would take 290ms (50+80+100+60). Parallel execution takes ~100ms (max of all).

Parallel execution is handled by `AggregationOrchestrator` using ExecutorService, which allows collecting results 
as tasks complete and failing fast when a required service fails.

### 2. Strategy Pattern for Data Providers

**Why:** Makes it easy to add new services without modifying `AggregateService`.
```java
public interface DataProvider<T> {
    ServiceType name();
    boolean isRequired();
    T provideData(String productId, String market, String customerId);
}
```

### 3. Required vs. Optional Services

| Service | Required? | Failure Behavior |
|---------|-----------|------------------|
| Catalog | ✅ Yes | 502 Bad Gateway - user must retry |
| Pricing | ❌ No | Partial response with warning |
| Availability | ❌ No | Partial response with warning |
| Customer | ❌ No | Partial response (standard view) |

**Why:** Users can't see a product without basic catalog info, but they can tolerate missing prices/stock temporarily.

### 4. Per-Service Timeouts

Configured in `application.yaml`:

```yaml
app: 
  services: 
    catalog: 
      connect-timeout: 200ms 
      read-timeout: 300ms 
    availability: 
      read-timeout: 600ms # Slowest service
```

**Why:** Prevents cascade failures. Even if Availability is slow, we don't wait forever.

### 6. Fail-Fast for Required Service

**Why:** If a required service fails, the orchestrator fails fast and remaining tasks are canceled. `502 Bad Gateway` 
is returned to the user. Optional services still degrade gracefully and produce partial responses.

Cancellation is best-effort. Already running HTTP calls may still finish briefly in the background.

### 7. Metrics with Micrometer

Tracks:
- `product.aggregation.time` - total aggregation latency
- `product.aggregation.partial` - count of partial responses
- `external.service.call` - per-service latency (with success/failure tag)
- `external.service.failures` - failure count per service

product.aggregation.time i product.aggregation.partial są liczone w AggregateServic natomiast external.service.call 
i external.service.failures w AggregationOrchestrato

**Available at:** `http://localhost:8080/actuator/metrics`

**Example metric:** `http://localhost:8080/actuator/metrics/product.aggregation.time`

## 🛠 What I Would Do Differently

### 1. Enhanced Resilience

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>
```

- **Circuit Breaker:** Stop calling services that are consistently failing
- **Retry Logic:** Retry transient failures (network blips)
- **Bulkhead:** Isolate thread pools per service

### 2. Caching Strategy

```java
@Cacheable(value = "catalog", key = "#productId + #market") 
public CatalogInfo getCatalogInfo(String productId, String market) { ... }
```

- Reduce load on upstream services
- Faster response times


## 💡 Design Question Answer

### Option A: Adding "Related Products" Service
**Context:**
- Latency: 200ms (slower than Catalog's 50ms)
- Reliability: 90% (worse than Pricing's 99.5%)

### My Approach

**Decision:** Make it optional.

**Reasoning:**
- It would have higher latency and lower reliability than the core product services.
- Recommendations are useful, but they are not required to show the product itself.
- The platform should prioritize a fast and reliable product page over blocking on non-critical enrichment data.

**How the design would adapt:**
- Add a new `DataProvider` for related products.
- Extend the aggregated response model with an optional `relatedProducts` field.
- Reuse the existing orchestration and partial-response behavior.
- If the service fails, return the product normally and omit recommendations.

**Result:**
- Core product visibility stays stable.
- The system remains easy to extend with additional optional services.
- The user still gets the most important information even when enrichment data is unavailable.