package com.dental.model;

import org.json.JSONObject;

/** DTO - a treatment type offered by the surgery and its base cost. */
public class Treatment {

    private int id;
    private String treatmentType;
    private double baseCost;
    private int durationMin = 30;
    private boolean active = true;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTreatmentType() { return treatmentType; }
    public void setTreatmentType(String treatmentType) { this.treatmentType = treatmentType; }

    public double getBaseCost() { return baseCost; }
    public void setBaseCost(double baseCost) { this.baseCost = baseCost; }

    public int getDurationMin() { return durationMin; }
    public void setDurationMin(int durationMin) { this.durationMin = durationMin; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public JSONObject toJson() {
        return new JSONObject()
                .put("id", id)
                .put("treatmentType", treatmentType)
                .put("baseCost", baseCost)
                .put("durationMin", durationMin)
                .put("active", active);
    }
}
