package com.dental.model;

import org.json.JSONObject;

/** DTO - a confirmation / reminder message raised by an observer. */
public class Notification {

    private int id;
    private String recipient;
    private String channel = "EMAIL";
    private String subject;
    private String message;
    private String status = "QUEUED";
    private String createdAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public JSONObject toJson() {
        return new JSONObject()
                .put("id", id)
                .put("recipient", recipient == null ? "" : recipient)
                .put("channel", channel)
                .put("subject", subject == null ? "" : subject)
                .put("message", message == null ? "" : message)
                .put("status", status)
                .put("createdAt", createdAt == null ? JSONObject.NULL : createdAt);
    }
}
