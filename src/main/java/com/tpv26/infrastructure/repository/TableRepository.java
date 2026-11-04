package com.tpv26.infrastructure.repository;

import com.tpv26.domain.model.DiningTable;
import com.tpv26.domain.model.TableStatus;
import com.tpv26.infrastructure.db.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class TableRepository {
    private final DatabaseManager db;

    public TableRepository(DatabaseManager db) {
        this.db = db;
    }

    public List<DiningTable> all() {
        String sql = "SELECT id, name, row_idx, col_idx, status FROM dining_tables ORDER BY row_idx, col_idx";
        List<DiningTable> tables = new ArrayList<>();
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                tables.add(new DiningTable(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getInt("row_idx"),
                    rs.getInt("col_idx"),
                    TableStatus.valueOf(rs.getString("status"))
                ));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error cargando mesas", e);
        }
        return tables;
    }

    public DiningTable create(String name, int row, int col) {
        String sql = "INSERT INTO dining_tables(name, row_idx, col_idx, status) VALUES (?, ?, ?, 'LIBRE')";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setInt(2, row);
            ps.setInt(3, col);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new DiningTable(keys.getLong(1), name, row, col, TableStatus.LIBRE);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error creando mesa", e);
        }
    }

    public void move(long tableId, int row, int col) {
        String sql = "UPDATE dining_tables SET row_idx = ?, col_idx = ? WHERE id = ?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, row);
            ps.setInt(2, col);
            ps.setLong(3, tableId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Error moviendo mesa", e);
        }
    }

    public void setStatus(long tableId, TableStatus status) {
        String sql = "UPDATE dining_tables SET status = ? WHERE id = ?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, tableId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Error cambiando estado de mesa", e);
        }
    }
}
