package com.nishant.coursemanagement.util;

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
}