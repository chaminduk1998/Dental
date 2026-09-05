package com.dental.model;

import org.json.JSONObject;

/** DTO - a staff account that can log into the system. */
public class User {

    private int id;
    private String username;
    private String password;   // SHA-256 digest, never sent to the browser
    private String role;       // ADMIN | STAFF
    private String fullName;
    private boolean active = true;
    private String createdAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public boolean isAdmin() { return "ADMIN".equalsIgnoreCase(role); }

    /** Password digest is deliberately excluded. */
    public JSONObject toJson() {
        return new JSONObject()
                .put("id", id)
                .put("username", username)
                .put("role", role)
                .put("fullName", fullName)
                .put("active", active)
                .put("createdAt", createdAt == null ? JSONObject.NULL : createdAt);
    }
}
