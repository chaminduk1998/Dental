package com.dental.dao;

import com.dental.config.DatabaseConnection;
import com.dental.model.Treatment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** DAO for the {@code treatments} table. */
public class TreatmentDAO implements GenericDAO<Treatment> {

    private static final String SELECT =
            "SELECT id, treatment_type, base_cost, duration_min, active FROM treatments ";

    @Override
    public List<Treatment> findAll() {
        return query(SELECT + "ORDER BY treatment_type");
    }

    public List<Treatment> findActive() {
        return query(SELECT + "WHERE active = 1 ORDER BY treatment_type");
    }

    @Override
    public Optional<Treatment> findById(int id) {
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + "WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load treatment " + id, e);
        }
    }

    @Override
    public int insert(Treatment t) {
        String sql = "INSERT INTO treatments (treatment_type, base_cost, duration_min, active) VALUES (?,?,?,?)";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, t.getTreatmentType());
            ps.setDouble(2, t.getBaseCost());
            ps.setInt(3, t.getDurationMin());
            ps.setBoolean(4, t.isActive());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not save treatment: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Treatment t) {
        String sql = "UPDATE treatments SET treatment_type=?, base_cost=?, duration_min=?, active=? WHERE id=?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, t.getTreatmentType());
            ps.setDouble(2, t.getBaseCost());
            ps.setInt(3, t.getDurationMin());
            ps.setBoolean(4, t.isActive());
            ps.setInt(5, t.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not update treatment: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(int id) {
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM treatments WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException(
                    "This treatment is used by existing appointments - mark it inactive instead", e);
        }
    }

    private List<Treatment> query(String sql) {
        List<Treatment> out = new ArrayList<>();
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load treatments", e);
        }
        return out;
    }

    private Treatment map(ResultSet rs) throws SQLException {
        Treatment t = new Treatment();
        t.setId(rs.getInt("id"));
        t.setTreatmentType(rs.getString("treatment_type"));
        t.setBaseCost(rs.getDouble("base_cost"));
        t.setDurationMin(rs.getInt("duration_min"));
        t.setActive(rs.getBoolean("active"));
        return t;
    }
}
