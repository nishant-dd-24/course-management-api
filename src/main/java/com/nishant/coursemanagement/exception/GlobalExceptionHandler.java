package com.nishant.coursemanagement.exception;


import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.nishant.coursemanagement.entity.Role;
import com.nishant.coursemanagement.exception.custom.CustomBadRequestException;
import com.nishant.coursemanagement.exception.custom.DuplicateResourceException;
import com.nishant.coursemanagement.exception.custom.ResourceNotFoundException;
import com.nishant.coursemanagement.exception.custom.UnauthorizedException;
import com.nishant.coursemanagement.exception.response.ErrorResponse;
import com.nishant.coursemanagement.exception.response.ErrorResponseFactory;
import com.nishant.coursemanagement.log.util.LogUtil;
import jakarta.servlet.http.HttpServletRequest;

import static com.nishant.coursemanagement.log.annotation.LogLevel.WARN;
import static com.nishant.coursemanagement.log.annotation.LogLevel.ERROR;
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
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(
                error -> errors.put(error.getField(), error.getDefaultMessage())
        );
        LogUtil.log(log, WARN, "VALIDATION_FAILED", "Validation failed", "fieldCount", errors.size(), "path", request.getRequestURI());
        return errorResponseFactory.build(HttpStatus.BAD_REQUEST, "Validation Failed", ErrorCode.VALIDATION_FAILED, errors, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        LogUtil.log(log, WARN, "ILLEGAL_ARGUMENT", "Illegal argument provided", "message", ex.getMessage(), "path", request.getRequestURI());
        return errorResponseFactory.build(HttpStatus.BAD_REQUEST, "Invalid value provided: " + ex.getMessage(), ErrorCode.ILLEGAL_ARGUMENT, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleEnumError(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String message = "Invalid request";
        String fieldName = null;
        if (ex.getCause() instanceof InvalidFormatException e) {
            fieldName = e.getPath().getFirst().getFieldName();
            Object invalidValue = e.getValue();
            message = "Invalid value '" + invalidValue + "' for field '" + fieldName + "'. Allowed values: " + Arrays.toString(Role.values());
        }
        LogUtil.log(log, WARN, "INVALID_ENUM_VALUE", "Invalid enum value provided", "field", fieldName, "path", request.getRequestURI());
        return errorResponseFactory.build(HttpStatus.BAD_REQUEST, message, ErrorCode.INVALID_ENUM_VALUE, request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        LogUtil.log(log, WARN, "RESOURCE_NOT_FOUND", "Resource not found", "message", ex.getMessage(), "path", request.getRequestURI());
        return errorResponseFactory.build(HttpStatus.NOT_FOUND, ex.getMessage(), ex.getErrorCode(), request);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDuplicate(DuplicateResourceException ex, HttpServletRequest request) {
        LogUtil.log(log, WARN, "DUPLICATE_RESOURCE", "Duplicate resource attempted", "message", ex.getMessage(), "path", request.getRequestURI());
        return errorResponseFactory.build(HttpStatus.CONFLICT, ex.getMessage(), ex.getErrorCode(), request);
    }

    @ExceptionHandler({AuthorizationDeniedException.class, AccessDeniedException.class})
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleAccessDenied(Exception ex, HttpServletRequest request) {
        LogUtil.log(log, WARN, "ACCESS_DENIED", "Access denied", "exceptionType", ex.getClass().getSimpleName(), "path", request.getRequestURI());
        return errorResponseFactory.forbidden(ex.getMessage(), request);
    }


    @ExceptionHandler({AuthenticationException.class, UnauthorizedException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        LogUtil.log(log, WARN, "AUTHENTICATION_FAILED", "Authentication failed", "exceptionType", ex.getClass().getSimpleName(), "path", request.getRequestURI());
        return errorResponseFactory.unauthorized(ex.getMessage(), request);
    }

    @ExceptionHandler(CustomBadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequest(CustomBadRequestException ex, HttpServletRequest request) {
        LogUtil.log(log, WARN, "BAD_REQUEST", "Bad request", "message", ex.getMessage(), "path", request.getRequestURI());
        return errorResponseFactory.build(HttpStatus.BAD_REQUEST, ex.getMessage(), ex.getErrorCode(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        assert ex.getRequiredType() != null;
        String message = "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'. Expected type: " + ex.getRequiredType().getSimpleName();
        LogUtil.log(log, WARN, "TYPE_MISMATCH", "Type mismatch in request parameter", "parameter", ex.getName(), "expectedType", ex.getRequiredType().getSimpleName(), "path", request.getRequestURI());
        return errorResponseFactory.build(HttpStatus.BAD_REQUEST, message, ErrorCode.BAD_REQUEST, request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ErrorResponse handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        String supported = ex.getSupportedHttpMethods() == null
                ? ""
                : ex.getSupportedHttpMethods().stream()
                  .map(HttpMethod::name)
                  .reduce((a, b) -> a + ", " + b)
                  .orElse("");
        String message = "HTTP method '" + ex.getMethod() + "' is not supported for this endpoint. Supported methods: " + supported;
        LogUtil.log(log, WARN, "METHOD_NOT_ALLOWED", "HTTP method not allowed", "method", ex.getMethod(), "supportedMethods", supported, "path", request.getRequestURI());
        return errorResponseFactory.build(HttpStatus.METHOD_NOT_ALLOWED, message, ErrorCode.BAD_REQUEST, request);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNoHandlerFound(NoHandlerFoundException ex, HttpServletRequest request) {
        LogUtil.log(log, WARN, "NO_HANDLER_FOUND", "No handler found for request", "method", ex.getHttpMethod(), "path", ex.getRequestURL());
        return errorResponseFactory.build(HttpStatus.NOT_FOUND, "The requested resource was not found", ErrorCode.NOT_FOUND, request);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleRuntimeException(Exception ex, HttpServletRequest request) {
        LogUtil.log(log, ERROR, "INTERNAL_SERVER_ERROR", "Internal server error occurred", "exceptionType", ex.getClass().getSimpleName(), "message", ex.getMessage(), "path", request.getRequestURI());
        log.debug("Exception details", ex);
        return errorResponseFactory.build(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong", ErrorCode.INTERNAL_ERROR, request);
    }
}
