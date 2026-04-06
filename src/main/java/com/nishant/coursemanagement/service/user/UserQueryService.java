package com.nishant.coursemanagement.service.user;

import com.nishant.coursemanagement.dto.common.PageResponse;
import com.nishant.coursemanagement.dto.user.UserResponse;
import com.nishant.coursemanagement.entity.User;
import com.nishant.coursemanagement.exception.ExceptionUtil;
import com.nishant.coursemanagement.mapper.PageMapper;
import com.nishant.coursemanagement.mapper.UserMapper;
import com.nishant.coursemanagement.repository.user.UserRepository;
import com.nishant.coursemanagement.log.annotation.Loggable;
import com.nishant.coursemanagement.log.util.LogUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.nishant.coursemanagement.log.annotation.LogLevel.DEBUG;
import static com.nishant.coursemanagement.log.annotation.LogLevel.WARN;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryService {
    private final UserRepository userRepository;
    private final ExceptionUtil exceptionUtil;

    @Loggable(
            action = "QUERY_GET_USER_BY_ID",
            extras = {"#id"},
            extraKeys = {"userId"},
            level = DEBUG
    )
    public User getUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> exceptionUtil.notFound("User not found"));
    }

    @Loggable(
            action = "QUERY_GET_USER_BY_EMAIL",
            extras = {"#email"},
            extraKeys = {"email"},
            level = DEBUG
    )
    public User getUser(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> exceptionUtil.notFound("Invalid credentials"));
    }

    @Cacheable(sync = true, value = "userById", key = "#id")
    @Loggable(
            action = "QUERY_GET_USER_RESPONSE",
            extras = {"#id"},
            extraKeys = {"userId"},
            level = DEBUG
    )
    public UserResponse getUserResponse(Long id) {
        return UserMapper.toResponse(getUser(id));
    }

    @Cacheable(
            sync = true,
            value = "users",
            key = "@cacheKeyUtil.buildUserKey(#name, #email, #active, #pageable)"
    )
    @Loggable(
            action = "QUERY_GET_ALL_USERS",
            extras = {"#name", "#email", "#active", "#pageable.getPageNumber()", "#pageable.getPageSize()"},
            extraKeys = {"name", "email", "active", "pageNumber", "pageSize"},
            level = DEBUG
    )
    public PageResponse<UserResponse> getAllUsers(String name, String email, Boolean active, Pageable pageable) {
        if (name == null && email == null && active == null) {
            LogUtil.log(log, WARN, "QUERY_GET_ALL_USERS_NO_FILTER", "Querying all users without filters", "pageNumber", pageable.getPageNumber(), "pageSize", pageable.getPageSize());
            return PageMapper.map(userRepository.findAll(pageable), UserMapper::toResponse);
        }
        return PageMapper.map(userRepository.findUsers(name, email, active, pageable),
                UserMapper::toResponse);
    }
}
