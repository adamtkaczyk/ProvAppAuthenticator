package com.ita.provapp.server.authentication;

import com.ita.provapp.server.provappcommon.exceptions.AuthTokenIncorrectException;
import com.ita.provapp.server.provappcommon.exceptions.EntityExistsException;
import com.ita.provapp.server.provappcommon.exceptions.EntityNotFoundException;
import com.ita.provapp.server.provappcommon.exceptions.PasswordIncorrectException;
import com.ita.provapp.server.provappcommon.json.DBUser;
import com.ita.provapp.server.provappcommon.json.NewUser;
import com.ita.provapp.server.provappcommon.json.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Random;

import java.util.ArrayList;

@Component("accountServiceTemporary")
public class AccountsServiceTemporary extends AccountsService {
    private ArrayList<DBUser> users = new ArrayList<>();
    private static Logger logger = LoggerFactory.getLogger(AuthenticationController.class);
    private Random rand = new Random();

    @Override
    public Integer addUser(NewUser user) throws EntityExistsException {
        Integer userId = rand.nextInt(Integer.MAX_VALUE);
        if(userExists(user.getUsername())) {
            logger.warn("Cannot add new user. User: '" + user.getUsername() + "' already exists");
            throw new EntityExistsException("Cannot add new user. User: '" + user.getUsername() + "' already exists");
        } else {
            users.add(new DBUser(
                    new User(userId,user.getUsername(),user.getName(),user.getSurname(),user.getEmail()),
                    generatePasswordHash(user.getPassword())));
        }

        return userId;
    }

    @Override
    protected User getUserByPassword(String username, String password) throws EntityNotFoundException, PasswordIncorrectException {
        DBUser user = findUser(username);
        String passwordHash = generatePasswordHash(password);

        if (passwordHash.equals(user.getPasswordHash())) {
            return user.getUser();
        } else {
            logger.warn("Incorrect password for user: " + user.getUser().getUsername());
            throw new PasswordIncorrectException();
        }
    }

    @Override
    public User getUserByToken(String username, String token) throws EntityNotFoundException, AuthTokenIncorrectException {
        if(token.isEmpty())
            throw new AuthTokenIncorrectException();
        return findUser(username).getUser();
    }

    @Override
    protected User getUserByToken(Integer userId, String token) throws EntityNotFoundException, AuthTokenIncorrectException {
        if(token.isEmpty())
            throw new AuthTokenIncorrectException();
        return findUser(userId).getUser();
    }

    @Override
    protected void saveToken(String token) {

    }

    public boolean userExists(String username) {
        return users.stream().anyMatch(user -> user.getUser().getUsername().equals(username));
    }

    public boolean userExists(Integer userID) {
        return users.stream().anyMatch(user -> user.getUser().getUserID().equals(userID));
    }

    public DBUser getUser(String username) {
        return users.stream().filter(user -> user.getUser().getUsername().equals(username)).findFirst().get();
    }

    public DBUser getUser(Integer userID) {
        return users.stream().filter(user -> user.getUser().getUserID().equals(userID)).findFirst().get();
    }

    private DBUser findUser(String username) throws EntityNotFoundException {
        if(userExists(username)) {
            logger.debug("Found user: " + username);
            return getUser(username);
        } else {
            logger.warn("User: " + username + " not exists.");
            throw new EntityNotFoundException("User: " + username + " not exists.");
        }
    }

    private DBUser findUser(Integer userID) throws EntityNotFoundException {
        if(userExists(userID)) {
            logger.debug("Found user: " + userID);
            return getUser(userID);
        } else {
            logger.warn("User: " + userID + " not exists.");
            throw new EntityNotFoundException("User: " + userID + " not exists.");
        }
    }
}
