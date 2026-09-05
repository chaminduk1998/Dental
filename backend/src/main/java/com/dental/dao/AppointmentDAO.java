package com.dental.dao;

import com.dental.config.DatabaseConnection;
import com.dental.model.Appointment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** DAO for the {@code appointments} table (joins in patient / dentist / treatment). */
public class AppointmentDAO implements GenericDAO<Appointment> {

    private static final String BASE = """
            SELECT a.id, a.appointment_no, a.patient_id, a.dentist_id, a.treatment_id,
                   a.appt_date, a.appt_time, a.status, a.notes, a.created_at,
                   p.name AS patient_name, p.address AS patient_address,
                   p.contact_no AS patient_contact, p.email AS patient_email,
                   d.name AS dentist_name, d.consultation_fee,
                   t.treatment_type, t.base_cost,
                   u.username AS created_by,
                   EXISTS (SELECT 1 FROM bills b WHERE b.appointment_id = a.id) AS billed
            FROM appointments a
            JOIN patients   p ON p.id = a.patient_id
            JOIN dentists   d ON d.id = a.dentist_id
            JOIN treatments t ON t.id = a.treatment_id
            LEFT JOIN users u ON u.id = a.created_by
            """;

    @Override
    public List<Appointment> findAll() {
        return search(null, null, null, null, 0, 300);
    }

    /**
     * Filtered listing used by the appointment screen.
     *
     * @param term      matches appointment no / patient name / contact no (nullable)
     * @param status    exact status filter (nullable)
     * @param fromDate  inclusive lower bound (nullable)
     * @param toDate    inclusive upper bound (nullable)
     * @param dentistId 0 = any dentist
     * @param limit     maximum rows returned
     */
    public List<Appointment> search(String term, String status, String fromDate, String toDate,
                                    int dentistId, int limit) {
        StringBuilder sql = new StringBuilder(BASE).append(" WHERE 1=1 ");
        List<Object> args = new ArrayList<>();

        if (term != null && !term.isBlank()) {
            sql.append(" AND (a.appointment_no LIKE ? OR p.name LIKE ? OR p.contact_no LIKE ?) ");
            String like = "%" + term.trim() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            sql.append(" AND a.status = ? ");
            args.add(status.toUpperCase());
        }
        if (fromDate != null && !fromDate.isBlank()) {
            sql.append(" AND a.appt_date >= ? ");
            args.add(fromDate);
        }
        if (toDate != null && !toDate.isBlank()) {
            sql.append(" AND a.appt_date <= ? ");
            args.add(toDate);
        }
        if (dentistId > 0) {
            sql.append(" AND a.dentist_id = ? ");
            args.add(dentistId);
        }
        sql.append(" ORDER BY a.appt_date DESC, a.appt_time DESC LIMIT ").append(Math.max(1, limit));

        List<Appointment> out = new ArrayList<>();
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            bind(ps, args);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Appointment search failed: " + e.getMessage(), e);
        }
        return out;
    }

    @Override
    public Optional<Appointment> findById(int id) {
        return one(BASE + " WHERE a.id = ?", id);
    }

    public Optional<Appointment> findByAppointmentNo(String no) {
        return one(BASE + " WHERE a.appointment_no = ?", no);
    }

    /** Full visit history of one patient, newest first. */
    public List<Appointment> findByPatient(int patientId) {
        List<Appointment> out = new ArrayList<>();
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                     BASE + " WHERE a.patient_id = ? ORDER BY a.appt_date DESC, a.appt_time DESC")) {
            ps.setInt(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load patient history", e);
        }
        return out;
    }

    /** True when the dentist already has a live booking at that date/time. */
    public boolean isSlotTaken(int dentistId, String date, String time, int excludeAppointmentId) {
        String sql = "SELECT COUNT(*) FROM appointments "
                   + "WHERE dentist_id = ? AND appt_date = ? AND appt_time = ? "
                   + "AND status <> 'CANCELLED' AND id <> ?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, dentistId);
            ps.setString(2, date);
            ps.setString(3, time);
            ps.setInt(4, excludeAppointmentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Slot check failed", e);
        }
    }

    /** Next reference in the {@code APT-1042} series. */
    public synchronized String nextAppointmentNo() {
        String sql = "SELECT appointment_no FROM appointments ORDER BY id DESC LIMIT 1";
        int next = 1001;
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                String last = rs.getString(1);
                String digits = last == null ? "" : last.replaceAll("\\D", "");
                if (!digits.isEmpty()) {
                    next = Integer.parseInt(digits) + 1;
                }
            }
        } catch (SQLException | NumberFormatException e) {
            throw new DataAccessException("Could not generate appointment number", e);
        }
        return String.format("APT-%04d", next);
    }

    @Override
    public int insert(Appointment a) {
        String sql = "INSERT INTO appointments "
                + "(appointment_no, patient_id, dentist_id, treatment_id, appt_date, appt_time, status, notes, created_by) "
                + "VALUES (?,?,?,?,?,?,?,?,(SELECT id FROM users WHERE username = ?))";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, a.getAppointmentNo());
            ps.setInt(2, a.getPatientId());
            ps.setInt(3, a.getDentistId());
            ps.setInt(4, a.getTreatmentId());
            ps.setString(5, a.getApptDate());
            ps.setString(6, a.getApptTime());
            ps.setString(7, a.getStatus());
            ps.setString(8, a.getNotes());
            ps.setString(9, a.getCreatedBy());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not save appointment: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Appointment a) {
        String sql = "UPDATE appointments SET dentist_id=?, treatment_id=?, appt_date=?, appt_time=?, "
                   + "status=?, notes=? WHERE id=?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, a.getDentistId());
            ps.setInt(2, a.getTreatmentId());
            ps.setString(3, a.getApptDate());
            ps.setString(4, a.getApptTime());
            ps.setString(5, a.getStatus());
            ps.setString(6, a.getNotes());
            ps.setInt(7, a.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not update appointment: " + e.getMessage(), e);
        }
    }

    public boolean updateStatus(int id, String status) {
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE appointments SET status=? WHERE id=?")) {
            ps.setString(1, status);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not change appointment status", e);
        }
    }

    @Override
    public boolean delete(int id) {
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM appointments WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException(
                    "A bill has already been issued for this appointment - cancel it instead", e);
        }
    }

    private Optional<Appointment> one(String sql, Object arg) {
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (arg instanceof Integer i) {
                ps.setInt(1, i);
            } else {
                ps.setString(1, String.valueOf(arg));
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load appointment: " + e.getMessage(), e);
        }
    }

    private void bind(PreparedStatement ps, List<Object> args) throws SQLException {
        for (int i = 0; i < args.size(); i++) {
            Object v = args.get(i);
            if (v instanceof Integer n) {
                ps.setInt(i + 1, n);
            } else {
                ps.setString(i + 1, String.valueOf(v));
            }
        }
    }

    private Appointment map(ResultSet rs) throws SQLException {
        Appointment a = new Appointment();
        a.setId(rs.getInt("id"));
        a.setAppointmentNo(rs.getString("appointment_no"));
        a.setPatientId(rs.getInt("patient_id"));
        a.setDentistId(rs.getInt("dentist_id"));
        a.setTreatmentId(rs.getInt("treatment_id"));
        a.setApptDate(rs.getString("appt_date"));
        String time = rs.getString("appt_time");
        a.setApptTime(time != null && time.length() >= 5 ? time.substring(0, 5) : time);
        a.setStatus(rs.getString("status"));
        a.setNotes(rs.getString("notes"));
        a.setCreatedBy(rs.getString("created_by"));
        a.setCreatedAt(rs.getString("created_at"));
        a.setPatientName(rs.getString("patient_name"));
        a.setPatientAddress(rs.getString("patient_address"));
        a.setPatientContact(rs.getString("patient_contact"));
        a.setPatientEmail(rs.getString("patient_email"));
        a.setDentistName(rs.getString("dentist_name"));
        a.setConsultationFee(rs.getDouble("consultation_fee"));
        a.setTreatmentType(rs.getString("treatment_type"));
        a.setTreatmentCost(rs.getDouble("base_cost"));
        a.setBilled(rs.getBoolean("billed"));
        return a;
    }
}
