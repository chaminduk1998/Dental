package com.dental.model;

import org.json.JSONObject;

/**
 * DTO - one booked appointment.
 *
 * <p>The read-only <code>*Name</code> / cost fields are filled in by the DAO's
 * JOIN query so the presentation tier can render a row without extra calls.</p>
 */
public class Appointment {

    public static final String PENDING = "PENDING";
    public static final String CONFIRMED = "CONFIRMED";
    public static final String COMPLETED = "COMPLETED";
    public static final String CANCELLED = "CANCELLED";

    private int id;
    private String appointmentNo;
    private int patientId;
    private int dentistId;
    private int treatmentId;
    private String apptDate;      // yyyy-MM-dd
    private String apptTime;      // HH:mm
    private String status = PENDING;
    private String notes;
    private String createdBy;
    private String createdAt;

    // joined, display-only
    private String patientName;
    private String patientAddress;
    private String patientContact;
    private String patientEmail;
    private String dentistName;
    private String treatmentType;
    private double treatmentCost;
    private double consultationFee;
    private boolean billed;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getAppointmentNo() { return appointmentNo; }
    public void setAppointmentNo(String appointmentNo) { this.appointmentNo = appointmentNo; }

    public int getPatientId() { return patientId; }
    public void setPatientId(int patientId) { this.patientId = patientId; }

    public int getDentistId() { return dentistId; }
    public void setDentistId(int dentistId) { this.dentistId = dentistId; }

    public int getTreatmentId() { return treatmentId; }
    public void setTreatmentId(int treatmentId) { this.treatmentId = treatmentId; }

    public String getApptDate() { return apptDate; }
    public void setApptDate(String apptDate) { this.apptDate = apptDate; }

    public String getApptTime() { return apptTime; }
    public void setApptTime(String apptTime) { this.apptTime = apptTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getPatientAddress() { return patientAddress; }
    public void setPatientAddress(String patientAddress) { this.patientAddress = patientAddress; }

    public String getPatientContact() { return patientContact; }
    public void setPatientContact(String patientContact) { this.patientContact = patientContact; }

    public String getPatientEmail() { return patientEmail; }
    public void setPatientEmail(String patientEmail) { this.patientEmail = patientEmail; }

    public String getDentistName() { return dentistName; }
    public void setDentistName(String dentistName) { this.dentistName = dentistName; }

    public String getTreatmentType() { return treatmentType; }
    public void setTreatmentType(String treatmentType) { this.treatmentType = treatmentType; }

    public double getTreatmentCost() { return treatmentCost; }
    public void setTreatmentCost(double treatmentCost) { this.treatmentCost = treatmentCost; }

    public double getConsultationFee() { return consultationFee; }
    public void setConsultationFee(double consultationFee) { this.consultationFee = consultationFee; }

    public boolean isBilled() { return billed; }
    public void setBilled(boolean billed) { this.billed = billed; }

    public JSONObject toJson() {
        return new JSONObject()
                .put("id", id)
                .put("appointmentNo", appointmentNo)
                .put("patientId", patientId)
                .put("dentistId", dentistId)
                .put("treatmentId", treatmentId)
                .put("date", apptDate)
                .put("time", apptTime)
                .put("status", status)
                .put("notes", notes == null ? "" : notes)
                .put("createdBy", createdBy == null ? "" : createdBy)
                .put("createdAt", createdAt == null ? JSONObject.NULL : createdAt)
                .put("patientName", patientName)
                .put("patientAddress", patientAddress == null ? "" : patientAddress)
                .put("patientContact", patientContact == null ? "" : patientContact)
                .put("patientEmail", patientEmail == null ? "" : patientEmail)
                .put("dentistName", dentistName)
                .put("treatmentType", treatmentType)
                .put("treatmentCost", treatmentCost)
                .put("consultationFee", consultationFee)
                .put("estimatedTotal", treatmentCost + consultationFee)
                .put("billed", billed);
    }
}
