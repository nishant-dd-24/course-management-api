package com.nishant.coursemanagement.exception.custom;


import com.nishant.coursemanagement.exception.ErrorCode;
import lombok.Getter;

@Getter
public class ResourceNotFoundException extends RuntimeException {
    private final ErrorCode errorCode = ErrorCode.NOT_FOUND;

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
