package com.ita.provapp.server.authentication;

import com.ita.provapp.server.DateDeserializer;
import com.ita.provapp.server.provappcommon.exceptions.AuthTokenIncorrectException;
import com.ita.provapp.server.provappcommon.exceptions.EntityExistsException;
import com.ita.provapp.server.provappcommon.exceptions.EntityNotFoundException;
import com.ita.provapp.server.provappcommon.exceptions.PasswordIncorrectException;
import com.ita.provapp.server.provappcommon.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = AuthenticationController.class)
public class AuthenticationControllerTest {
    Credential credential = new Credential("adam123","password");
    User user = new User(1, "adam123", "Adam","Kowalski","adamkow@gmail.com");
    Gson gson = new GsonBuilder().registerTypeAdapter(Date.class, new DateDeserializer()).create();

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountsService accountsService;

    @Test
    public void testAuthentication() throws EntityNotFoundException, PasswordIncorrectException, Exception {
        LoginUser loginUser = new LoginUser("1234", user);

        when(this.accountsService.authenticate(credential)).thenReturn(loginUser);

        mockMvc.perform(
                post("/users/authtokens")
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .accept(MediaType.APPLICATION_JSON_VALUE)
                    .content(gson.toJson(credential)))
               .andExpect(content().json(gson.toJson(loginUser)))
               .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
               .andExpect(status().isCreated()).andReturn();
    }

    @Test
    public void testAuthenticationIncorrectPassword() throws EntityNotFoundException, PasswordIncorrectException, Exception {
        when(this.accountsService.authenticate(credential)).thenThrow(new PasswordIncorrectException());
        ErrorMessage errorMessage = new ErrorMessage("Incorrect username or password", 404);

        mockMvc.perform(
                post("/users/authtokens")
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .accept(MediaType.APPLICATION_JSON_VALUE)
                    .content(gson.toJson(credential)))
                .andExpect(jsonPath("$.errors", org.hamcrest.Matchers.is(errorMessage.getErrors())))
                .andExpect(jsonPath("$.status", org.hamcrest.Matchers.is(errorMessage.getStatus())))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isNotFound()).andReturn();
    }

    @Test
    public void testAuthenticationIncorrectUsername() throws EntityNotFoundException, PasswordIncorrectException, Exception {
        when(this.accountsService.authenticate(credential)).thenThrow(new EntityNotFoundException("User not exist"));
        ErrorMessage errorMessage = new ErrorMessage("Incorrect username or password", 404);

        mockMvc.perform(
                post("/users/authtokens")
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .accept(MediaType.APPLICATION_JSON_VALUE)
                    .content(gson.toJson(credential)))
                .andExpect(jsonPath("$.errors", org.hamcrest.Matchers.is(errorMessage.getErrors())))
                .andExpect(jsonPath("$.status", org.hamcrest.Matchers.is(errorMessage.getStatus())))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isNotFound()).andReturn();
    }

    @Test
    public void testAuthenticationEmptyUsername() throws Exception {
        ErrorMessage errorMessage = new ErrorMessage("Username can't be empty", 400);

        mockMvc.perform(
                post("/users/authtokens")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .content("{\"username\": \"\", \"password\": \"adam321\"}"))
                .andExpect(jsonPath("$.errors", org.hamcrest.Matchers.is(errorMessage.getErrors())))
                .andExpect(jsonPath("$.status", org.hamcrest.Matchers.is(errorMessage.getStatus())))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isBadRequest()).andReturn();
    }

    @Test
    public void testAuthenticationEmptyPassword() throws Exception {
        ErrorMessage errorMessage = new ErrorMessage("Password can't be empty", 400);

        mockMvc.perform(
                post("/users/authtokens")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .content("{\"user\": \"adam321\", \"password\": \"\"}"))
                .andExpect(jsonPath("$.errors", org.hamcrest.Matchers.is(errorMessage.getErrors())))
                .andExpect(jsonPath("$.status", org.hamcrest.Matchers.is(errorMessage.getStatus())))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isBadRequest()).andReturn();
    }

    @Test
    public void testAddUser() throws EntityExistsException, Exception {
        NewUser newUser = new NewUser("adam123", "Adam","Kowalski","adamkow@gmail.com", "pass123");
        when(this.accountsService.addUser(newUser)).thenReturn(1234);

        mockMvc.perform(
                post("/users")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .content(gson.toJson(newUser)))
                .andExpect(header().string("Location", "/users/" + newUser.getUsername()))
                .andExpect(status().isCreated()).andReturn();
    }

    @Test
    public void testAddUserWhichAlreadyExists() throws EntityExistsException, Exception {
        NewUser newUser = new NewUser("adam123", "Adam","Kowalski","adamkow@gmail.com", "pass123");
        ErrorMessage errorMessage = new ErrorMessage("Cannot add new user. User: \"adam123\" already exists", 409);
        when(this.accountsService.addUser(newUser)).thenThrow(new EntityExistsException("Cannot add new user. User: \"adam123\" already exists"));

        mockMvc.perform(
                post("/users")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .content(gson.toJson(newUser)))
                .andExpect(jsonPath("$.errors", org.hamcrest.Matchers.is(errorMessage.getErrors())))
                .andExpect(jsonPath("$.status", org.hamcrest.Matchers.is(errorMessage.getStatus())))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isConflict()).andReturn();
    }

    @Test
    public void testAddUserIncorrectUsername() throws Exception {
        NewUser newUser = new NewUser("", "Adam","Kowalski","adamkow@gmail.com", "pass123");
        ErrorMessage errorMessage = new ErrorMessage("Username can't be empty", 400);

        mockMvc.perform(
                post("/users")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .content(gson.toJson(newUser)))
                .andExpect(jsonPath("$.errors", org.hamcrest.Matchers.is(errorMessage.getErrors())))
                .andExpect(jsonPath("$.status", org.hamcrest.Matchers.is(errorMessage.getStatus())))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isBadRequest()).andReturn();
    }

    @Test
    public void testAddUserIncorrectName() throws Exception {
        NewUser newUser = new NewUser("adam", "","Kowalski","adamkow@gmail.com", "pass123");
        ErrorMessage errorMessage = new ErrorMessage("Name can't be empty", 400);

        mockMvc.perform(
                post("/users")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .content(gson.toJson(newUser)))
                .andExpect(jsonPath("$.errors", org.hamcrest.Matchers.is(errorMessage.getErrors())))
                .andExpect(jsonPath("$.status", org.hamcrest.Matchers.is(errorMessage.getStatus())))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isBadRequest()).andReturn();
    }

    @Test
    public void testAddUserIncorrectSurname() throws Exception {
        NewUser newUser = new NewUser("adam", "Adam","","adamkow@gmail.com", "pass123");
        ErrorMessage errorMessage = new ErrorMessage("Surname can't be empty", 400);

        mockMvc.perform(
                post("/users")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .content(gson.toJson(newUser)))
                .andExpect(jsonPath("$.errors", org.hamcrest.Matchers.is(errorMessage.getErrors())))
                .andExpect(jsonPath("$.status", org.hamcrest.Matchers.is(errorMessage.getStatus())))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isBadRequest()).andReturn();
    }

    @Test
    public void testAddUserIncorrectEmail() throws Exception {
        NewUser newUser = new NewUser("adam", "Adam","Kowalski","adamkowgmail.com", "pass123");
        ErrorMessage errorMessage = new ErrorMessage("Incorrect email address", 400);

        mockMvc.perform(
                post("/users")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .content(gson.toJson(newUser)))
                .andExpect(jsonPath("$.errors", org.hamcrest.Matchers.is(errorMessage.getErrors())))
                .andExpect(jsonPath("$.status", org.hamcrest.Matchers.is(errorMessage.getStatus())))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isBadRequest()).andReturn();
    }

    @Test
    public void testLogout() throws Exception {
        String authtoken = "1234asf";
        when(accountsService.logout(authtoken)).thenReturn(true);
        mockMvc.perform(
                delete("/users/authtokens")
                    .header("Authorization", authtoken))
                .andExpect(status().isOk()).andReturn();
    }

    @Test
    public void testLogoutNoContent() throws Exception {
        String authtoken = "1234asf";
        when(accountsService.logout(authtoken)).thenReturn(false);
        mockMvc.perform(
                delete("/users/authtokens")
                        .header("Authorization", authtoken))
                .andExpect(status().isNoContent()).andReturn();
    }

    @Test
    public void testGetUser() throws EntityNotFoundException, AuthTokenIncorrectException, Exception {
        String username = "user123";
        String authtoken = "123qwe";

        when(accountsService.getUserByToken(username,authtoken)).thenReturn(user);

        mockMvc.perform(
                get(String.format("/users/%s",username))
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .header("Authorization", authtoken))
                .andExpect(content().json(gson.toJson(user)))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk()).andReturn();
    }

    @Test
    public void testGetUserMissingAuthToken() throws EntityNotFoundException, AuthTokenIncorrectException, Exception {
        String username = "user123";
        String authtoken = "123qwe";
        ErrorMessage errorMessage = new ErrorMessage("Incorrect email address", 401);

        when(accountsService.getUserByToken(username,authtoken)).thenReturn(user);

        mockMvc.perform(
                get(String.format("/users/%s",username))
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.errors", org.hamcrest.Matchers.is(errorMessage.getErrors())))
                .andExpect(jsonPath("$.status", org.hamcrest.Matchers.is(errorMessage.getStatus())))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isUnauthorized()).andReturn();
    }

    @Test
    public void testGetUserIncorrectAuthToken() throws EntityNotFoundException, AuthTokenIncorrectException, Exception {
        String username = "user123";
        String authtoken = "123qwe";
        ErrorMessage errorMessage = new ErrorMessage("Unauthorized access. Authorization token is incorrect.", 401);

        when(accountsService.getUserByToken(username,authtoken)).thenThrow(new AuthTokenIncorrectException());

        mockMvc.perform(
                get(String.format("/users/%s",username))
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .header("Authorization", authtoken))
                .andExpect(jsonPath("$.errors", org.hamcrest.Matchers.is(errorMessage.getErrors())))
                .andExpect(jsonPath("$.status", org.hamcrest.Matchers.is(errorMessage.getStatus())))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isUnauthorized()).andReturn();
    }

    @Test
    public void testGetNoExistsUser() throws EntityNotFoundException, AuthTokenIncorrectException, Exception {
        String username = "user123";
        String authtoken = "123qwe";
        ErrorMessage errorMessage = new ErrorMessage("User: " + username + " not exists.", 404);

        when(accountsService.getUserByToken(username,authtoken)).thenThrow(new EntityNotFoundException("User: " + username + " not exists."));

        mockMvc.perform(
                get(String.format("/users/%s",username))
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .header("Authorization", authtoken))
                .andExpect(jsonPath("$.errors", org.hamcrest.Matchers.is(errorMessage.getErrors())))
                .andExpect(jsonPath("$.status", org.hamcrest.Matchers.is(errorMessage.getStatus())))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isNotFound()).andReturn();
    }
}

