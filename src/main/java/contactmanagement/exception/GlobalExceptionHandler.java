package contactmanagement.exception;

import contactmanagement.model.WebResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<WebResponse<Void>> methodArgumentNotValidException(MethodArgumentNotValidException exception){
        Map<String, String> errors = new LinkedHashMap<>();

        exception.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getField).thenComparing(fieldError -> String.valueOf(fieldError.getDefaultMessage())))
                .forEach(fieldError -> errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage()));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(WebResponse.<Void>builder().errors(errors).build());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<WebResponse<Void>> responseStatusException(ResponseStatusException exception){
        return ResponseEntity.status(exception.getStatusCode()).body(WebResponse.<Void>builder().errors(exception.getMessage()).build());
    }
}
