package com.nishant.coursemanagement.controller.common;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RootController {

    public static final String SWAGGER_UI_URL = "/swagger-ui/index.html";
    public static final String SWAGGER_UI_REDIRECT = "redirect:" + SWAGGER_UI_URL;

    @GetMapping("/")
    public String root() {
        return SWAGGER_UI_REDIRECT;
    }

    @GetMapping("/docs")
    public String docs() {
        return SWAGGER_UI_REDIRECT;
    }
}
