package com.dental.dao;

import com.dental.config.DatabaseConnection;
import com.dental.model.Dentist;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** DAO for the {@code dentists} table. */
public class DentistDAO implements GenericDAO<Dentist> {

    private static final String SELECT =
            "SELECT id, name, specialization, consultation_fee, active FROM dentists ";

    @Override
    public List<Dentist> findAll() {
        return query(SELECT + "ORDER BY name");
    }

    public List<Dentist> findActive() {
        return query(SELECT + "WHERE active = 1 ORDER BY name");
    }

    @Override
    public Optional<Dentist> findById(int id) {
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + "WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load dentist " + id, e);
        }
    }

    @Override
    public int insert(Dentist d) {
        String sql = "INSERT INTO dentists (name, specialization, consultation_fee, active) VALUES (?,?,?,?)";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, d.getName());
            ps.setString(2, d.getSpecialization());
            ps.setDouble(3, d.getConsultationFee());
            ps.setBoolean(4, d.isActive());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not save dentist: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Dentist d) {
        String sql = "UPDATE dentists SET name=?, specialization=?, consultation_fee=?, active=? WHERE id=?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, d.getName());
            ps.setString(2, d.getSpecialization());
            ps.setDouble(3, d.getConsultationFee());
            ps.setBoolean(4, d.isActive());
            ps.setInt(5, d.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not update dentist: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(int id) {
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM dentists WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException(
                    "This dentist already has appointments - mark them inactive instead of deleting", e);
        }
    }

    private List<Dentist> query(String sql) {
        List<Dentist> out = new ArrayList<>();
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load dentists", e);
        }
        return out;
    }

    private Dentist map(ResultSet rs) throws SQLException {
        Dentist d = new Dentist();
        d.setId(rs.getInt("id"));
        d.setName(rs.getString("name"));
        d.setSpecialization(rs.getString("specialization"));
        d.setConsultationFee(rs.getDouble("consultation_fee"));
        d.setActive(rs.getBoolean("active"));
        return d;
    }
}
