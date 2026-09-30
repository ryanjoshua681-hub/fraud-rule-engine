package com.ryanm.fraudruleengine.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Produces RFC 9457 ProblemDetail responses for all error cases.
 * Keeps controller code free of error-handling boilerplate.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final URI TYPE_NOT_FOUND =
            URI.create("https://fraud-rule-engine/errors/not-found");
    private static final URI TYPE_VALIDATION =
            URI.create("https://fraud-rule-engine/errors/validation");
    private static final URI TYPE_INTERNAL =
            URI.create("https://fraud-rule-engine/errors/internal");

    @ExceptionHandler(TransactionNotFoundException.class)
    public ProblemDetail handleTransactionNotFound(final TransactionNotFoundException ex) {
        final ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setType(TYPE_NOT_FOUND);
        pd.setTitle("Transaction Not Found");
        return pd;
    }

    @ExceptionHandler(FraudFlagNotFoundException.class)
    public ProblemDetail handleFraudFlagNotFound(final FraudFlagNotFoundException ex) {
        final ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setType(TYPE_NOT_FOUND);
        pd.setTitle("Fraud Flag Not Found");
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(final MethodArgumentNotValidException ex) {
        final Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid",
                        (a, b) -> a));
        final ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Request validation failed");
        pd.setType(TYPE_VALIDATION);
        pd.setTitle("Validation Error");
        pd.setProperty("errors", errors);
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(final Exception ex) {
        log.error("Unexpected error", ex);
        final ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        pd.setType(TYPE_INTERNAL);
        pd.setTitle("Internal Server Error");
        return pd;
    }
}
