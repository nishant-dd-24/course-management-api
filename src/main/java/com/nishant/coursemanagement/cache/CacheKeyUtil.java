package com.nishant.coursemanagement.cache;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class CacheKeyUtil {
    public static String buildCourseKey(String title, Boolean active, Long instructorId, Pageable pageable){
        String normalizedTitle = normalize(title);

        String sortKey = pageable.getSort().stream()
                .map(order -> order.getProperty() + ":" + order.getDirection())
                .collect(Collectors.joining(","));

        return String.format(
                "title=%s|active=%s|instructorId=%s|page=%d|size=%d|sort=%s",
                normalizedTitle,
                active,
                instructorId,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sortKey
        );
    }

    public static String buildUserKey(String name, String email, Boolean active, Pageable pageable){

        boolean noFilter = (name == null && email == null && active == null);

        String normalizedName = normalize(name);
        String normalizedEmail = normalize(email);

        String sortKey = pageable.getSort().stream()
                .map(order -> order.getProperty() + ":" + order.getDirection())
                .collect(Collectors.joining(","));

        return String.format(
                "type=%s|name=%s|email=%s|active=%s|page=%d|size=%d|sort=%s",
                noFilter ? "all" : "filtered",
                normalizedName,
                normalizedEmail,
                active,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sortKey
        );
    }

    private static String normalize(String str){
        if(str==null) return null;
        str = str.replace("%", "");
        return str.trim().toLowerCase();
    }
}
