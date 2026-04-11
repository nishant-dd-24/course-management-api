package com.nishant.coursemanagement.service;

import com.nishant.coursemanagement.entity.Role;
import com.nishant.coursemanagement.entity.User;
import com.nishant.coursemanagement.exception.ExceptionUtil;
import com.nishant.coursemanagement.exception.custom.CustomBadRequestException;
import com.nishant.coursemanagement.exception.custom.ResourceNotFoundException;
import com.nishant.coursemanagement.security.AuthUtil;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public abstract class BaseServiceTest {
    @Mock
    protected AuthUtil authUtil;

    @Mock
    protected ApplicationEventPublisher eventPublisher;

    @Mock
    protected ExceptionUtil exceptionUtil;

    protected User buildUser(Long id) {
        return User.builder()
                .id(id)
                .name("User " + id)
                .email("user" + id + "@email.com")
                .password("password")
                .role(Role.STUDENT)
                .isActive(true)
                .build();
    }

    protected User buildUser(Long id, Role role) {
        User user = buildUser(id);
        user.setRole(role);
        return user;
    }

    protected void mockCurrentUser(User user) {
        when(authUtil.getCurrentUser()).thenReturn(user);
    }

    protected void mockNotFoundException(String message) {
        when(exceptionUtil.notFound(message))
                .thenReturn(new ResourceNotFoundException(message));
    }

    protected void mockBadRequestException(String message) {
        when(exceptionUtil.badRequest(message))
                .thenReturn( new CustomBadRequestException(message));
    }

    protected void verifyNoEventPublished() {
        verify(eventPublisher, never()).publishEvent(any());
    }

    protected <T> T captureEvent(Class<T> clazz) {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());
        return clazz.cast(captor.getValue());
    }

    protected <T> List<T> captureEvents(Class<T> clazz, int expectedCount) {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(expectedCount)).publishEvent(captor.capture());
        return captor.getAllValues().stream()
                .map(clazz::cast)
                .collect(Collectors.toList());
    }

}
