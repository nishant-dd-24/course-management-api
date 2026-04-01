package com.nishant.coursemanagement.util;

public class StringUtil {
    public static boolean isNullOrBlank(String str) {
        return str == null || str.isBlank();
    }
    public static boolean isBlankButNotNull(String str) {
        return str != null && str.isBlank();
    }
    public static String normalize(String str){
        return isNullOrBlank(str) ? null : str.trim();
    }
}
