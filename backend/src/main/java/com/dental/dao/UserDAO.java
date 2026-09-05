package com.dental.dao;

import com.dental.config.DatabaseConnection;
import com.dental.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** DAO for the {@code users} table. */
public class UserDAO implements GenericDAO<User> {

    private static final String SELECT =
            "SELECT id, username, password, role, full_name, active, created_at FROM users ";

    @Override
    public List<User> findAll() {
        List<User> out = new ArrayList<>();
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + "ORDER BY username");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load users", e);
        }
        return out;
    }

    @Override
    public Optional<User> findById(int id) {
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + "WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load user " + id, e);
        }
    }

    public Optional<User> findByUsername(String username) {
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + "WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not look up user " + username, e);
        }
    }

    @Override
    public int insert(User u) {
        String sql = "INSERT INTO users (username, password, role, full_name, active) VALUES (?,?,?,?,?)";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getUsername());
            ps.setString(2, u.getPassword());
            ps.setString(3, u.getRole());
            ps.setString(4, u.getFullName());
            ps.setBoolean(5, u.isActive());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not create user: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(User u) {
        boolean withPassword = u.getPassword() != null && !u.getPassword().isBlank();
        String sql = withPassword
                ? "UPDATE users SET username=?, full_name=?, role=?, active=?, password=? WHERE id=?"
                : "UPDATE users SET username=?, full_name=?, role=?, active=? WHERE id=?";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, u.getUsername());
            ps.setString(i++, u.getFullName());
            ps.setString(i++, u.getRole());
            ps.setBoolean(i++, u.isActive());
            if (withPassword) {
                ps.setString(i++, u.getPassword());
            }
            ps.setInt(i, u.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not update user: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(int id) {
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM users WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete user: " + e.getMessage(), e);
        }
    }

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setPassword(rs.getString("password"));
        u.setRole(rs.getString("role"));
        u.setFullName(rs.getString("full_name"));
        u.setActive(rs.getBoolean("active"));
        u.setCreatedAt(rs.getString("created_at"));
        return u;
    }
}
