package com.dental.dao;

import com.dental.config.DatabaseConnection;
import com.dental.model.Bill;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** DAO for the {@code bills} table. */
public class BillDAO implements GenericDAO<Bill> {

    private static final String BASE = """
            SELECT b.id, b.bill_no, b.appointment_id, b.treatment_cost, b.consultation_fee,
                   b.discount, b.tax, b.total, b.pricing_strategy, b.payment_method,
                   b.issued_by, b.issued_at,
                   a.appointment_no, a.appt_date, a.appt_time,
                   p.name AS patient_name, p.address AS patient_address, p.contact_no AS patient_contact,
                   d.name AS dentist_name, t.treatment_type
            FROM bills b
            JOIN appointments a ON a.id = b.appointment_id
            JOIN patients   p ON p.id = a.patient_id
            JOIN dentists   d ON d.id = a.dentist_id
            JOIN treatments t ON t.id = a.treatment_id
            """;

    @Override
    public List<Bill> findAll() {
        List<Bill> out = new ArrayList<>();
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(BASE + " ORDER BY b.issued_at DESC LIMIT 300");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load bills", e);
        }
        return out;
    }

    @Override
    public Optional<Bill> findById(int id) {
        return one(BASE + " WHERE b.id = ?", id);
    }

    public Optional<Bill> findByAppointmentId(int appointmentId) {
        return one(BASE + " WHERE b.appointment_id = ?", appointmentId);
    }

    public Optional<Bill> findByBillNo(String billNo) {
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(BASE + " WHERE b.bill_no = ?")) {
            ps.setString(1, billNo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load bill", e);
        }
    }

    public synchronized String nextBillNo() {
        int next = 5001;
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT bill_no FROM bills ORDER BY id DESC LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                String digits = String.valueOf(rs.getString(1)).replaceAll("\\D", "");
                if (!digits.isEmpty()) {
                    next = Integer.parseInt(digits) + 1;
                }
            }
        } catch (SQLException | NumberFormatException e) {
            throw new DataAccessException("Could not generate bill number", e);
        }
        return String.format("BILL-%04d", next);
    }

    @Override
    public int insert(Bill b) {
        String sql = "INSERT INTO bills (bill_no, appointment_id, treatment_cost, consultation_fee, "
                   + "discount, tax, total, pricing_strategy, payment_method, issued_by) "
                   + "VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, b.getBillNo());
            ps.setInt(2, b.getAppointmentId());
            ps.setDouble(3, b.getTreatmentCost());
            ps.setDouble(4, b.getConsultationFee());
            ps.setDouble(5, b.getDiscount());
            ps.setDouble(6, b.getTax());
            ps.setDouble(7, b.getTotal());
            ps.setString(8, b.getPricingStrategy());
            ps.setString(9, b.getPaymentMethod());
            ps.setString(10, b.getIssuedBy());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not save bill: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Bill b) {
        String sql = "UPDATE bills SET treatment_cost=?, consultation_fee=?, discount=?, tax=?, total=?, "
                   + "pricing_strategy=?, payment_method=? WHERE id=?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDouble(1, b.getTreatmentCost());
            ps.setDouble(2, b.getConsultationFee());
            ps.setDouble(3, b.getDiscount());
            ps.setDouble(4, b.getTax());
            ps.setDouble(5, b.getTotal());
            ps.setString(6, b.getPricingStrategy());
            ps.setString(7, b.getPaymentMethod());
            ps.setInt(8, b.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not update bill", e);
        }
    }

    @Override
    public boolean delete(int id) {
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM bills WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete bill", e);
        }
    }

    private Optional<Bill> one(String sql, int arg) {
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, arg);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load bill", e);
        }
    }

    private Bill map(ResultSet rs) throws SQLException {
        Bill b = new Bill();
        b.setId(rs.getInt("id"));
        b.setBillNo(rs.getString("bill_no"));
        b.setAppointmentId(rs.getInt("appointment_id"));
        b.setTreatmentCost(rs.getDouble("treatment_cost"));
        b.setConsultationFee(rs.getDouble("consultation_fee"));
        b.setDiscount(rs.getDouble("discount"));
        b.setTax(rs.getDouble("tax"));
        b.setTotal(rs.getDouble("total"));
        b.setPricingStrategy(rs.getString("pricing_strategy"));
        b.setPaymentMethod(rs.getString("payment_method"));
        b.setIssuedBy(rs.getString("issued_by"));
        b.setIssuedAt(rs.getString("issued_at"));
        b.setAppointmentNo(rs.getString("appointment_no"));
        b.setApptDate(rs.getString("appt_date"));
        String time = rs.getString("appt_time");
        b.setApptTime(time != null && time.length() >= 5 ? time.substring(0, 5) : time);
        b.setPatientName(rs.getString("patient_name"));
        b.setPatientAddress(rs.getString("patient_address"));
        b.setPatientContact(rs.getString("patient_contact"));
        b.setDentistName(rs.getString("dentist_name"));
        b.setTreatmentType(rs.getString("treatment_type"));
        return b;
    }
}
