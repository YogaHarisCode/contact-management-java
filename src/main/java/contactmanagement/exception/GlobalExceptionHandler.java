package contactmanagement.exception;

import contactmanagement.model.WebResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Collectors;

/**
 * Central exception handler that converts exceptions into consistent {@link WebResponse}
 * error bodies.
 *
 * <p>Client errors (4xx) are logged at WARN or DEBUG level, server errors (5xx) at ERROR level
 * with the stack trace. Rejected input values are never logged because they may contain
 * passwords.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles Bean Validation failures raised for {@code @Valid} request bodies.
     *
     * @param exception the validation exception holding all field errors
     * @return HTTP 400 whose {@code errors} lists every violated field as
     *         {@code "<field> <message>"}, sorted alphabetically and separated by {@code "; "}
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<WebResponse<Void>> handleValidation(MethodArgumentNotValidException exception) {
        // Sorted so the message is deterministic when one field violates several constraints.
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));

        // Only field names and constraint messages are logged, never rejected values.
        log.warn("Request validation failed: target={}, errors=[{}]",
                exception.getBindingResult().getObjectName(), message);

        return error(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Handles request bodies that are missing or are not valid JSON.
     *
     * @param exception the exception thrown by the message converter
     * @return HTTP 400 with a generic message
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<WebResponse<Void>> handleUnreadableBody(HttpMessageNotReadableException exception) {
        // The exception message is intentionally not logged: it may echo a fragment of the payload.
        log.warn("Malformed or missing request body: exceptionType={}",
                exception.getClass().getSimpleName());

        return error(HttpStatus.BAD_REQUEST, "Malformed or missing request body");
    }

    /**
     * Handles {@link ResponseStatusException} thrown by the service layer.
     *
     * @param exception the exception carrying the HTTP status and an optional reason
     * @return a response with the exception's status; {@code errors} is the reason, or the
     *         standard reason phrase of the status when no reason was provided
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<WebResponse<Void>> handleResponseStatus(ResponseStatusException exception) {
        HttpStatusCode status = exception.getStatusCode();
        String message = exception.getReason() != null
                ? exception.getReason()
                : HttpStatus.valueOf(status.value()).getReasonPhrase();

        if (status.is5xxServerError()) {
            log.error("Request failed: status={}, reason={}", status.value(), message, exception);
        } else {
            // 4xx raised by services are already logged there with business context (ids, usernames).
            log.debug("Request rejected: status={}, reason={}", status.value(), message);
        }

        return error(status, message);
    }

    /**
     * Builds an error response in the standard envelope.
     *
     * @param statusCode the HTTP status of the response
     * @param message    the text placed in {@code errors}
     * @return the response entity without any {@code data}
     */
    private ResponseEntity<WebResponse<Void>> error(HttpStatusCode statusCode, String message) {
        return ResponseEntity.status(statusCode)
                .body(WebResponse.<Void>builder().errors(message).build());
    }
}