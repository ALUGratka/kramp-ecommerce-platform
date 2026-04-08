package com.ag.backend.krampecommerceplatform.exception;

import com.ag.backend.krampecommerceplatform.config.ServiceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Nested
    @DisplayName("ExternalServiceUnavailableException handling")
    class ExternalServiceUnavailableExceptionTests {

        @Test
        @DisplayName("Should return 502 Bad Gateway for external service failure")
        void shouldReturn502ForExternalServiceFailure() {
            // Given
            RuntimeException cause = new RuntimeException("Connection timeout");
            ExternalServiceUnavailableException exception = new ExternalServiceUnavailableException(
                    ServiceType.CATALOG,
                    "PROD-123",
                    cause
            );

            // When
            ProblemDetail result = exceptionHandler.handleExternalServiceUnavailable(exception);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY.value());
            assertThat(result.getTitle()).isEqualTo("External Service Unavailable");
            assertThat(result.getDetail()).contains("CATALOG");
            assertThat(result.getDetail()).contains("PROD-123");
        }

        @Test
        @DisplayName("Should handle different service types")
        void shouldHandleDifferentServiceTypes() {
            // Given
            ExternalServiceUnavailableException pricingException = new ExternalServiceUnavailableException(
                    ServiceType.PRICING,
                    "PROD-456",
                    new RuntimeException("Service down")
            );

            // When
            ProblemDetail result = exceptionHandler.handleExternalServiceUnavailable(pricingException);

            // Then
            assertThat(result.getStatus()).isEqualTo(502);
            assertThat(result.getDetail()).contains("PRICING");
            assertThat(result.getDetail()).contains("PROD-456");
        }

        @Test
        @DisplayName("Should include all required fields in ProblemDetail")
        void shouldIncludeAllRequiredFields() {
            // Given
            ExternalServiceUnavailableException exception = new ExternalServiceUnavailableException(
                    ServiceType.AVAILABILITY,
                    "PROD-789",
                    new RuntimeException("Timeout")
            );

            // When
            ProblemDetail result = exceptionHandler.handleExternalServiceUnavailable(exception);

            // Then
            assertThat(result.getStatus()).isEqualTo(502);
            assertThat(result.getTitle()).isNotBlank();
            assertThat(result.getDetail()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("CompletionException handling")
    class CompletionExceptionTests {

        @Test
        @DisplayName("Should unwrap and handle ExternalServiceUnavailableException from CompletionException")
        void shouldUnwrapExternalServiceException() {
            // Given
            ExternalServiceUnavailableException cause = new ExternalServiceUnavailableException(
                    ServiceType.CATALOG,
                    "PROD-001",
                    new RuntimeException("Service error")
            );
            CompletionException completionException = new CompletionException(cause);

            // When
            ProblemDetail result = exceptionHandler.handleCompletionException(completionException);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY.value());
            assertThat(result.getTitle()).isEqualTo("External Service Unavailable");
            assertThat(result.getDetail()).contains("CATALOG");
            assertThat(result.getDetail()).contains("PROD-001");
        }

        @Test
        @DisplayName("Should return 500 for other CompletionException causes")
        void shouldReturn500ForOtherCompletionExceptions() {
            // Given
            RuntimeException unexpectedCause = new RuntimeException("Unexpected async error");
            CompletionException completionException = new CompletionException(unexpectedCause);

            // When
            ProblemDetail result = exceptionHandler.handleCompletionException(completionException);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
            assertThat(result.getTitle()).isEqualTo("Internal Server Error");
            assertThat(result.getDetail()).contains("async operation");
        }

        @Test
        @DisplayName("Should handle CompletionException with null cause")
        void shouldHandleCompletionExceptionWithNullCause() {
            // Given
            CompletionException completionException = new CompletionException(null);

            // When
            ProblemDetail result = exceptionHandler.handleCompletionException(completionException);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(500);
            assertThat(result.getTitle()).isEqualTo("Internal Server Error");
        }
    }

    @Nested
    @DisplayName("MissingServletRequestParameterException handling")
    class MissingParameterExceptionTests {

        @Test
        @DisplayName("Should return 400 Bad Request for missing parameter")
        void shouldReturn400ForMissingParameter() {
            // Given
            MissingServletRequestParameterException exception =
                    new MissingServletRequestParameterException("market", "String");

            // When
            ProblemDetail result = exceptionHandler.handleMissingParams(exception);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(result.getTitle()).isEqualTo("Bad Request");
            assertThat(result.getDetail()).contains("market");
            assertThat(result.getDetail()).contains("missing");
        }

        @Test
        @DisplayName("Should handle different parameter names")
        void shouldHandleDifferentParameterNames() {
            // Given
            MissingServletRequestParameterException exception =
                    new MissingServletRequestParameterException("customerId", "String");

            // When
            ProblemDetail result = exceptionHandler.handleMissingParams(exception);

            // Then
            assertThat(result.getStatus()).isEqualTo(400);
            assertThat(result.getDetail()).contains("customerId");
        }
    }

    @Nested
    @DisplayName("MethodArgumentTypeMismatchException handling")
    class TypeMismatchExceptionTests {

        @Test
        @DisplayName("Should return 400 Bad Request for type mismatch")
        void shouldReturn400ForTypeMismatch() {
            // Given
            MethodArgumentTypeMismatchException exception = mock(MethodArgumentTypeMismatchException.class);
            when(exception.getName()).thenReturn("productId");

            // When
            ProblemDetail result = exceptionHandler.handleTypeMismatch(exception);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(result.getTitle()).isEqualTo("Bad Request");
            assertThat(result.getDetail()).contains("productId");
            assertThat(result.getDetail()).contains("invalid format");
        }

        @Test
        @DisplayName("Should handle different parameter names in type mismatch")
        void shouldHandleDifferentParameterNamesInTypeMismatch() {
            // Given
            MethodArgumentTypeMismatchException exception = mock(MethodArgumentTypeMismatchException.class);
            when(exception.getName()).thenReturn("quantity");

            // When
            ProblemDetail result = exceptionHandler.handleTypeMismatch(exception);

            // Then
            assertThat(result.getStatus()).isEqualTo(400);
            assertThat(result.getDetail()).contains("quantity");
        }
    }

    @Nested
    @DisplayName("Generic Exception handling")
    class GenericExceptionTests {

        @Test
        @DisplayName("Should return 500 Internal Server Error for unexpected exceptions")
        void shouldReturn500ForUnexpectedException() {
            // Given
            Exception exception = new RuntimeException("Unexpected error");

            // When
            ProblemDetail result = exceptionHandler.handleGenericException(exception);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
            assertThat(result.getTitle()).isEqualTo("Internal Server Error");
            assertThat(result.getDetail()).contains("unexpected error occurred");
        }

        @Test
        @DisplayName("Should handle NullPointerException")
        void shouldHandleNullPointerException() {
            // Given
            Exception exception = new NullPointerException("Null value encountered");

            // When
            ProblemDetail result = exceptionHandler.handleGenericException(exception);

            // Then
            assertThat(result.getStatus()).isEqualTo(500);
            assertThat(result.getTitle()).isEqualTo("Internal Server Error");
        }

        @Test
        @DisplayName("Should handle IllegalArgumentException")
        void shouldHandleIllegalArgumentException() {
            // Given
            Exception exception = new IllegalArgumentException("Invalid argument");

            // When
            ProblemDetail result = exceptionHandler.handleGenericException(exception);

            // Then
            assertThat(result.getStatus()).isEqualTo(500);
            assertThat(result.getDetail()).isNotNull();
        }
    }

    @Nested
    @DisplayName("ProblemDetail structure validation")
    class ProblemDetailStructureTests {

        @Test
        @DisplayName("Should create valid ProblemDetail for all exception types")
        void shouldCreateValidProblemDetailForAllTypes() {
            // Test ExternalServiceUnavailableException
            ProblemDetail pd1 = exceptionHandler.handleExternalServiceUnavailable(
                    new ExternalServiceUnavailableException(ServiceType.CATALOG, "P1", new RuntimeException())
            );
            validateProblemDetail(pd1, 502);

            // Test CompletionException
            ProblemDetail pd2 = exceptionHandler.handleCompletionException(
                    new CompletionException(new RuntimeException("Test"))
            );
            validateProblemDetail(pd2, 500);

            // Test MissingServletRequestParameterException
            ProblemDetail pd3 = exceptionHandler.handleMissingParams(
                    new MissingServletRequestParameterException("param", "String")
            );
            validateProblemDetail(pd3, 400);

            // Test generic Exception
            ProblemDetail pd4 = exceptionHandler.handleGenericException(new RuntimeException());
            validateProblemDetail(pd4, 500);
        }

        private void validateProblemDetail(ProblemDetail pd, int expectedStatus) {
            assertThat(pd).isNotNull();
            assertThat(pd.getStatus()).isEqualTo(expectedStatus);
            assertThat(pd.getTitle()).isNotBlank();
            assertThat(pd.getDetail()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("Edge cases and boundary conditions")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle exception with very long message")
        void shouldHandleExceptionWithLongMessage() {
            // Given
            String longMessage = "A".repeat(1000);
            Exception exception = new RuntimeException(longMessage);

            // When
            ProblemDetail result = exceptionHandler.handleGenericException(exception);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(500);
        }

        @Test
        @DisplayName("Should handle exception with null message")
        void shouldHandleExceptionWithNullMessage() {
            // Given
            Exception exception = new RuntimeException((String) null);

            // When
            ProblemDetail result = exceptionHandler.handleGenericException(exception);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getDetail()).isNotNull();
        }

        @Test
        @DisplayName("Should handle exception with special characters in message")
        void shouldHandleExceptionWithSpecialCharacters() {
            // Given
            Exception exception = new RuntimeException("Error: <script>alert('xss')</script>");

            // When
            ProblemDetail result = exceptionHandler.handleGenericException(exception);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getDetail()).isEqualTo("An unexpected error occurred");
        }
    }
}