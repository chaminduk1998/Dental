package com.dental.model;

import org.json.JSONObject;

/** DTO - a dentist and the consultation fee they charge. */
public class Dentist {

    private int id;
    private String name;
    private String specialization;
    private double consultationFee;
    private boolean active = true;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public double getConsultationFee() { return consultationFee; }
    public void setConsultationFee(double consultationFee) { this.consultationFee = consultationFee; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public JSONObject toJson() {
        return new JSONObject()
                .put("id", id)
                .put("name", name)
                .put("specialization", specialization == null ? "" : specialization)
                .put("consultationFee", consultationFee)
                .put("active", active);
    }
}
