package com.nishant.coursemanagement.log.aspect;

import com.nishant.coursemanagement.log.annotation.LogLevel;
import com.nishant.coursemanagement.log.annotation.Loggable;
import com.nishant.coursemanagement.security.AuthUtil;
import com.nishant.coursemanagement.log.util.LogUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class LoggingAspect {

    private final ExpressionParser parser = new SpelExpressionParser();
    private final AuthUtil authUtil;

    @Around("@annotation(loggable)")
    public Object log(ProceedingJoinPoint pjp, Loggable loggable) throws Throwable {
        long start = System.nanoTime();
        populateMdc(pjp, loggable);
        try {
            Object result = pjp.proceed();
            LogUtil.put("status", "SUCCESS");
            LogUtil.put("durationMs",
                    (System.nanoTime() - start) / 1_000_000);
            logAtLevel(loggable.level(), loggable.action(), loggable.message());
            return result;
        } catch (Throwable ex) {
            LogUtil.put("status", "SUCCESS");
            LogUtil.put("durationMs",
                    (System.nanoTime() - start) / 1_000_000);
            LogUtil.put("error", ex.getMessage());
            log.error("Exception in {}", loggable.action(), ex);
            throw ex;
        } finally {
            LogUtil.clear();
        }
    }

    private void populateMdc(ProceedingJoinPoint pjp, Loggable loggable) {
        LogUtil.put("action", loggable.action());

        if (loggable.includeCurrentUser()) {
            try {
                LogUtil.put("currentUserId", authUtil.getCurrentUser().getId());
            } catch (Exception ex) {
                log.debug("Failed to fetch current user", ex);
            }
        }

        String[] keys = loggable.extraKeys();
        String[] exprs = loggable.extras();
        if (exprs.length == 0) return;

        MethodSignature sig = (MethodSignature) pjp.getSignature();
        String[] params = sig.getParameterNames();
        Object[] args = pjp.getArgs();

        EvaluationContext ctx = new StandardEvaluationContext();
        for (int i = 0; i < params.length; i++) ctx.setVariable(params[i], args[i]);

        for (int i = 0; i < exprs.length; i++) {
            try {
                Object val = parser.parseExpression(exprs[i]).getValue(ctx);
                String key = (i < keys.length) ? keys[i] : "extra" + i;
                LogUtil.put(key, val);
            } catch (Exception ex) {
                log.debug("Failed to evaluate SpEL: {}", exprs[i], ex);
            }
        }
    }

    private void logAtLevel(LogLevel level, String action, String message) {
        String text = message.isBlank() ? action : message;
        switch (level) {
            case DEBUG -> log.debug(text);
            case WARN  -> log.warn(text);
            case ERROR -> log.error(text);
            default    -> log.info(text);
        }
    }
}