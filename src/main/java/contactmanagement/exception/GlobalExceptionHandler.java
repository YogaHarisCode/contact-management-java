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

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<WebResponse<Void>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));

        // Only field names and constraint messages are logged, never rejected values.
        log.warn("Request validation failed: target={}, errors=[{}]",
                exception.getBindingResult().getObjectName(), message);

        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<WebResponse<Void>> handleUnreadableBody(HttpMessageNotReadableException exception) {
        // The exception message is intentionally not logged: it may echo a fragment of the payload.
        log.warn("Malformed or missing request body: exceptionType={}",
                exception.getClass().getSimpleName());

        return error(HttpStatus.BAD_REQUEST, "Malformed or missing request body");
    }

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

    private ResponseEntity<WebResponse<Void>> error(HttpStatusCode statusCode, String message) {
        return ResponseEntity.status(statusCode)
                .body(WebResponse.<Void>builder().errors(message).build());
    }
}