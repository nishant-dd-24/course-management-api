package com.nishant.coursemanagement.exception;



import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.nishant.coursemanagement.entity.Role;
import com.nishant.coursemanagement.exception.custom.CustomBadRequestException;
import com.nishant.coursemanagement.exception.custom.DuplicateResourceException;
import com.nishant.coursemanagement.exception.custom.ResourceNotFoundException;
import com.nishant.coursemanagement.exception.custom.UnauthorizedException;
import com.nishant.coursemanagement.exception.response.ErrorResponse;
import com.nishant.coursemanagement.exception.response.ErrorResponseFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.naming.AuthenticationException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {
    private final ErrorResponseFactory errorResponseFactory;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationErrors(MethodArgumentNotValidException ex){
        log.warn("action=VALIDATION_FAILED message={}", ex.getMessage());
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(
                error -> errors.put(error.getField(), error.getDefaultMessage())
        );
        return errorResponseFactory.build(HttpStatus.BAD_REQUEST, "Validation Failed", ErrorCode.VALIDATION_FAILED, errors);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("action=ILLEGAL_ARGUMENT message={}", ex.getMessage());
        return errorResponseFactory.build(HttpStatus.BAD_REQUEST, "Invalid value provided: " + ex.getMessage(), ErrorCode.ILLEGAL_ARGUMENT);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleEnumError(HttpMessageNotReadableException ex) {
        log.warn("action=INVALID_ENUM_VALUE message={}", ex.getMessage());
        String message = "Invalid request";
        if (ex.getCause() instanceof InvalidFormatException e) {
            String fieldName = e.getPath().getFirst().getFieldName();
            Object invalidValue = e.getValue();
            message = "Invalid value '" + invalidValue + "' for field '" + fieldName + "'. Allowed values: " + Arrays.toString(Role.values());
        }
        return errorResponseFactory.build(HttpStatus.BAD_REQUEST, message, ErrorCode.INVALID_ENUM_VALUE);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(ResourceNotFoundException ex){
        log.warn("action=RESOURCE_NOT_FOUND message={}", ex.getMessage());
        return errorResponseFactory.build(HttpStatus.NOT_FOUND, ex.getMessage(), ex.getErrorCode());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDuplicate(DuplicateResourceException ex){
        log.warn("action=DUPLICATE_RESOURCE message={}", ex.getMessage());
        return errorResponseFactory.build(HttpStatus.CONFLICT, ex.getMessage(), ex.getErrorCode());
    }

    @ExceptionHandler({AuthorizationDeniedException.class, AccessDeniedException.class})
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleAccessDenied(Exception ex){
        log.warn("action=ACCESS_DENIED message={}", ex.getMessage());
        return errorResponseFactory.forbidden(ex.getMessage());
    }


    @ExceptionHandler({AuthenticationException.class, UnauthorizedException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleAuthenticationException(AuthenticationException ex){
        log.warn("action=AUTHENTICATION_FAILED message={}", ex.getMessage());
        return errorResponseFactory.unauthorized(ex.getMessage());
    }

    @ExceptionHandler(CustomBadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequest(CustomBadRequestException ex){
        log.warn("action=BAD_REQUEST message={}", ex.getMessage());
        return errorResponseFactory.build(HttpStatus.BAD_REQUEST, ex.getMessage(), ex.getErrorCode());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleRuntimeException(RuntimeException ex){
        log.error("action=INTERNAL_SERVER_ERROR message={}", ex.getMessage(), ex);
        return errorResponseFactory.build(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), ErrorCode.INTERNAL_ERROR);
    }
}
