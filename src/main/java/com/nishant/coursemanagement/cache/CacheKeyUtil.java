package com.nishant.coursemanagement.cache;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class CacheKeyUtil {
    public String buildCourseKey(String title, Boolean isActive, Long instructorId, Pageable pageable){
        String normalizedTitle = normalize(title);

        String sortKey = pageable.getSort().stream()
                .map(order -> order.getProperty() + ":" + order.getDirection())
                .collect(Collectors.joining(","));

        return String.format(
                "title=%s|isActive=%s|instructorId=%s|page=%d|size=%d|sort=%s",
                normalizedTitle,
                isActive,
                instructorId,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sortKey
        );
    }

    public String buildUserKey(String name, String email, Boolean isActive, Pageable pageable){

        boolean noFilter = (name == null && email == null && isActive == null);

        String normalizedName = normalize(name);
        String normalizedEmail = normalize(email);

        String sortKey = pageable.getSort().stream()
                .map(order -> order.getProperty() + ":" + order.getDirection())
                .collect(Collectors.joining(","));

        return String.format(
                "type=%s|name=%s|email=%s|isActive=%s|page=%d|size=%d|sort=%s",
                noFilter ? "all" : "filtered",
                normalizedName,
                normalizedEmail,
                isActive,
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
