package com.tpv26.infrastructure.repository;

import com.tpv26.domain.model.CashSession;
import com.tpv26.domain.model.User;
import com.tpv26.domain.model.UserRole;
import com.tpv26.infrastructure.db.DatabaseManager;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CashAndUserRepository {
    private final DatabaseManager db;

    public CashAndUserRepository(DatabaseManager db) {
        this.db = db;
    }

    public List<User> users() {
        String sql = "SELECT id, username, password_hash, role FROM users ORDER BY username";
        List<User> users = new ArrayList<>();
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(new User(rs.getLong("id"), rs.getString("username"), rs.getString("password_hash"), UserRole.valueOf(rs.getString("role"))));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error cargando usuarios", e);
        }
        return users;
    }

    public User createUser(String username, String passwordHash, UserRole role) {
        String sql = "INSERT INTO users(username, password_hash, role) VALUES (?, ?, ?)";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.setString(3, role.name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new User(keys.getLong(1), username, passwordHash, role);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error creando usuario", e);
        }
    }

    public CashSession openCash(long userId, BigDecimal openingAmount) {
        String sql = "INSERT INTO cash_sessions(user_id, opened_at, closed_at, opening_amount, open) VALUES (?, ?, ?, ?, true)";
        LocalDateTime now = LocalDateTime.now();
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, userId);
            ps.setObject(2, now);
            ps.setObject(3, null);
            ps.setBigDecimal(4, openingAmount);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new CashSession(keys.getLong(1), userId, now, null, openingAmount, true);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error abriendo caja", e);
        }
    }

    public Optional<CashSession> openCashSession() {
        String sql = "SELECT id, user_id, opened_at, closed_at, opening_amount, open FROM cash_sessions WHERE open = true ORDER BY opened_at DESC LIMIT 1";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return Optional.of(new CashSession(
                    rs.getLong("id"),
                    rs.getLong("user_id"),
                    rs.getObject("opened_at", LocalDateTime.class),
                    rs.getObject("closed_at", LocalDateTime.class),
                    rs.getBigDecimal("opening_amount"),
                    rs.getBoolean("open")
                ));
            }
            return Optional.empty();
        } catch (Exception e) {
            throw new IllegalStateException("Error consultando caja", e);
        }
    }

    public void closeCash(long cashId) {
        String sql = "UPDATE cash_sessions SET open = false, closed_at = ? WHERE id = ?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, LocalDateTime.now());
            ps.setLong(2, cashId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Error cerrando caja", e);
        }
    }
}
