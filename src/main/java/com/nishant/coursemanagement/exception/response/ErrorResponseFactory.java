package com.nishant.coursemanagement.exception.response;

import com.nishant.coursemanagement.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

@Component
public class ErrorResponseFactory {
    public ErrorResponse build(HttpStatus status, String message, ErrorCode errorCode, HttpServletRequest request){
        return ErrorResponse.builder()
                .traceId((String) request.getAttribute("traceId"))
                .path(request.getRequestURI())
                .method(request.getMethod())
                .status(status.value())
                .message(message)
                .errorCode(errorCode)
                .timestamp(LocalDateTime.now())
                .errors(Collections.emptyMap())
                .build();
    }

    public ErrorResponse build(HttpStatus status, String message, ErrorCode errorCode, Map<String, String > errors, HttpServletRequest request){
        return ErrorResponse.builder()
                .traceId((String) request.getAttribute("traceId"))
                .path(request.getRequestURI())
                .method(request.getMethod())
                .status(status.value())
                .message(message)
                .errorCode(errorCode)
                .timestamp(LocalDateTime.now())
                .errors(errors)
                .build();
    }

    public ErrorResponse forbidden(String message, HttpServletRequest request){
        return build(HttpStatus.FORBIDDEN, message, ErrorCode.ACCESS_DENIED, request);
    }

    public ErrorResponse unauthorized(String message, HttpServletRequest request){
        return build(HttpStatus.UNAUTHORIZED, message, ErrorCode.UNAUTHENTICATED, request);
    }
}
