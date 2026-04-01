package com.nishant.coursemanagement.exception;

import com.nishant.coursemanagement.exception.custom.CustomBadRequestException;
import com.nishant.coursemanagement.exception.custom.DuplicateResourceException;
import com.nishant.coursemanagement.exception.custom.ResourceNotFoundException;
import com.nishant.coursemanagement.exception.custom.UnauthorizedException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class ExceptionUtil {

    public ResourceNotFoundException notFound(String message){
        return new ResourceNotFoundException(message);
    }

    public DuplicateResourceException duplicate(String message){
        return new DuplicateResourceException(message);
    }

    public AccessDeniedException accessDenied(String message){
        return new AccessDeniedException(message);
    }

    public UnauthorizedException unauthorized(String message){
        return new UnauthorizedException(message);
    }

    public CustomBadRequestException badRequest(String message){
        return new CustomBadRequestException(message);
    }
}
