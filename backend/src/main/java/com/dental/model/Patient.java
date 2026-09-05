package com.dental.model;

import org.json.JSONObject;

/** DTO - a patient of the surgery. */
public class Patient {

    private int id;
    private String name;
    private String address;
    private String contactNo;
    private String email;
    private String createdAt;
    private int visitCount;      // populated by report queries

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getContactNo() { return contactNo; }
    public void setContactNo(String contactNo) { this.contactNo = contactNo; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public int getVisitCount() { return visitCount; }
    public void setVisitCount(int visitCount) { this.visitCount = visitCount; }

    public JSONObject toJson() {
        return new JSONObject()
                .put("id", id)
                .put("name", name)
                .put("address", address == null ? "" : address)
                .put("contactNo", contactNo == null ? "" : contactNo)
                .put("email", email == null ? "" : email)
                .put("visitCount", visitCount)
                .put("createdAt", createdAt == null ? JSONObject.NULL : createdAt);
    }
}
