package com.dental.service;

import com.dental.dao.BillDAO;
import com.dental.dao.DAOFactory;
import com.dental.model.Appointment;
import com.dental.model.Bill;
import com.dental.pricing.PricingStrategy;
import com.dental.pricing.PricingStrategyFactory;
import com.dental.util.ValidationException;
import com.dental.util.Validator;

import java.util.List;

/**
 * Business logic behind "Calculate and Print Bill". Delegates the discount
 * calculation to a {@link PricingStrategy} chosen by the front desk, keeping
 * this class free of pricing rules (Strategy pattern).
 */
public class BillingService {

    private final BillDAO billDAO = DAOFactory.bills();
    private final AppointmentService appointmentService = new AppointmentService();

    public List<PricingStrategy> pricingOptions() {
        return PricingStrategyFactory.all();
    }

    public Bill getByAppointmentNo(String appointmentNo) {
        Appointment a = appointmentService.getByNo(appointmentNo);
        return billDAO.findByAppointmentId(a.getId())
                .orElseThrow(() -> new ValidationException("No bill has been issued for " + appointmentNo + " yet"));
    }

    public Bill getByBillNo(String billNo) {
        return billDAO.findByBillNo(Validator.required(billNo, "Bill number"))
                .orElseThrow(() -> new ValidationException("Bill not found"));
    }

    public List<Bill> recent() {
        return billDAO.findAll();
    }

    /** Preview the total for a strategy without saving anything (used for live UI updates). */
    public Bill preview(int appointmentId, String pricingCode) {
        Appointment a = appointmentService.getById(appointmentId);
        return compute(a, pricingCode, "CASH", null);
    }

    /**
     * Calculates and permanently issues the bill for a completed appointment.
     * An appointment can only be billed once.
     */
    public Bill issue(int appointmentId, String pricingCode, String paymentMethod, String issuedBy) {
        Appointment a = appointmentService.getById(appointmentId);

        if (billDAO.findByAppointmentId(appointmentId).isPresent()) {
            throw new ValidationException("Appointment " + a.getAppointmentNo() + " has already been billed");
        }
        if (Appointment.CANCELLED.equals(a.getStatus())) {
            throw new ValidationException("A cancelled appointment cannot be billed");
        }

        // Billing marks the visit as completed if it was still pending/confirmed.
        if (!Appointment.COMPLETED.equals(a.getStatus())) {
            appointmentService.changeStatus(appointmentId, Appointment.COMPLETED);
            a = appointmentService.getById(appointmentId);
        }

        Bill bill = compute(a, pricingCode, paymentMethod, issuedBy);
        bill.setBillNo(billDAO.nextBillNo());
        bill.setId(billDAO.insert(bill));
        return bill;
    }

    private Bill compute(Appointment a, String pricingCode, String paymentMethod, String issuedBy) {
        PricingStrategy strategy = PricingStrategyFactory.of(pricingCode);
        double subTotal = a.getTreatmentCost() + a.getConsultationFee();
        double discount = Math.min(strategy.discountFor(subTotal), subTotal);
        double taxable = subTotal - discount;
        double taxRate = com.dental.config.AppConfig.get().getDouble("clinic.taxRate", 0.0);
        double tax = round(taxable * taxRate);
        double total = round(taxable + tax);

        Bill bill = new Bill();
        bill.setAppointmentId(a.getId());
        bill.setTreatmentCost(a.getTreatmentCost());
        bill.setConsultationFee(a.getConsultationFee());
        bill.setDiscount(discount);
        bill.setTax(tax);
        bill.setTotal(total);
        bill.setPricingStrategy(strategy.code());
        bill.setPaymentMethod(paymentMethod == null || paymentMethod.isBlank() ? "CASH" : paymentMethod.toUpperCase());
        bill.setIssuedBy(issuedBy);

        // display fields for the receipt, filled straight from the appointment
        bill.setAppointmentNo(a.getAppointmentNo());
        bill.setApptDate(a.getApptDate());
        bill.setApptTime(a.getApptTime());
        bill.setPatientName(a.getPatientName());
        bill.setPatientAddress(a.getPatientAddress());
        bill.setPatientContact(a.getPatientContact());
        bill.setDentistName(a.getDentistName());
        bill.setTreatmentType(a.getTreatmentType());
        return bill;
    }

    private double round(double v) {
        return Math.round(v * 100d) / 100d;
    }
}
