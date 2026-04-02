package com.nishant.coursemanagement.exception.response;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public void write(HttpServletResponse response, ErrorResponse error) throws IOException {
        if(response.isCommitted()) return;
        response.resetBuffer();
        response.setStatus(error.status());
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(error));
        response.flushBuffer();
    }
}