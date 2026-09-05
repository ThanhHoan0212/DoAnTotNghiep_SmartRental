package com.phongtro.backend.exception;

import com.phongtro.backend.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Object>> handleAppException(AppException ex) {
        log.warn("AppException occurred: [{}] {}", ex.getErrorCode().getCode(), ex.getMessage());
        ErrorCode errorCode = ex.getErrorCode();
        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message(ex.getMessage() != null ? ex.getMessage() : errorCode.getMessage())
                .data(Map.of("errorCode", errorCode.getCode()))
                .build();
        return new ResponseEntity<>(response, errorCode.getHttpStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        log.warn("Validation failed: {}", errors);
        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .success(false)
                .message("Dữ liệu không hợp lệ, vui lòng kiểm tra lại các trường thông tin")
                .data(errors)
                .build();
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Bad credentials error: {}", ex.getMessage());
        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message(ErrorCode.BAD_CREDENTIALS.getMessage())
                .data(Map.of("errorCode", ErrorCode.BAD_CREDENTIALS.getCode()))
                .build();
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied error: {}", ex.getMessage());
        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message(ErrorCode.UNAUTHORIZED.getMessage())
                .data(Map.of("errorCode", ErrorCode.UNAUTHORIZED.getCode()))
                .build();
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneralException(Exception ex) {
        log.error("Unhandled exception caught: ", ex);
        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message(ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage())
                .data(Map.of("errorCode", ErrorCode.UNCATEGORIZED_EXCEPTION.getCode()))
                .build();
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
