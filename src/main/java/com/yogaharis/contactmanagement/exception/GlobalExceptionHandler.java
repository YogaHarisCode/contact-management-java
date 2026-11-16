package com.yogaharis.contactmanagement.exception;

import com.yogaharis.contactmanagement.dto.ErrorDetail;
import com.yogaharis.contactmanagement.dto.ErrorResponse;
import com.yogaharis.contactmanagement.dto.WebResponse;
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
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<WebResponse<Void>> handleResponseStatusException(ResponseStatusException exception) {
        return buildError(
                exception.getStatusCode().toString(),
                exception.getReason(),
                null,
                exception.getStatusCode()
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

        return buildError(
                "VALIDATION_ERROR",
                "Request validation failed",
                errorDetails,
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<WebResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException exception) {
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
