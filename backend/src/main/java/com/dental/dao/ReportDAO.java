package com.dental.dao;

import com.dental.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only DAO holding the aggregate queries behind the dashboard and the
 * report screens. Rows come back as ordered maps so the service tier stays
 * independent of JDBC types.
 */
public class ReportDAO {

    /** Head-line figures for the dashboard cards. */
    public Map<String, Object> summary() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("appointmentsToday", scalar(
                "SELECT COUNT(*) FROM appointments WHERE appt_date = CURDATE() AND status <> 'CANCELLED'"));
        m.put("appointmentsTotal", scalar("SELECT COUNT(*) FROM appointments"));
        m.put("pending", scalar("SELECT COUNT(*) FROM appointments WHERE status = 'PENDING'"));
        m.put("confirmed", scalar("SELECT COUNT(*) FROM appointments WHERE status = 'CONFIRMED'"));
        m.put("completed", scalar("SELECT COUNT(*) FROM appointments WHERE status = 'COMPLETED'"));
        m.put("cancelled", scalar("SELECT COUNT(*) FROM appointments WHERE status = 'CANCELLED'"));
        m.put("patients", scalar("SELECT COUNT(*) FROM patients"));
        m.put("dentists", scalar("SELECT COUNT(*) FROM dentists WHERE active = 1"));
        m.put("revenueToday", scalar("SELECT COALESCE(SUM(total),0) FROM bills WHERE DATE(issued_at) = CURDATE()"));
        m.put("revenueMonth", scalar(
                "SELECT COALESCE(SUM(total),0) FROM bills "
                + "WHERE YEAR(issued_at) = YEAR(CURDATE()) AND MONTH(issued_at) = MONTH(CURDATE())"));
        m.put("revenueTotal", scalar("SELECT COALESCE(SUM(total),0) FROM bills"));
        m.put("unbilledCompleted", scalar(
                "SELECT COUNT(*) FROM appointments a WHERE a.status = 'COMPLETED' "
                + "AND NOT EXISTS (SELECT 1 FROM bills b WHERE b.appointment_id = a.id)"));
        return m;
    }

    /** Appointment count per day for the last {@code days} days (oldest first). */
    public List<Map<String, Object>> appointmentsPerDay(int days) {
        String sql = """
                SELECT DATE_FORMAT(appt_date, '%Y-%m-%d') AS label,
                       COUNT(*) AS value
                FROM appointments
                WHERE appt_date >= DATE_SUB(CURDATE(), INTERVAL ? DAY)
                  AND appt_date <= DATE_ADD(CURDATE(), INTERVAL 7 DAY)
                GROUP BY label
                ORDER BY label
                """;
        return query(sql, days);
    }

    /** Billed revenue per day for the last {@code days} days (oldest first). */
    public List<Map<String, Object>> revenuePerDay(int days) {
        String sql = """
                SELECT DATE_FORMAT(issued_at, '%Y-%m-%d') AS label,
                       ROUND(SUM(total), 2) AS value
                FROM bills
                WHERE issued_at >= DATE_SUB(CURDATE(), INTERVAL ? DAY)
                  GROUP BY label
                  ORDER BY label
                """;
        return query(sql, days);
    }

    /** Most requested treatments, with the revenue each one produced. */
    public List<Map<String, Object>> topTreatments(int limit) {
        String sql = """
                SELECT t.treatment_type AS label,
                       COUNT(a.id)      AS value,
                       COALESCE(SUM(b.total), 0) AS revenue
                FROM treatments t
                LEFT JOIN appointments a ON a.treatment_id = t.id AND a.status <> 'CANCELLED'
                LEFT JOIN bills b        ON b.appointment_id = a.id
                GROUP BY t.id, t.treatment_type
                HAVING value > 0
                ORDER BY value DESC, revenue DESC
                LIMIT ?
                """;
        return query(sql, limit);
    }

    /** How many appointments (and how much revenue) each dentist handled. */
    public List<Map<String, Object>> dentistWorkload() {
        String sql = """
                SELECT d.name AS label,
                       COUNT(a.id) AS value,
                       COALESCE(SUM(b.total), 0) AS revenue
                FROM dentists d
                LEFT JOIN appointments a ON a.dentist_id = d.id AND a.status <> 'CANCELLED'
                LEFT JOIN bills b        ON b.appointment_id = a.id
                GROUP BY d.id, d.name
                ORDER BY value DESC
                """;
        return query(sql);
    }

    /** Appointment counts grouped by status - drives the donut chart. */
    public List<Map<String, Object>> statusBreakdown() {
        return query("SELECT status AS label, COUNT(*) AS value FROM appointments GROUP BY status ORDER BY value DESC");
    }

    /** Every bill issued between two dates, for the revenue report. */
    public List<Map<String, Object>> revenueBetween(String from, String to) {
        String sql = """
                SELECT b.bill_no, b.total, b.discount, b.pricing_strategy, b.payment_method,
                       DATE_FORMAT(b.issued_at, '%Y-%m-%d %H:%i') AS issued_at,
                       a.appointment_no, p.name AS patient_name, t.treatment_type, d.name AS dentist_name
                FROM bills b
                JOIN appointments a ON a.id = b.appointment_id
                JOIN patients   p ON p.id = a.patient_id
                JOIN treatments t ON t.id = a.treatment_id
                JOIN dentists   d ON d.id = a.dentist_id
                WHERE DATE(b.issued_at) BETWEEN ? AND ?
                ORDER BY b.issued_at DESC
                """;
        return query(sql, from, to);
    }

    /** Patients ranked by number of visits. */
    public List<Map<String, Object>> topPatients(int limit) {
        String sql = """
                SELECT p.name AS label, COUNT(a.id) AS value,
                       COALESCE(SUM(b.total), 0) AS revenue,
                       p.contact_no
                FROM patients p
                JOIN appointments a ON a.patient_id = p.id AND a.status <> 'CANCELLED'
                LEFT JOIN bills b   ON b.appointment_id = a.id
                GROUP BY p.id, p.name, p.contact_no
                ORDER BY value DESC
                LIMIT ?
                """;
        return query(sql, limit);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private double scalar(String sql) {
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getDouble(1) : 0d;
        } catch (SQLException e) {
            throw new DataAccessException("Report query failed: " + e.getMessage(), e);
        }
    }

    private List<Map<String, Object>> query(String sql, Object... args) {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Integer n) {
                    ps.setInt(i + 1, n);
                } else {
                    ps.setString(i + 1, String.valueOf(args[i]));
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                int cols = md.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= cols; i++) {
                        Object v = rs.getObject(i);
                        if (v instanceof java.math.BigDecimal bd) {
                            v = bd.doubleValue();
                        } else if (v instanceof java.sql.Date || v instanceof java.sql.Timestamp) {
                            v = String.valueOf(v);
                        }
                        row.put(md.getColumnLabel(i), v);
                    }
                    rows.add(row);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Report query failed: " + e.getMessage(), e);
        }
        return rows;
    }
}
