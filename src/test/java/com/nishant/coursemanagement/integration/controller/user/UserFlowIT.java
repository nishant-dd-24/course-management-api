package com.nishant.coursemanagement.integration.controller.user;

import com.jayway.jsonpath.JsonPath;
import com.nishant.coursemanagement.dto.user.*;
import com.nishant.coursemanagement.integration.controller.BaseIntegrationTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static com.nishant.coursemanagement.entity.Role.INSTRUCTOR;
import static com.nishant.coursemanagement.entity.Role.STUDENT;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserFlowIT extends BaseIntegrationTest {

    private static final String ROOT_ENDPOINT = "/users";
    private static final String REGISTER_ENDPOINT = ROOT_ENDPOINT + "/register";
    private static final String LOGIN_ENDPOINT = ROOT_ENDPOINT + "/login";
    private static final String MY_ENDPOINT = ROOT_ENDPOINT + "/my";
    private static final String ID_ENDPOINT = ROOT_ENDPOINT + "/%d";
    private static final String CHANGE_PASSWORD_ENDPOINT = MY_ENDPOINT + "/change-password";

    private static final String NEW_NAME = "New User";
    private static final String NEW_EMAIL = "newuser@email.com";
    private static final String WRONG_PASSWORD = "wrong_password";
    private static final String NEW_PASSWORD = "new_password";

    @Nested
    class AuthTests {

        private UserRequest userRequest;
        private LoginRequest loginRequest;

        private void buildUserRequest(){
            userRequest = UserRequest.builder()
                    .name(NEW_NAME)
                    .email(NEW_EMAIL)
                    .password(PASSWORD)
                    .role(STUDENT)
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

            String token = JsonPath.read(response, "$.token");
            String email = JsonPath.read(response, "$.user.email");

            assertNotNull(token);
            assertFalse(token.isBlank());
            assertEquals(EMAIL, email);
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
                            .param("active", "true")
                            .with(auth()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].name").value(NEW_NAME))
                    .andExpect(jsonPath("$.content[0].email").value(NEW_EMAIL));

        }
    }

    @Nested
    class UpdateTests{

        private UserUpdateRequest userUpdateRequest;

        private void buildUserUpdateRequest(){
            userUpdateRequest = UserUpdateRequest.builder()
                    .name(NEW_NAME)
                    .email(NEW_EMAIL)
                    .role(INSTRUCTOR)
                    .build();
        }

        @Test
        void shouldUpdateUserSuccessfully() throws Exception {
            setAdminToken();
            buildUser(DUMMY_NAME, DUMMY_EMAIL);
            buildUserUpdateRequest();

            mockMvc.perform(put(String.format(ID_ENDPOINT, dummyUser.getId()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(userUpdateRequest))
                            .with(auth()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value(userUpdateRequest.name()))
                    .andExpect(jsonPath("$.email").value(userUpdateRequest.email()))
                    .andExpect(jsonPath("$.role").value(userUpdateRequest.role().toString()));

        }

        @Test
        void shouldUpdateOwnProfileSuccessfully_andShouldNotUpdateRole_whenNonAdmin() throws Exception {
            buildUserUpdateRequest();
            setStudentToken();
            mockMvc.perform(put(MY_ENDPOINT)
                            .with(auth())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(userUpdateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value(userUpdateRequest.name()))
                    .andExpect(jsonPath("$.email").value(userUpdateRequest.email()))
                    .andExpect(jsonPath("$.role").value(testUser.getRole().toString()));
        }
    }

    @Nested
    class PatchTests{
        private UserPatchRequest userPatchRequest;

        private void buildUserPatchRequest(){
            userPatchRequest = UserPatchRequest.builder()
                    .name(NEW_NAME)
                    .role(INSTRUCTOR)
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
                            .content(toJson(userPatchRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value(userPatchRequest.name()))
                    .andExpect(jsonPath("$.email").value(dummyUser.getEmail()))
                    .andExpect(jsonPath("$.role").value(userPatchRequest.role().toString()));
        }

        @Test
        void shouldThrowBadRequest_whenPatchRequestEmpty() throws Exception {
            setAdminToken();
            buildUser(DUMMY_NAME, DUMMY_EMAIL);
            userPatchRequest = UserPatchRequest.builder()
                    .name("  ")
                    .build();

            mockMvc.perform(patch(String.format(ID_ENDPOINT, dummyUser.getId()))
                            .with(auth())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(userPatchRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldPatchOwnProfileSuccessfully_andShouldNotPatchRole_whenNonAdmin() throws Exception {
            buildUserPatchRequest();
            setInstructorToken();
            mockMvc.perform(patch(MY_ENDPOINT)
                            .with(auth())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(userPatchRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value(userPatchRequest.name()))
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

}
