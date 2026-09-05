package com.dental.dao;

import com.dental.config.DatabaseConnection;
import com.dental.model.Patient;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** DAO for the {@code patients} table. */
public class PatientDAO implements GenericDAO<Patient> {

    private static final String BASE =
            "SELECT p.id, p.name, p.address, p.contact_no, p.email, p.created_at, "
          + "       (SELECT COUNT(*) FROM appointments a WHERE a.patient_id = p.id) AS visit_count "
          + "FROM patients p ";

    @Override
    public List<Patient> findAll() {
        return query(BASE + "ORDER BY p.name");
    }

    /** Free-text search over name / contact number / address. */
    public List<Patient> search(String term) {
        if (term == null || term.isBlank()) {
            return findAll();
        }
        List<Patient> out = new ArrayList<>();
        String sql = BASE + "WHERE p.name LIKE ? OR p.contact_no LIKE ? OR p.address LIKE ? ORDER BY p.name";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            String like = "%" + term.trim() + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Patient search failed", e);
        }
        return out;
    }

    @Override
    public Optional<Patient> findById(int id) {
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(BASE + "WHERE p.id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load patient " + id, e);
        }
    }

    /**
     * Finds an existing patient by name + contact number, so booking a repeat
     * visit does not create a duplicate record.
     */
    public Optional<Patient> findByNameAndContact(String name, String contactNo) {
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(BASE + "WHERE p.name = ? AND p.contact_no = ? LIMIT 1")) {
            ps.setString(1, name);
            ps.setString(2, contactNo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Patient lookup failed", e);
        }
    }

    @Override
    public int insert(Patient p) {
        String sql = "INSERT INTO patients (name, address, contact_no, email) VALUES (?,?,?,?)";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getName());
            ps.setString(2, p.getAddress());
            ps.setString(3, p.getContactNo());
            ps.setString(4, p.getEmail());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not save patient: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Patient p) {
        String sql = "UPDATE patients SET name=?, address=?, contact_no=?, email=? WHERE id=?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.getName());
            ps.setString(2, p.getAddress());
            ps.setString(3, p.getContactNo());
            ps.setString(4, p.getEmail());
            ps.setInt(5, p.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not update patient: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(int id) {
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM patients WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException(
                    "This patient has appointments on record and cannot be deleted", e);
        }
    }

    private List<Patient> query(String sql) {
        List<Patient> out = new ArrayList<>();
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load patients", e);
        }
        return out;
    }

    private Patient map(ResultSet rs) throws SQLException {
        Patient p = new Patient();
        p.setId(rs.getInt("id"));
        p.setName(rs.getString("name"));
        p.setAddress(rs.getString("address"));
        p.setContactNo(rs.getString("contact_no"));
        p.setEmail(rs.getString("email"));
        p.setCreatedAt(rs.getString("created_at"));
        p.setVisitCount(rs.getInt("visit_count"));
        return p;
    }
}
