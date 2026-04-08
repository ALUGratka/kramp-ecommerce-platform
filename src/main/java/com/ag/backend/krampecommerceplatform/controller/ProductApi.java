package com.ag.backend.krampecommerceplatform.controller;

import com.ag.backend.krampecommerceplatform.model.Product;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Products", description = "Product Aggregation Information")
public interface ProductApi {

    @Operation(
            summary = "Get aggregated product information",
            description = "Aggregates product data from multiple internal services into a unified response for a specific market.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully aggregated product data", content = @Content(schema = @Schema(implementation = Product.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid request parameters", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "502", description = "Required external service unavailable (e.g., Catalog service)", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
            }
    )
    ResponseEntity<Product> aggregateProductDetails(
            @Parameter(description = "Product ID", example = "BRG-001")
            @PathVariable @NotBlank String productId,
            @Parameter(description = "Market Code ", example = "pl-PL")
            @Pattern(regexp = "[a-z]{2}-[A-Z]{2}", message = "Market must be in format 'xx-XX' (e.g. 'pl-PL')")
            @RequestParam String market,
            @Parameter(description = "Customer ID (optional)", example = "CUST-1")
            @RequestParam(required = false) String customerId);
}
