package com.dental.service;

import com.dental.dao.AuditLogDAO;
import com.dental.dao.DAOFactory;
import com.dental.dao.UserDAO;
import com.dental.model.AuditLog;
import com.dental.model.User;
import com.dental.util.PasswordUtil;
import com.dental.util.ValidationException;
import com.dental.util.Validator;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Business logic for login / logout and session look-up.
 *
 * <p>Sessions are an in-memory token store (no external framework available),
 * keyed by an opaque random token handed back as a cookie. Good enough for a
 * single-instance coursework deployment; would move to a shared store for a
 * clustered one.</p>
 */
public class AuthService {

    /** Session idle timeout. */
    private static final long SESSION_TTL_MS = 2 * 60 * 60 * 1000L; // 2 hours

    private final UserDAO userDAO = DAOFactory.users();
    private final AuditLogDAO auditDAO = DAOFactory.auditLogs();
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public static class Session {
        public final String token;
        public final User user;
        public volatile long lastSeen;

        Session(String token, User user) {
            this.token = token;
            this.user = user;
            this.lastSeen = Instant.now().toEpochMilli();
        }
    }

    public Session login(String username, String password) {
        username = Validator.required(username, "Username");
        password = Validator.required(password, "Password");

        Optional<User> found = userDAO.findByUsername(username);
        if (found.isEmpty() || !found.get().isActive() || !PasswordUtil.matches(password, found.get().getPassword())) {
            throw new ValidationException("Invalid username or password");
        }
        User user = found.get();
        String token = newToken();
        Session session = new Session(token, user);
        sessions.put(token, session);
        auditDAO.insert(new AuditLog(user.getUsername(), "LOGIN", "USER", user.getUsername(), "Signed in"));
        return session;
    }

    public void logout(String token) {
        Session s = sessions.remove(token);
        if (s != null) {
            auditDAO.insert(new AuditLog(s.user.getUsername(), "LOGOUT", "USER", s.user.getUsername(), "Signed out"));
        }
    }

    /** Returns the session for a valid, unexpired token and refreshes its idle timer. */
    public Optional<Session> resolve(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        Session s = sessions.get(token);
        if (s == null) {
            return Optional.empty();
        }
        long now = Instant.now().toEpochMilli();
        if (now - s.lastSeen > SESSION_TTL_MS) {
            sessions.remove(token);
            return Optional.empty();
        }
        s.lastSeen = now;
        return Optional.of(s);
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
