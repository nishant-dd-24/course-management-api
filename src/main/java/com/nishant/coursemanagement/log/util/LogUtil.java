package com.nishant.coursemanagement.log.util;

import com.nishant.coursemanagement.log.annotation.LogLevel;
import org.slf4j.Logger;
import org.slf4j.MDC;

import java.util.HashSet;
import java.util.Set;

public class LogUtil {

    private static final ThreadLocal<Set<String>> addedKeys =
            ThreadLocal.withInitial(HashSet::new);

    public static void put(String key, Object value) {
        if (value != null) {
            MDC.put(key, String.valueOf(value));
            addedKeys.get().add(key);
        }
    }

    public static void clear() {
        addedKeys.get().forEach(MDC::remove);
        addedKeys.remove();
    }

    public static void log(Logger logger, LogLevel level, String action, String message, Object... kvPairs) {
        try {
            put("action", action);
            for (int i = 0; i + 1 < kvPairs.length; i += 2) {
                put(String.valueOf(kvPairs[i]), kvPairs[i + 1]);
            }
            switch (level) {
                case DEBUG -> logger.debug(message);
                case WARN  -> logger.warn(message);
                case ERROR -> logger.error(message);
                default    -> logger.info(message);
            }
        } finally {
            clear();
        }
    }
}