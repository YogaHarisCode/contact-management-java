package com.yogaharis.contactmanagement.exception;

import com.yogaharis.contactmanagement.dto.ErrorDetail;
import com.yogaharis.contactmanagement.dto.ErrorResponse;
import com.yogaharis.contactmanagement.dto.WebResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Central exception handler that turns every error into the standard
 * {@link WebResponse} envelope, so clients always receive the same error shape.
 *
 * @author Yoga Harisistianugroho
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles errors raised deliberately with a status and reason, such as a
     * {@code 409 CONFLICT} for a duplicate username.
     *
     * @param exception the thrown {@link ResponseStatusException}
     * @return a response with the exception's status and reason
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<WebResponse<Void>> handleResponseStatusException(ResponseStatusException exception) {
        HttpStatusCode status = exception.getStatusCode();
        String message = exception.getReason();
        // Client errors (4xx) are expected, so no stack trace; server errors (5xx) keep it for diagnosis.
        if (status.is5xxServerError()) {
            log.error("Request failed: status={}, message={}", status.value(), message, exception);
        } else {
            log.debug("Request rejected: status={}, message={}", status.value(), message);
        }
        return buildError(
                status.toString(),
                message,
                null,
                status
        );
    }

    /**
     * Handles request body validation failures (from {@code @Valid}).
     *
     * @param exception the validation exception holding all field errors
     * @return a {@code 400 BAD_REQUEST} response with code {@code VALIDATION_ERROR} and one detail per invalid field
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<WebResponse<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        List<ErrorDetail> errorDetails = exception.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> ErrorDetail.builder()
                        .field(fieldError.getField())
                        .message(fieldError.getDefaultMessage())
                        .build()
                )
                .toList();

        List<String> invalidFields = errorDetails.stream().map(ErrorDetail::getField).toList();
        log.warn("Request validation failed: violationCount={}, fields={}", errorDetails.size(), invalidFields);

        return buildError(
                "VALIDATION_ERROR",
                "Request validation failed",
                errorDetails,
                HttpStatus.BAD_REQUEST
        );
    }

    /**
     * Handles a request body that is missing or is not valid JSON.
     *
     * @param exception the exception thrown while reading the request body
     * @return a {@code 400 BAD_REQUEST} response
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<WebResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException exception) {
        // Only the exception type is logged: Jackson messages can quote part of the request body,
        // which might contain a password.
        log.warn("Malformed or missing request body: exceptionType={}",
                exception.getClass().getSimpleName());
        return buildError(
                HttpStatus.BAD_REQUEST.toString(),
                "Request body is missing or malformed",
                null,
                HttpStatus.BAD_REQUEST
        );
    }

    /**
     * Fallback for every exception not handled above.
     *
     * @param exception the unhandled exception
     * @return the framework's own status for web errors (404, 405, 415, ...),
     *         otherwise a generic {@code 500 INTERNAL_SERVER_ERROR} that hides internal details
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<WebResponse<Void>> handleUnexpectedException(Exception exception) {
        // Spring web exceptions (404, 405, 415, ...) already carry the right status, so keep it.
        if (exception instanceof org.springframework.web.ErrorResponse frameworkError) {
            HttpStatusCode status = frameworkError.getStatusCode();
            if (status.is5xxServerError()) {
                log.error("Framework error: status={}, exceptionType={}",
                        status.value(), exception.getClass().getSimpleName(), exception);
            } else {
                log.debug("Framework rejected request: status={}, exceptionType={}",
                        status.value(), exception.getClass().getSimpleName());
            }
            return buildError(frameworkError.getStatusCode().toString(), frameworkError.getBody().getDetail(), null, frameworkError.getStatusCode());
        }

        // Anything else is a server fault: keep the stack trace in the log, but return a generic message to the client.
        log.error("Unexpected error: exceptionType={}", exception.getClass().getSimpleName(), exception);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR.toString(), "Unexpected error occurred", null, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Builds the standard error response.
     *
     * @param code         the error code shown to the client
     * @param message      the human-readable error message
     * @param errorDetails per-field problems, or {@code null} when not applicable
     * @param statusCode   the HTTP status of the response
     * @return a {@link ResponseEntity} wrapping a {@link WebResponse} that carries only {@code errors}
     */
    private ResponseEntity<WebResponse<Void>> buildError(String code,
                                                         String message,
                                                         List<ErrorDetail> errorDetails,
                                                         HttpStatusCode statusCode) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(code)
                .message(message)
                .details(errorDetails)
                .build();

        WebResponse<Void> body = WebResponse.<Void>builder().errors(errorResponse).build();
        return ResponseEntity.status(statusCode).body(body);
    }
}