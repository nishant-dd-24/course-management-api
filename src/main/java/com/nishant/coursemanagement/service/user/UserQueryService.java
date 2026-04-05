package com.nishant.coursemanagement.service.user;

import com.nishant.coursemanagement.dto.common.PageResponse;
import com.nishant.coursemanagement.dto.user.UserResponse;
import com.nishant.coursemanagement.entity.User;
import com.nishant.coursemanagement.exception.ExceptionUtil;
import com.nishant.coursemanagement.mapper.PageMapper;
import com.nishant.coursemanagement.mapper.UserMapper;
import com.nishant.coursemanagement.repository.user.UserRepository;
import com.nishant.coursemanagement.util.LogUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryService {
    private final UserRepository userRepository;
    private final ExceptionUtil exceptionUtil;

    public User getUser(Long id) {
        try {
            LogUtil.put("action", "QUERY_GET_USER_BY_ID");
            LogUtil.put("userId", id);
            log.debug("Querying user by ID");
        } finally {
            LogUtil.clear();
        }
        return userRepository.findById(id).orElseThrow(() -> exceptionUtil.notFound("User not found"));
    }

    public User getUser(String email) {
        try {
            LogUtil.put("action", "QUERY_GET_USER_BY_EMAIL");
            LogUtil.put("email", email);
            log.debug("Querying user by email");
        } finally {
            LogUtil.clear();
        }
        return userRepository.findByEmail(email).orElseThrow(() -> exceptionUtil.notFound("Invalid credentials"));
    }

    @Cacheable(sync = true, value = "userById", key = "#id")
    public UserResponse getUserResponse(Long id) {
        try {
            LogUtil.put("action", "QUERY_GET_USER_RESPONSE");
            LogUtil.put("userId", id);
            log.debug("Querying user response");
        } finally {
            LogUtil.clear();
        }
        return UserMapper.toResponse(getUser(id));
    }

    @Cacheable(
            sync = true,
            value = "users",
            key = "@cacheKeyUtil.buildUserKey(#name, #email, #active, #pageable)"
    )
    public PageResponse<UserResponse> getAllUsers(String name, String email, Boolean active, Pageable pageable) {
        try {
            LogUtil.put("action", "QUERY_GET_ALL_USERS");
            LogUtil.put("name", name);
            LogUtil.put("email", email);
            LogUtil.put("active", active);
            LogUtil.put("pageNumber", pageable.getPageNumber());
            LogUtil.put("pageSize", pageable.getPageSize());
            log.debug("Querying all users");
        } finally {
            LogUtil.clear();
        }
        if (name == null && email == null && active == null) {
            try {
                LogUtil.put("action", "QUERY_GET_ALL_USERS_NO_FILTER");
                LogUtil.put("pageNumber", pageable.getPageNumber());
                LogUtil.put("pageSize", pageable.getPageSize());
                log.warn("Querying all users without filters");
            } finally {
                LogUtil.clear();
            }
            return PageMapper.map(userRepository.findAll(pageable), UserMapper::toResponse);
        }
        return PageMapper.map(userRepository.findUsers(name, email, active, pageable),
                UserMapper::toResponse);
    }
}
