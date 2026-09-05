package com.dental.dao;

import com.dental.config.DatabaseConnection;
import com.dental.model.Notification;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** DAO for the {@code notifications} table written by the observers. */
public class NotificationDAO implements GenericDAO<Notification> {

    private static final String SELECT =
            "SELECT id, recipient, channel, subject, message, status, created_at FROM notifications ";

    @Override
    public List<Notification> findAll() {
        List<Notification> out = new ArrayList<>();
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + "ORDER BY id DESC LIMIT 200");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load notifications", e);
        }
        return out;
    }

    @Override
    public Optional<Notification> findById(int id) {
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + "WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load notification", e);
        }
    }

    @Override
    public int insert(Notification n) {
        String sql = "INSERT INTO notifications (recipient, channel, subject, message, status) VALUES (?,?,?,?,?)";
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, n.getRecipient());
            ps.setString(2, n.getChannel());
            ps.setString(3, n.getSubject());
            ps.setString(4, n.getMessage());
            ps.setString(5, n.getStatus());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not queue notification", e);
        }
    }

    @Override
    public boolean update(Notification n) {
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE notifications SET status = ? WHERE id = ?")) {
            ps.setString(1, n.getStatus());
            ps.setInt(2, n.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not update notification", e);
        }
    }

    @Override
    public boolean delete(int id) {
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM notifications WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete notification", e);
        }
    }

    private Notification map(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setId(rs.getInt("id"));
        n.setRecipient(rs.getString("recipient"));
        n.setChannel(rs.getString("channel"));
        n.setSubject(rs.getString("subject"));
        n.setMessage(rs.getString("message"));
        n.setStatus(rs.getString("status"));
        n.setCreatedAt(rs.getString("created_at"));
        return n;
    }
}
