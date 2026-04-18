package com.nishant.coursemanagement.integration.controller.user;

import com.jayway.jsonpath.JsonPath;
import com.nishant.coursemanagement.dto.user.*;
import com.nishant.coursemanagement.integration.BaseIntegrationTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;


import static com.nishant.coursemanagement.entity.Role.INSTRUCTOR;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserFlowIT extends BaseIntegrationTest {


    private static final String ROOT_ENDPOINT = "/users";
    private static final String REGISTER_ENDPOINT = ROOT_ENDPOINT + "/register";
    private static final String LOGIN_ENDPOINT = ROOT_ENDPOINT + "/login";
    private static final String REFRESH_ENDPOINT = ROOT_ENDPOINT + "/refresh";
    private static final String LOGOUT_ENDPOINT = ROOT_ENDPOINT + "/logout";
    private static final String MY_ENDPOINT = ROOT_ENDPOINT + "/my";
    private static final String ID_ENDPOINT = ROOT_ENDPOINT + "/%d";
    private static final String CHANGE_PASSWORD_ENDPOINT = MY_ENDPOINT + "/change-password";

    private static final String NEW_NAME = "New User";
    private static final String NEW_EMAIL = "newuser@email.com";
    private static final String WRONG_PASSWORD = "wrong_password";
    private static final String NEW_PASSWORD = "new_password";

    private String loginAndGetAccessToken() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email(EMAIL)
                .password(PASSWORD)
                .build();

        MvcResult result = mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private String loginAndGetRefreshToken() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email(EMAIL)
                .password(PASSWORD)
                .build();

        MvcResult result = mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        return JsonPath.read(result.getResponse().getContentAsString(), "$.refreshToken");
    }

    private String[] loginAndGetTokens() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email(EMAIL)
                .password(PASSWORD)
                .build();

        MvcResult result = mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        return new String[]{
                JsonPath.read(response, "$.accessToken"),
                JsonPath.read(response, "$.refreshToken")
        };
    }

    @Nested
    class AuthTests {

        private UserRequest userRequest;
        private LoginRequest loginRequest;

        private void buildUserRequest(){
            userRequest = UserRequest.builder()
                    .name(NEW_NAME)
                    .email(NEW_EMAIL)
                    .password(PASSWORD)
                    .build();
        }

        private void buildLoginRequest(String pass){
            loginRequest = LoginRequest.builder()
                    .email(EMAIL)
                    .password(pass)
                    .build();
        }

        @Test
        void shouldRegisterUserSuccessfully() throws Exception {
            buildUserRequest();
            mockMvc.perform(post(REGISTER_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(userRequest)))
                    .andExpect(status().isCreated());

            assertTrue(userRepository.findByEmail(NEW_EMAIL).isPresent());
        }

        @Test
        void shouldThrowConflict_whenRegisteringWithDuplicateEmail() throws Exception {
            buildUserRequest();

            mockMvc.perform(post(REGISTER_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(userRequest)))
                    .andExpect(status().isCreated());

            long countBefore = userRepository.count();

            mockMvc.perform(post(REGISTER_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(userRequest)))
                    .andExpect(status().isConflict());

            assertEquals(countBefore, userRepository.count());
        }

        @Test
        void shouldLoginSuccessfully_andReturnToken() throws Exception {
            buildLoginRequest(PASSWORD);
            MvcResult result = mockMvc.perform(post(LOGIN_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(loginRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            String response = result.getResponse().getContentAsString();

            String accessToken = JsonPath.read(response, "$.accessToken");
            String refreshToken = JsonPath.read(response, "$.refreshToken");
            String email = JsonPath.read(response, "$.user.email");

            assertNotNull(accessToken);
            assertFalse(accessToken.isBlank());
            assertNotNull(refreshToken);
            assertFalse(refreshToken.isBlank());
            assertEquals(EMAIL, email);
        }

        @Test
        void shouldRefreshTokenSuccessfully() throws Exception {
            buildLoginRequest(PASSWORD);
            MvcResult loginResult = mockMvc.perform(post(LOGIN_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(loginRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            String loginResponse = loginResult.getResponse().getContentAsString();
            String oldAccessToken = JsonPath.read(loginResponse, "$.accessToken");
            String refreshToken = JsonPath.read(loginResponse, "$.refreshToken");

            RefreshRequest refreshRequest = RefreshRequest.builder()
                    .refreshToken(refreshToken)
                    .build();

            MvcResult refreshResult = mockMvc.perform(post(REFRESH_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(refreshRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").value(refreshToken))
                    .andExpect(jsonPath("$.user.email").value(EMAIL))
                    .andReturn();

            String refreshedResponse = refreshResult.getResponse().getContentAsString();
            String newAccessToken = JsonPath.read(refreshedResponse, "$.accessToken");
            assertNotEquals(oldAccessToken, newAccessToken);
        }


        @Test
        void shouldThrowUnauthorized_whenLoginWithWrongPassword() throws Exception {
            buildLoginRequest(WRONG_PASSWORD);

            mockMvc.perform(post(LOGIN_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(loginRequest)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldThrowUnauthorized_whenLoginWithInactiveUser() throws Exception {
            testUser.setIsActive(false);
            userRepository.save(testUser);

            buildLoginRequest(PASSWORD);

            mockMvc.perform(post(LOGIN_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(loginRequest)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class AuthorizationTests{

        @Test
        void shouldReturnUnauthorized_whenAccessWithoutToken() throws Exception {
            mockMvc.perform(get(ROOT_ENDPOINT))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldReturnForbidden_whenAccessUsersAsStudent() throws Exception {
            setStudentToken();

            mockMvc.perform(get(ROOT_ENDPOINT)
                            .with(auth()))
                    .andExpect(status().isForbidden());
        }

        @Test
        void shouldAllowAccess_whenAccessUsersAsAdmin() throws Exception {
            setAdminToken();

            mockMvc.perform(get(ROOT_ENDPOINT)
                            .with(auth()))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    class RetrievalTests{

        @Test
        void shouldGetCurrentUserSuccessfully() throws Exception {
            mockMvc.perform(get(MY_ENDPOINT)
                            .with(auth()))
                    .andExpect(status().isOk());
        }

        @Test
        void shouldGetUserById_whenAdmin() throws Exception {
            setAdminToken();
            mockMvc.perform(get(String.format(ID_ENDPOINT, testUser.getId()))
                            .with(auth()))
                    .andExpect(status().isOk());
        }

        @Test
        void shouldReturnForbidden_whenGetUserByIdAsNonAdmin() throws Exception {
            setInstructorToken();
            mockMvc.perform(get(String.format(ID_ENDPOINT, testUser.getId()))
                            .with(auth()))
                    .andExpect(status().isForbidden());
        }

        @Test
        void shouldGetUsersWithPagination() throws Exception {
            setAdminToken();
            buildThisManyUsers();

            mockMvc.perform(get(ROOT_ENDPOINT)
                            .with(auth()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(5))
                    .andExpect(jsonPath("$.pageNumber").value(0))
                    .andExpect(jsonPath("$.pageSize").value(5))
                    .andExpect(jsonPath("$.totalElements").value(userRepository.count()));

        }

        @Test
        void shouldFilterUsersByNameEmailAndActive() throws Exception {
            setAdminToken();
            buildThisManyUsers();
            buildUser(NEW_NAME, NEW_EMAIL);

            mockMvc.perform(get(ROOT_ENDPOINT)
                            .param("name", NEW_NAME)
                            .param("email", NEW_EMAIL)
                            .param("isActive", "true")
                            .with(auth()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].name").value(NEW_NAME))
                    .andExpect(jsonPath("$.content[0].email").value(NEW_EMAIL));

        }
    }

    @Nested
    class UpdateTests{

        private UserAdminUpdateRequest userAdminUpdateRequest;
        private UserSelfUpdateRequest userSelfUpdateRequest;

        private void buildUserUpdateRequest(){
            userAdminUpdateRequest = UserAdminUpdateRequest.builder()
                    .name(NEW_NAME)
                    .email(NEW_EMAIL)
                    .role(INSTRUCTOR)
                    .isActive(true)
                    .build();
        }

        private void buildUserSelfUpdateRequest(){
            userSelfUpdateRequest = UserSelfUpdateRequest.builder()
                    .name(NEW_NAME)
                    .email(NEW_EMAIL)
                    .build();
        }

        @Test
        void
        shouldUpdateUserSuccessfully() throws Exception {
            setAdminToken();
            buildUser(DUMMY_NAME, DUMMY_EMAIL);
            buildUserUpdateRequest();

            mockMvc.perform(put(String.format(ID_ENDPOINT, dummyUser.getId()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(userAdminUpdateRequest))
                            .with(auth()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value(userAdminUpdateRequest.name()))
                    .andExpect(jsonPath("$.email").value(userAdminUpdateRequest.email()))
                    .andExpect(jsonPath("$.role").value(userAdminUpdateRequest.role().toString()))
                    .andExpect(jsonPath("$.isActive").value(userAdminUpdateRequest.isActive()));

        }

        @Test
        void shouldUpdateOwnProfileSuccessfully_andShouldNotUpdateRole_whenNonAdmin() throws Exception {
            buildUserSelfUpdateRequest();
            setStudentToken();
            mockMvc.perform(put(MY_ENDPOINT)
                            .with(auth())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(userSelfUpdateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value(userSelfUpdateRequest.name()))
                    .andExpect(jsonPath("$.email").value(userSelfUpdateRequest.email()))
                    .andExpect(jsonPath("$.role").value(testUser.getRole().toString()));
        }
    }

    @Nested
    class PatchTests{
        private UserAdminPatchRequest userAdminPatchRequest;
        private UserSelfPatchRequest userSelfPatchRequest;
        private void buildUserPatchRequest(){
            userAdminPatchRequest = UserAdminPatchRequest.builder()
                    .name(NEW_NAME)
                    .role(INSTRUCTOR)
                    .build();
        }

        private void buildUserSelfPatchRequest(){
            userSelfPatchRequest = UserSelfPatchRequest.builder()
                    .name(NEW_NAME)
                    .build();
        }

        @Test
        void shouldPatchUserSuccessfully() throws Exception {
            setAdminToken();
            buildUser(DUMMY_NAME, DUMMY_EMAIL);
            buildUserPatchRequest();
            mockMvc.perform(patch(String.format(ID_ENDPOINT, dummyUser.getId()))
                            .with(auth())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(userAdminPatchRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value(userAdminPatchRequest.name()))
                    .andExpect(jsonPath("$.email").value(dummyUser.getEmail()))
                    .andExpect(jsonPath("$.role").value(userAdminPatchRequest.role().toString()));
        }

        @Test
        void shouldThrowBadRequest_whenPatchRequestEmpty() throws Exception {
            setAdminToken();
            buildUser(DUMMY_NAME, DUMMY_EMAIL);
            userAdminPatchRequest = UserAdminPatchRequest.builder()
                    .name("  ")
                    .build();

            mockMvc.perform(patch(String.format(ID_ENDPOINT, dummyUser.getId()))
                            .with(auth())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(userAdminPatchRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldPatchOwnProfileSuccessfully_andShouldNotPatchRole_whenNonAdmin() throws Exception {
            buildUserSelfPatchRequest();
            setInstructorToken();
            mockMvc.perform(patch(MY_ENDPOINT)
                            .with(auth())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(userSelfPatchRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value(userSelfPatchRequest.name()))
                    .andExpect(jsonPath("$.email").value(testUser.getEmail()))
                    .andExpect(jsonPath("$.role").value(testUser.getRole().toString()));
        }
    }

    @Nested
    class DeleteTests{

        @Test
        void shouldDeactivateUserSuccessfully() throws Exception {
            setAdminToken();
            buildUser(DUMMY_NAME, DUMMY_EMAIL);

            mockMvc.perform(delete(String.format(ID_ENDPOINT, dummyUser.getId()))
                                .with(auth()))
                    .andExpect(status().isNoContent());
        }

        @Test
        void shouldDeactivateOwnAccountSuccessfully() throws Exception {
            setInstructorToken();
            mockMvc.perform(delete(MY_ENDPOINT)
                            .with(auth()))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    class PasswordTests{
        private NewPasswordRequest newPasswordRequest;

        private void buildNewPasswordRequest(String oldPassword){
            newPasswordRequest = NewPasswordRequest.builder()
                    .oldPassword(oldPassword)
                    .newPassword(NEW_PASSWORD)
                    .confirmPassword(NEW_PASSWORD)
                    .build();
        }

        @Test
        void shouldChangePasswordSuccessfully() throws Exception {
            buildNewPasswordRequest(PASSWORD);
            mockMvc.perform(post(CHANGE_PASSWORD_ENDPOINT)
                            .with(auth())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(newPasswordRequest)))
                    .andExpect(status().isOk());
        }

        @Test
        void shouldThrowBadRequest_whenPasswordsDoNotMatch() throws Exception {
            newPasswordRequest = new NewPasswordRequest(PASSWORD, NEW_PASSWORD, WRONG_PASSWORD);
            mockMvc.perform(post(CHANGE_PASSWORD_ENDPOINT)
                            .with(auth())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(newPasswordRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldThrowUnauthorized_whenOldPasswordIncorrect() throws Exception {
            buildNewPasswordRequest(WRONG_PASSWORD);
            mockMvc.perform(post(CHANGE_PASSWORD_ENDPOINT)
                            .with(auth())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(newPasswordRequest)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class LogoutTests {

        @Test
        void shouldReturnUnauthorized_whenAccessingProtectedEndpointAfterLogout() throws Exception {
            String accessToken = loginAndGetAccessToken();

            mockMvc.perform(post(LOGOUT_ENDPOINT)
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get(MY_ENDPOINT)
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldReturnUnauthorized_whenRefreshingAfterLogoutWithRefreshToken() throws Exception {
            String[] tokens = loginAndGetTokens();
            String accessToken = tokens[0];
            String refreshToken = tokens[1];

            mockMvc.perform(post(LOGOUT_ENDPOINT)
                            .header("Authorization", "Bearer " + accessToken)
                            .header("X-Refresh-Token", refreshToken))
                    .andExpect(status().isNoContent());

            RefreshRequest refreshRequest = RefreshRequest.builder()
                    .refreshToken(refreshToken)
                    .build();

            mockMvc.perform(post(REFRESH_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(refreshRequest)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldNotAffectOtherUsers_whenOneUserLogsOut() throws Exception {
            String[] user1Tokens = loginAndGetTokens();
            String user1AccessToken = user1Tokens[0];

            buildUser(DUMMY_NAME, DUMMY_EMAIL);
            String user2Token = generateToken(dummyUser);

            mockMvc.perform(post(LOGOUT_ENDPOINT)
                            .header("Authorization", "Bearer " + user1AccessToken))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get(MY_ENDPOINT)
                            .header("Authorization", "Bearer " + user1AccessToken))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(get(MY_ENDPOINT)
                            .header("Authorization", "Bearer " + user2Token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(DUMMY_EMAIL));
        }

        @Test
        void shouldReturnUnauthorized_whenAuthorizationHeaderMalformed() throws Exception {
            String accessToken = loginAndGetAccessToken();

            mockMvc.perform(post(LOGOUT_ENDPOINT)
                            .header("Authorization", "Token " + accessToken))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldNotCrash_whenLogoutWithBlankRefreshToken() throws Exception {
            String accessToken = loginAndGetAccessToken();

            mockMvc.perform(post(LOGOUT_ENDPOINT)
                            .header("Authorization", "Bearer " + accessToken)
                            .header("X-Refresh-Token", "   "))
                    .andExpect(status().isNoContent());
        }

        @Test
        void shouldReturnUnauthorized_whenRefreshingAfterLogout() throws Exception {
            String[] tokens = loginAndGetTokens();
            String accessToken = tokens[0];
            String refreshToken = tokens[1];

            mockMvc.perform(post(LOGOUT_ENDPOINT)
                            .header("Authorization", "Bearer " + accessToken)
                            .header("X-Refresh-Token", refreshToken))
                    .andExpect(status().isNoContent());

            RefreshRequest refreshRequest = RefreshRequest.builder()
                    .refreshToken(refreshToken)
                    .build();

            mockMvc.perform(post(REFRESH_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(refreshRequest)))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(get(MY_ENDPOINT)
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class RefreshTokenTests {

        @Test
        void shouldAllowRefreshTokenReuse_whenTokenStillValid() throws Exception {
            String refreshToken = loginAndGetRefreshToken();

            RefreshRequest refreshRequest = RefreshRequest.builder()
                    .refreshToken(refreshToken)
                    .build();

            MvcResult firstRefresh = mockMvc.perform(post(REFRESH_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(refreshRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andReturn();

            MvcResult secondRefresh = mockMvc.perform(post(REFRESH_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(refreshRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andReturn();

            String firstAccessToken = JsonPath.read(
                    firstRefresh.getResponse().getContentAsString(), "$.accessToken");
            String secondAccessToken = JsonPath.read(
                    secondRefresh.getResponse().getContentAsString(), "$.accessToken");

            assertNotEquals(firstAccessToken, secondAccessToken);
        }

        @Test
        void shouldReturnUnauthorized_whenRefreshTokenTampered() throws Exception {
            String refreshToken = loginAndGetRefreshToken();
            String tamperedToken = refreshToken.substring(0, refreshToken.length() - 5) + "XXXXX";

            RefreshRequest refreshRequest = RefreshRequest.builder()
                    .refreshToken(tamperedToken)
                    .build();

            mockMvc.perform(post(REFRESH_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(refreshRequest)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldReturnBadRequest_whenRefreshTokenEmpty() throws Exception {
            RefreshRequest refreshRequest = RefreshRequest.builder()
                    .refreshToken("")
                    .build();

            mockMvc.perform(post(REFRESH_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(refreshRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class SessionLifecycleTests {

        @Test
        void shouldCompleteFullSessionLifecycle() throws Exception {
            String[] tokens = loginAndGetTokens();
            String accessToken = tokens[0];
            String refreshToken = tokens[1];

            mockMvc.perform(get(MY_ENDPOINT)
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(EMAIL));

            RefreshRequest refreshRequest = RefreshRequest.builder()
                    .refreshToken(refreshToken)
                    .build();

            MvcResult refreshResult = mockMvc.perform(post(REFRESH_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(refreshRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andReturn();

            String newAccessToken = JsonPath.read(
                    refreshResult.getResponse().getContentAsString(), "$.accessToken");

            mockMvc.perform(get(MY_ENDPOINT)
                            .header("Authorization", "Bearer " + newAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(EMAIL));

            mockMvc.perform(post(LOGOUT_ENDPOINT)
                            .header("Authorization", "Bearer " + newAccessToken)
                            .header("X-Refresh-Token", refreshToken))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get(MY_ENDPOINT)
                            .header("Authorization", "Bearer " + newAccessToken))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(post(REFRESH_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(refreshRequest)))
                    .andExpect(status().isUnauthorized());
        }
    }

}
