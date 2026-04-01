package com.nishant.coursemanagement.exception.security;

import com.nishant.coursemanagement.exception.response.ErrorResponse;
import com.nishant.coursemanagement.exception.response.ErrorResponseFactory;
import com.nishant.coursemanagement.exception.response.ErrorResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ErrorResponseWriter errorResponseWriter;
    private final ErrorResponseFactory errorResponseFactory;

    @Override
    public void commence(@NonNull HttpServletRequest request,
                         @NonNull HttpServletResponse response,
                         @NonNull AuthenticationException ex) throws IOException{
        log.warn("action=AUTHENTICATION_FAILED path={} message={}", request.getRequestURI(), ex.getMessage());
        ErrorResponse error = errorResponseFactory.unauthorized(ex.getMessage());
        errorResponseWriter.write(response, error);
    }
}
