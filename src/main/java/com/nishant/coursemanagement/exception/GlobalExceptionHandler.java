package com.nishant.coursemanagement.exception;


import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.nishant.coursemanagement.entity.Role;
import com.nishant.coursemanagement.exception.custom.CustomBadRequestException;
import com.nishant.coursemanagement.exception.custom.DuplicateResourceException;
import com.nishant.coursemanagement.exception.custom.ResourceNotFoundException;
import com.nishant.coursemanagement.exception.custom.UnauthorizedException;
import com.nishant.coursemanagement.exception.response.ErrorResponse;
import com.nishant.coursemanagement.exception.response.ErrorResponseFactory;
import com.nishant.coursemanagement.util.LogUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

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
    public ErrorResponse handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        try {
            LogUtil.put("action", "VALIDATION_FAILED");
            LogUtil.put("message", ex.getMessage());
            log.warn("Validation failed");
        } finally {
            LogUtil.clear();
        }
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(
                error -> errors.put(error.getField(), error.getDefaultMessage())
        );
        return errorResponseFactory.build(HttpStatus.BAD_REQUEST, "Validation Failed", ErrorCode.VALIDATION_FAILED, errors, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        try {
            LogUtil.put("action", "ILLEGAL_ARGUMENT");
            LogUtil.put("message", ex.getMessage());
            log.warn("Illegal argument");
        } finally {
            LogUtil.clear();
        }
        return errorResponseFactory.build(HttpStatus.BAD_REQUEST, "Invalid value provided: " + ex.getMessage(), ErrorCode.ILLEGAL_ARGUMENT, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleEnumError(HttpMessageNotReadableException ex, HttpServletRequest request) {
        try {
            LogUtil.put("action", "INVALID_ENUM_VALUE");
            LogUtil.put("message", ex.getMessage());
            log.warn("Invalid enum value");
        } finally {
            LogUtil.clear();
        }
        String message = "Invalid request";
        if (ex.getCause() instanceof InvalidFormatException e) {
            String fieldName = e.getPath().getFirst().getFieldName();
            Object invalidValue = e.getValue();
            message = "Invalid value '" + invalidValue + "' for field '" + fieldName + "'. Allowed values: " + Arrays.toString(Role.values());
        }
        return errorResponseFactory.build(HttpStatus.BAD_REQUEST, message, ErrorCode.INVALID_ENUM_VALUE, request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        try {
            LogUtil.put("action", "RESOURCE_NOT_FOUND");
            LogUtil.put("message", ex.getMessage());
            log.warn("Resource not found");
        } finally {
            LogUtil.clear();
        }
        return errorResponseFactory.build(HttpStatus.NOT_FOUND, ex.getMessage(), ex.getErrorCode(), request);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDuplicate(DuplicateResourceException ex, HttpServletRequest request) {
        try {
            LogUtil.put("action", "DUPLICATE_RESOURCE");
            LogUtil.put("message", ex.getMessage());
            log.warn("Duplicate resource");
        } finally {
            LogUtil.clear();
        }
        return errorResponseFactory.build(HttpStatus.CONFLICT, ex.getMessage(), ex.getErrorCode(), request);
    }

    @ExceptionHandler({AuthorizationDeniedException.class, AccessDeniedException.class})
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleAccessDenied(Exception ex, HttpServletRequest request) {
        try {
            LogUtil.put("action", "ACCESS_DENIED");
            LogUtil.put("message", ex.getMessage());
            log.warn("Access denied");
        } finally {
            LogUtil.clear();
        }
        return errorResponseFactory.forbidden(ex.getMessage(), request);
    }


    @ExceptionHandler({AuthenticationException.class, UnauthorizedException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        try {
            LogUtil.put("action", "AUTHENTICATION_FAILED");
            LogUtil.put("message", ex.getMessage());
            log.warn("Authentication failed");
        } finally {
            LogUtil.clear();
        }
        return errorResponseFactory.unauthorized(ex.getMessage(), request);
    }

    @ExceptionHandler(CustomBadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequest(CustomBadRequestException ex, HttpServletRequest request) {
        try {
            LogUtil.put("action", "BAD_REQUEST");
            LogUtil.put("message", ex.getMessage());
            log.warn("Bad request");
        } finally {
            LogUtil.clear();
        }
        return errorResponseFactory.build(HttpStatus.BAD_REQUEST, ex.getMessage(), ex.getErrorCode(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        try {
            LogUtil.put("action", "TYPE_MISMATCH");
            LogUtil.put("message", ex.getMessage());
            LogUtil.put("parameter", ex.getName());
            LogUtil.put("value", String.valueOf(ex.getValue()));
            log.warn("Type mismatch");
        } finally {
            LogUtil.clear();
        }
        assert ex.getRequiredType() != null;
        String message = "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'. Expected type: " + ex.getRequiredType().getSimpleName();
        return errorResponseFactory.build(HttpStatus.BAD_REQUEST, message, ErrorCode.BAD_REQUEST, request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ErrorResponse handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        try {
            LogUtil.put("action", "METHOD_NOT_ALLOWED");
            LogUtil.put("message", ex.getMessage());
            LogUtil.put("method", ex.getMethod());
            log.warn("Method not allowed");
        } finally {
            LogUtil.clear();
        }
        String supported = ex.getSupportedHttpMethods() == null
                ? ""
                : ex.getSupportedHttpMethods().stream()
                  .map(HttpMethod::name)
                  .reduce((a, b) -> a + ", " + b)
                  .orElse("");
        String message = "HTTP method '" + ex.getMethod() + "' is not supported for this endpoint. Supported methods: " + supported;
        return errorResponseFactory.build(HttpStatus.METHOD_NOT_ALLOWED, message, ErrorCode.BAD_REQUEST, request);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNoHandlerFound(NoHandlerFoundException ex, HttpServletRequest request) {
        try {
            LogUtil.put("action", "NO_HANDLER_FOUND");
            LogUtil.put("message", ex.getMessage());
            log.warn("No handler found");
        } finally {
            LogUtil.clear();
        }
        return errorResponseFactory.build(HttpStatus.NOT_FOUND, "The requested resource was not found", ErrorCode.NOT_FOUND, request);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleRuntimeException(Exception ex, HttpServletRequest request) {
        try {
            LogUtil.put("action", "INTERNAL_SERVER_ERROR");
            LogUtil.put("message", ex.getMessage());
            log.error("Internal server error", ex);
        } finally {
            LogUtil.clear();
        }
        return errorResponseFactory.build(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong", ErrorCode.INTERNAL_ERROR, request);
    }
}
