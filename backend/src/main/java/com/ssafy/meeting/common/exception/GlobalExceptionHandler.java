package com.ssafy.meeting.common.exception;

import com.ssafy.meeting.common.api.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException exception) {
        log.warn("Resource not found: {}", exception.getMessage());
        return error(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(UnauthorizedException exception) {
        log.warn("Unauthorized request: {}", exception.getMessage());
        return error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", exception.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(ForbiddenException exception) {
        log.warn("Forbidden request: {}", exception.getMessage());
        return error(HttpStatus.FORBIDDEN, "FORBIDDEN", exception.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(ConflictException exception) {
        log.warn("Conflict request: {}", exception.getMessage());
        return error(HttpStatus.CONFLICT, "CONFLICT", exception.getMessage());
    }


    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException exception) {
        log.warn("Access denied: {}", exception.getMessage());
        return error(HttpStatus.FORBIDDEN, "FORBIDDEN", "Insufficient permission");
    }

    @ExceptionHandler({ValidationException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<ApiResponse<Void>> handleValidation(Exception exception) {
        log.warn("Validation failed: {}", exception.getMessage());
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception) {
        log.error("Unexpected server error", exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "Unexpected server error");
    }

    private ResponseEntity<ApiResponse<Void>> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ApiResponse.failure(code, message));
    }
}
