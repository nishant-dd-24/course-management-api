package com.nishant.coursemanagement.exception.security;

import com.nishant.coursemanagement.exception.response.ErrorResponse;
import com.nishant.coursemanagement.exception.response.ErrorResponseFactory;
import com.nishant.coursemanagement.exception.response.ErrorResponseWriter;
import com.nishant.coursemanagement.log.util.LogUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;


import java.io.IOException;

import static com.nishant.coursemanagement.log.annotation.LogLevel.WARN;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ErrorResponseWriter errorResponseWriter;
    private final ErrorResponseFactory errorResponseFactory;

    @Override
    public void handle(@NonNull HttpServletRequest request,
                       @NonNull HttpServletResponse response,
                       @NonNull AccessDeniedException ex) throws IOException {
        LogUtil.log(log, WARN, "ACCESS_DENIED", ex.getMessage(), "path", request.getRequestURI());
        ErrorResponse error = errorResponseFactory.forbidden("Access Denied", request);
        errorResponseWriter.write(response, error);
    }
}
