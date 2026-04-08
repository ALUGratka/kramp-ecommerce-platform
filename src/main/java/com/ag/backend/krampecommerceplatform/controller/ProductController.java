package com.ag.backend.krampecommerceplatform.controller;

import com.ag.backend.krampecommerceplatform.model.Product;
import com.ag.backend.krampecommerceplatform.service.AggregateService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("api/v1/products")
@AllArgsConstructor
public class ProductController implements ProductApi {

    private final AggregateService service;

    @GetMapping("/{productId}/aggregate")
    public ResponseEntity<Product> aggregateProductDetails(@PathVariable String productId, @RequestParam String market, @RequestParam(required = false) String customerId) {
        return ResponseEntity.ok(service.aggregateProduct(productId, market, customerId));
    }
}
