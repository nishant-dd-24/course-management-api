package com.nishant.coursemanagement.exception.response;



import com.nishant.coursemanagement.exception.ErrorCode;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

@Builder
public record ErrorResponse (
    String traceId,
    String path,
    String method,
    int status,
    String message,
    ErrorCode errorCode,
    LocalDateTime timestamp,
    Map<String, String> errors
){}