package com.nishant.coursemanagement.exception.custom;


import com.nishant.coursemanagement.exception.ErrorCode;
import lombok.Getter;

@Getter
public class DuplicateResourceException extends RuntimeException {
    private final ErrorCode errorCode = ErrorCode.ALREADY_EXISTS;

    public DuplicateResourceException(String message) {
        super(message);
    }
}
