package com.dental.model;

import org.json.JSONObject;

/** DTO - the receipt produced for a completed appointment. */
public class Bill {

    private int id;
    private String billNo;
    private int appointmentId;
    private double treatmentCost;
    private double consultationFee;
    private double discount;
    private double tax;
    private double total;
    private String pricingStrategy = "STANDARD";
    private String paymentMethod = "CASH";
    private String issuedBy;
    private String issuedAt;

    // joined, display-only
    private String appointmentNo;
    private String patientName;
    private String patientAddress;
    private String patientContact;
    private String dentistName;
    private String treatmentType;
    private String apptDate;
    private String apptTime;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getBillNo() { return billNo; }
    public void setBillNo(String billNo) { this.billNo = billNo; }

    public int getAppointmentId() { return appointmentId; }
    public void setAppointmentId(int appointmentId) { this.appointmentId = appointmentId; }

    public double getTreatmentCost() { return treatmentCost; }
    public void setTreatmentCost(double treatmentCost) { this.treatmentCost = treatmentCost; }

    public double getConsultationFee() { return consultationFee; }
    public void setConsultationFee(double consultationFee) { this.consultationFee = consultationFee; }

    public double getDiscount() { return discount; }
    public void setDiscount(double discount) { this.discount = discount; }

    public double getTax() { return tax; }
    public void setTax(double tax) { this.tax = tax; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public String getPricingStrategy() { return pricingStrategy; }
    public void setPricingStrategy(String pricingStrategy) { this.pricingStrategy = pricingStrategy; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getIssuedBy() { return issuedBy; }
    public void setIssuedBy(String issuedBy) { this.issuedBy = issuedBy; }

    public String getIssuedAt() { return issuedAt; }
    public void setIssuedAt(String issuedAt) { this.issuedAt = issuedAt; }

    public String getAppointmentNo() { return appointmentNo; }
    public void setAppointmentNo(String appointmentNo) { this.appointmentNo = appointmentNo; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getPatientAddress() { return patientAddress; }
    public void setPatientAddress(String patientAddress) { this.patientAddress = patientAddress; }

    public String getPatientContact() { return patientContact; }
    public void setPatientContact(String patientContact) { this.patientContact = patientContact; }

    public String getDentistName() { return dentistName; }
    public void setDentistName(String dentistName) { this.dentistName = dentistName; }

    public String getTreatmentType() { return treatmentType; }
    public void setTreatmentType(String treatmentType) { this.treatmentType = treatmentType; }

    public String getApptDate() { return apptDate; }
    public void setApptDate(String apptDate) { this.apptDate = apptDate; }

    public String getApptTime() { return apptTime; }
    public void setApptTime(String apptTime) { this.apptTime = apptTime; }

    public double getSubTotal() { return treatmentCost + consultationFee; }

    public JSONObject toJson() {
        return new JSONObject()
                .put("id", id)
                .put("billNo", billNo)
                .put("appointmentId", appointmentId)
                .put("appointmentNo", appointmentNo)
                .put("treatmentCost", treatmentCost)
                .put("consultationFee", consultationFee)
                .put("subTotal", getSubTotal())
                .put("discount", discount)
                .put("tax", tax)
                .put("total", total)
                .put("pricingStrategy", pricingStrategy)
                .put("paymentMethod", paymentMethod)
                .put("issuedBy", issuedBy == null ? "" : issuedBy)
                .put("issuedAt", issuedAt == null ? JSONObject.NULL : issuedAt)
                .put("patientName", patientName)
                .put("patientAddress", patientAddress == null ? "" : patientAddress)
                .put("patientContact", patientContact == null ? "" : patientContact)
                .put("dentistName", dentistName)
                .put("treatmentType", treatmentType)
                .put("date", apptDate)
                .put("time", apptTime);
    }
}
