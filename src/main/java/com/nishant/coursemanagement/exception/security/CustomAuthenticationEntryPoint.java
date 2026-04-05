package com.nishant.coursemanagement.exception.security;

import com.nishant.coursemanagement.exception.response.ErrorResponse;
import com.nishant.coursemanagement.exception.response.ErrorResponseFactory;
import com.nishant.coursemanagement.exception.response.ErrorResponseWriter;
import com.nishant.coursemanagement.util.LogUtil;
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
                         @NonNull AuthenticationException ex) throws IOException {
        try {
            LogUtil.put("action", "AUTHENTICATION_FAILED");
            LogUtil.put("path", request.getRequestURI());
            LogUtil.put("message", ex.getMessage());
            log.warn("Authentication failed");
        } finally {
            LogUtil.clear();
        }
        ErrorResponse error = errorResponseFactory.unauthorized("Authentication required", request);
        errorResponseWriter.write(response, error);
    }
}
