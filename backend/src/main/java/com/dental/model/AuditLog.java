package com.dental.model;

import org.json.JSONObject;

/** DTO - one "who did what" trail entry. */
public class AuditLog {

    private int id;
    private String username;
    private String action;     // CREATE | UPDATE | CANCEL | LOGIN | BILL ...
    private String entity;     // APPOINTMENT | BILL | DENTIST ...
    private String entityRef;
    private String details;
    private String createdAt;

    public AuditLog() { }

    public AuditLog(String username, String action, String entity, String entityRef, String details) {
        this.username = username;
        this.action = action;
        this.entity = entity;
        this.entityRef = entityRef;
        this.details = details;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getEntity() { return entity; }
    public void setEntity(String entity) { this.entity = entity; }

    public String getEntityRef() { return entityRef; }
    public void setEntityRef(String entityRef) { this.entityRef = entityRef; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public JSONObject toJson() {
        return new JSONObject()
                .put("id", id)
                .put("username", username == null ? "" : username)
                .put("action", action)
                .put("entity", entity)
                .put("entityRef", entityRef == null ? "" : entityRef)
                .put("details", details == null ? "" : details)
                .put("createdAt", createdAt == null ? JSONObject.NULL : createdAt);
    }
}
