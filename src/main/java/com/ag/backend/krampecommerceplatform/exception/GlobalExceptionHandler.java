package com.ag.backend.krampecommerceplatform.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.concurrent.CompletionException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ExternalServiceUnavailableException.class)
    public ProblemDetail handleExternalServiceUnavailable(ExternalServiceUnavailableException ex) {
        log.error("External service unavailable", ex);

        return createProblemDetail(
                HttpStatus.BAD_GATEWAY,
                "External Service Unavailable",
                ex.getMessage()
        );
    }

    @ExceptionHandler(CompletionException.class)
    public ProblemDetail handleCompletionException(CompletionException ex) {
        Throwable cause = ex.getCause();

        if (cause instanceof ExternalServiceUnavailableException) {
            log.error("External service unavailable (from async)", cause);
            return createProblemDetail(
                    HttpStatus.BAD_GATEWAY,
                    "External Service Unavailable",
                    cause.getMessage()
            );
        }
        log.error("Unexpected completion exception", ex);

        return createProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred during async operation"
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingParams(MissingServletRequestParameterException ex) {
        log.error("Missing required parameter", ex);

        return createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                String.format("Required parameter '%s' is missing", ex.getParameterName())
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.error("Invalid parameter type", ex);

        return createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                String.format("Parameter '%s' has invalid format", ex.getName()));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail handleValidationException(HandlerMethodValidationException ex) {
        log.error("Validation Failed", ex);

        return createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Validation Failed",
                ex.getMessage()
        );
    }

    @ExceptionHandler(AggregationTimeoutException.class)
    public ProblemDetail handleAggregationTimeout(AggregationTimeoutException ex) {
        log.error("Aggregation timed out", ex);

        return createProblemDetail(
                HttpStatus.GATEWAY_TIMEOUT,
                "Gateway Timeout",
                ex.getMessage()
        );
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);

        return createProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred"
        );
    }

    private ProblemDetail createProblemDetail(HttpStatus status, String title, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);

        return problemDetail;
    }
}
