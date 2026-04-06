package com.nishant.coursemanagement.log.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Loggable {
    String action();
    String message() default "";
    LogLevel level() default LogLevel.INFO;
    // SpEL expressions referencing method args, e.g. "#request.email"
    String[] extras() default {};
    // MDC key names matching extras[] by index
    String[] extraKeys() default {};

    boolean includeCurrentUser() default false;
}
