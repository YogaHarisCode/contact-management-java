package contactmanagement.exception;

import contactmanagement.model.WebResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<WebResponse<Void>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));

        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<WebResponse<Void>> handleUnreadableBody(HttpMessageNotReadableException exception) {
        return error(HttpStatus.BAD_REQUEST, "Malformed or missing request body");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<WebResponse<Void>> handleResponseStatus(ResponseStatusException exception) {
        String message = exception.getReason() != null
                ? exception.getReason()
                : HttpStatus.valueOf(exception.getStatusCode().value()).getReasonPhrase();

        return error(exception.getStatusCode(), message);
    }

    private ResponseEntity<WebResponse<Void>> error(HttpStatusCode statusCode, String message) {
        return ResponseEntity.status(statusCode)
                .body(WebResponse.<Void>builder().errors(message).build());
    }
}