package hometask.ticket_client.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {
    private static final String USERS_SESSION_KEY = "users";

    public void addUser(HttpSession session, UUID user) {
        List<UUID> users = getSessionUsers(session);
        if (!users.contains(user)) {
            users.add(user);
        }
        session.setAttribute(USERS_SESSION_KEY, users);
    }

    public List<UUID> getUsers(HttpSession session) {
        return getSessionUsers(session);
    }

    private List<UUID> getSessionUsers(HttpSession session) {
        List<UUID> users = (List<UUID>) session.getAttribute(USERS_SESSION_KEY);
        if (users == null) {
            users = new ArrayList<>();
        }

        return users;
    }
}
