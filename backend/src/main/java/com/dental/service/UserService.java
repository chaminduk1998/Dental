package com.dental.service;

import com.dental.dao.DAOFactory;
import com.dental.dao.UserDAO;
import com.dental.model.User;
import com.dental.util.PasswordUtil;
import com.dental.util.ValidationException;
import com.dental.util.Validator;

import java.util.List;
import java.util.Optional;

/** Business logic for the admin panel's "manage staff users" screen. */
public class UserService {

    private final UserDAO dao = DAOFactory.users();

    public List<User> list() {
        return dao.findAll();
    }

    public User get(int id) {
        return dao.findById(id).orElseThrow(() -> new ValidationException("User not found"));
    }

    public User create(User u, String plainPassword) {
        u.setUsername(Validator.required(u.getUsername(), "Username"));
        Validator.required(plainPassword, "Password");
        if (plainPassword.length() < 6) {
            throw new ValidationException("Password must be at least 6 characters");
        }
        u.setFullName(Validator.required(u.getFullName(), "Full name"));
        u.setRole(normaliseRole(u.getRole()));

        Optional<User> clash = dao.findByUsername(u.getUsername());
        if (clash.isPresent()) {
            throw new ValidationException("Username '" + u.getUsername() + "' is already taken");
        }
        u.setPassword(PasswordUtil.hash(plainPassword));
        u.setId(dao.insert(u));
        return u;
    }

    public User update(int id, User u, String newPlainPassword) {
        get(id);
        u.setId(id);
        u.setUsername(Validator.required(u.getUsername(), "Username"));
        u.setFullName(Validator.required(u.getFullName(), "Full name"));
        u.setRole(normaliseRole(u.getRole()));
        if (newPlainPassword != null && !newPlainPassword.isBlank()) {
            if (newPlainPassword.length() < 6) {
                throw new ValidationException("Password must be at least 6 characters");
            }
            u.setPassword(PasswordUtil.hash(newPlainPassword));
        }
        dao.update(u);
        return u;
    }

    public void delete(int id, String requestingUsername) {
        User target = get(id);
        if (target.getUsername().equalsIgnoreCase(requestingUsername)) {
            throw new ValidationException("You cannot delete your own account while logged in");
        }
        dao.delete(id);
    }

    private String normaliseRole(String role) {
        String r = (role == null || role.isBlank()) ? "STAFF" : role.trim().toUpperCase();
        if (!r.equals("ADMIN") && !r.equals("STAFF")) {
            throw new ValidationException("Role must be ADMIN or STAFF");
        }
        return r;
    }
}
