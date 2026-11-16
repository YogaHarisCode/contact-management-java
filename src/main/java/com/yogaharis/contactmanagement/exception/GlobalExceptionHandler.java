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

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<WebResponse<Void>> handleResponseStatusException(ResponseStatusException exception) {
        HttpStatusCode status = exception.getStatusCode();
        String message = exception.getReason();
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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<WebResponse<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        List<ErrorDetail> errorDetails = exception.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> ErrorDetail.builder()
                        .field(fieldError.getField())
                        .message(fieldError.getDefaultMessage())
                        .build()
                )
                .toList();
        log.warn("Request validation failed: target={}, errors=[{}]",
                errorDetails.getFirst().getField(), errorDetails.getFirst().getMessage());

        return buildError(
                "VALIDATION_ERROR",
                "Request validation failed",
                errorDetails,
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<WebResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException exception) {
        log.warn("Malformed or missing request body: exceptionType={}",
                exception.getClass().getSimpleName());
        return buildError(
                HttpStatus.BAD_REQUEST.toString(),
                "Request body is missing or malformed",
                null,
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<WebResponse<Void>> handleUnexpectedException(Exception exception) {
        if (exception instanceof org.springframework.web.ErrorResponse frameworkError) {
            return buildError(frameworkError.getStatusCode().toString(), frameworkError.getBody().getDetail(), null, frameworkError.getStatusCode());
        }

        return buildError(HttpStatus.INTERNAL_SERVER_ERROR.toString(), "Unexpected error occurred", null, HttpStatus.INTERNAL_SERVER_ERROR);
    }

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
