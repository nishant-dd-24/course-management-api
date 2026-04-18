package com.nishant.coursemanagement.integration.cache;

import com.nishant.coursemanagement.dto.user.UserAdminUpdateRequest;
import com.nishant.coursemanagement.entity.Role;
import com.nishant.coursemanagement.integration.BaseIntegrationTest;
import com.nishant.coursemanagement.repository.user.UserRepository;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class CacheFlowIT extends BaseIntegrationTest {

    private static final String USERS_ENDPOINT = "/users";
    private static final String USER_BY_ID_ENDPOINT = "/users/%d";

    @Autowired
    private CaffeineCacheManager caffeineCacheManager;

    @MockitoSpyBean
    private UserRepository userRepository;

    @Test
    void shouldServeSecondGetFromCache_whenRequestIsIdentical() throws Exception {
        setAdminToken();
        clearInvocations(userRepository);

        MvcResult first = performGetUsersPage0();
        MvcResult second = performGetUsersPage0();

        assertEquals(first.getResponse().getContentAsString(), second.getResponse().getContentAsString());
        verify(userRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    void shouldUseL2WithoutDbHit_whenL1IsCleared() throws Exception {
        setAdminToken();
        clearInvocations(userRepository);

        performGetUsersPage0();

        Cache usersL1 = caffeineCacheManager.getCache("users");
        if (usersL1 != null) {
            usersL1.clear();
        }

        performGetUsersPage0();

        verify(userRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    void shouldHitDbAgainAfterWrite_whenCacheIsEvicted() throws Exception {
        setAdminToken();
        clearInvocations(userRepository);

        performGetUsersPage0();

        UserAdminUpdateRequest request = UserAdminUpdateRequest.builder()
                .name("Updated Name")
                .email("updated.email@example.com")
                .role(Role.ADMIN)
                .isActive(true)
                .build();

        mockMvc.perform(put(String.format(USER_BY_ID_ENDPOINT, testUser.getId()))
                        .with(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk());

        Awaitility.await()
                .atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    performGetUsersPage0();
                    verify(userRepository, times(2)).findAll(any(Pageable.class));
                });
    }

    @Test
    void shouldPreventStampede_whenConcurrentGetsUseSameCacheKey() throws Exception {
        setAdminToken();
        clearInvocations(userRepository);

        int requestCount = 12;
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < requestCount; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    assertTrue(start.await(5, TimeUnit.SECONDS));
                    return performGetUsersPage0().getResponse().getStatus();
                }));
            }

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            for (Future<Integer> future : futures) {
                assertEquals(200, future.get(10, TimeUnit.SECONDS));
            }
        } finally {
            executor.shutdownNow();
        }

        verify(userRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    void shouldEvictCacheAfterUserUpdate_andHitDbOnNextGet() throws Exception {
        setAdminToken();
        clearInvocations(userRepository);

        performGetUsersPage0();

        UserAdminUpdateRequest request = UserAdminUpdateRequest.builder()
                .name("Updated Again")
                .email("updated.again@example.com")
                .role(Role.ADMIN)
                .isActive(true)
                .build();

        mockMvc.perform(put(String.format(USER_BY_ID_ENDPOINT, testUser.getId()))
                        .with(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk());

        Awaitility.await()
                .atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    performGetUsersPage0();
                    verify(userRepository, times(2)).findAll(any(Pageable.class));
                });
    }

    @Test
    void shouldNotCache_whenPageIsNotZero() throws Exception {
        setAdminToken();
        clearInvocations(userRepository);

        performGetUsers("1", null, null, null);
        performGetUsers("1", null, null, null);

        verify(userRepository, times(2)).findAll(any(Pageable.class));
    }

    @Test
    void shouldUseDifferentCacheEntries_forDifferentFilters() throws Exception {
        setAdminToken();
        clearInvocations(userRepository);

        performGetUsers("0", "test", null, null);
        performGetUsers("0", "test", null, null);

        performGetUsers("0", "dummy", null, null);
        performGetUsers("0", "dummy", null, null);

        verify(userRepository, times(2)).findUsers(any(), any(), any(), any(Pageable.class));
    }

    @Test
    void shouldReturnUpdatedData_afterEviction() throws Exception {
        setAdminToken();
        clearInvocations(userRepository);

        MvcResult first = performGetUsersPage0();
        assertTrue(first.getResponse().getContentAsString().contains(EMAIL));

        String updatedEmail = "fresh.updated@email.com";
        UserAdminUpdateRequest request = UserAdminUpdateRequest.builder()
                .name("Fresh Updated")
                .email(updatedEmail)
                .role(Role.ADMIN)
                .isActive(true)
                .build();

        mockMvc.perform(put(String.format(USER_BY_ID_ENDPOINT, testUser.getId()))
                        .with(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk());

        Awaitility.await()
                .atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    MvcResult updated = performGetUsersPage0();
                    assertTrue(updated.getResponse().getContentAsString().contains(updatedEmail));
                    verify(userRepository, times(2)).findAll(any(Pageable.class));
                });
    }

    private MvcResult performGetUsersPage0() throws Exception {
        return performGetUsers("0", null, null, null);
    }

    private MvcResult performGetUsers(String page, String name, String email, String isActive) throws Exception {
        MockHttpServletRequestBuilder request = get(USERS_ENDPOINT)
                .with(auth())
                .param("page", page);

        if (name != null) {
            request.param("name", name);
        }
        if (email != null) {
            request.param("email", email);
        }
        if (isActive != null) {
            request.param("isActive", isActive);
        }

        return mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn();
    }
}
