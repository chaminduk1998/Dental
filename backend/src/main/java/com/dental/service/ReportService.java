package com.dental.service;

import com.dental.dao.DAOFactory;
import com.dental.dao.ReportDAO;
import com.dental.util.ValidationException;
import com.dental.util.Validator;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Aggregation logic behind the dashboard and the report screens. */
public class ReportService {

    private final ReportDAO dao = DAOFactory.reports();

    public Map<String, Object> dashboardSummary() {
        return dao.summary();
    }

    public List<Map<String, Object>> appointmentsTrend(int days) {
        return dao.appointmentsPerDay(clampDays(days));
    }

    public List<Map<String, Object>> revenueTrend(int days) {
        return dao.revenuePerDay(clampDays(days));
    }

    public List<Map<String, Object>> topTreatments(int limit) {
        return dao.topTreatments(limit <= 0 ? 10 : limit);
    }

    public List<Map<String, Object>> dentistWorkload() {
        return dao.dentistWorkload();
    }

    public List<Map<String, Object>> statusBreakdown() {
        return dao.statusBreakdown();
    }

    public List<Map<String, Object>> topPatients(int limit) {
        return dao.topPatients(limit <= 0 ? 10 : limit);
    }

    /** Revenue report for an arbitrary date range (defaults to the current month). */
    public List<Map<String, Object>> revenueReport(String from, String to) {
        LocalDate today = LocalDate.now();
        LocalDate f = (from == null || from.isBlank()) ? today.withDayOfMonth(1) : Validator.date(from, "From date");
        LocalDate t = (to == null || to.isBlank()) ? today : Validator.date(to, "To date");
        if (f.isAfter(t)) {
            throw new ValidationException("'From' date must not be after 'To' date");
        }
        return dao.revenueBetween(f.toString(), t.toString());
    }

    private int clampDays(int days) {
        if (days <= 0) {
            return 14;
        }
        return Math.min(days, 180);
    }
}
