package com.dental.dao;

import com.dental.config.DatabaseConnection;
import com.dental.model.AuditLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** DAO for the {@code audit_logs} table. */
public class AuditLogDAO implements GenericDAO<AuditLog> {

    private static final String SELECT =
            "SELECT id, username, action, entity, entity_ref, details, created_at FROM audit_logs ";

    @Override
    public List<AuditLog> findAll() {
        return findRecent(200, null);
    }

    public List<AuditLog> findRecent(int limit, String entityFilter) {
        StringBuilder sql = new StringBuilder(SELECT);
        boolean filtered = entityFilter != null && !entityFilter.isBlank()
                && !"ALL".equalsIgnoreCase(entityFilter);
        if (filtered) {
            sql.append("WHERE entity = ? ");
        }
        sql.append("ORDER BY id DESC LIMIT ").append(Math.max(1, limit));

        List<AuditLog> out = new ArrayList<>();
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            if (filtered) {
                ps.setString(1, entityFilter.toUpperCase());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load audit log", e);
        }
        return out;
    }

    @Override
    public Optional<AuditLog> findById(int id) {
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + "WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load audit entry", e);
        }
    }

    @Override
    public int insert(AuditLog log) {
        String sql = "INSERT INTO audit_logs (username, action, entity, entity_ref, details) VALUES (?,?,?,?,?)";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, log.getUsername());
            ps.setString(2, log.getAction());
            ps.setString(3, log.getEntity());
            ps.setString(4, log.getEntityRef());
            ps.setString(5, log.getDetails());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        } catch (SQLException e) {
            // auditing must never break the operation that triggered it
            System.err.println("[audit] could not write entry: " + e.getMessage());
            return 0;
        }
    }

    @Override
    public boolean update(AuditLog entity) {
        throw new UnsupportedOperationException("Audit entries are immutable");
    }

    @Override
    public boolean delete(int id) {
        throw new UnsupportedOperationException("Audit entries are immutable");
    }

    private AuditLog map(ResultSet rs) throws SQLException {
        AuditLog a = new AuditLog();
        a.setId(rs.getInt("id"));
        a.setUsername(rs.getString("username"));
        a.setAction(rs.getString("action"));
        a.setEntity(rs.getString("entity"));
        a.setEntityRef(rs.getString("entity_ref"));
        a.setDetails(rs.getString("details"));
        a.setCreatedAt(rs.getString("created_at"));
        return a;
    }
}
