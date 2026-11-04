package com.tpv26.infrastructure.repository;

import com.tpv26.domain.model.IvaType;
import com.tpv26.domain.model.PaymentMethod;
import com.tpv26.domain.model.Product;
import com.tpv26.domain.model.ProductGroup;
import com.tpv26.domain.model.Sale;
import com.tpv26.domain.model.SaleLine;
import com.tpv26.domain.model.SaleStatus;
import com.tpv26.infrastructure.db.DatabaseManager;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SaleRepository {
    private final DatabaseManager db;

    public SaleRepository(DatabaseManager db) {
        this.db = db;
    }

    public Sale createSale(long cashSessionId, Long tableId, boolean barSale) {
        String sql = "INSERT INTO sales(created_at, status, payment_method, cash_session_id, table_id, bar_sale) VALUES (?, ?, ?, ?, ?, ?)";
        LocalDateTime now = LocalDateTime.now();
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, now);
            ps.setString(2, SaleStatus.ABIERTA.name());
            ps.setString(3, null);
            ps.setLong(4, cashSessionId);
            ps.setObject(5, tableId);
            ps.setBoolean(6, barSale);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new Sale(keys.getLong(1), now, SaleStatus.ABIERTA, null, cashSessionId, tableId, barSale);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error creando venta", e);
        }
    }

    public Optional<Sale> openSaleByTable(long tableId) {
        String sql = "SELECT id, created_at, status, payment_method, cash_session_id, table_id, bar_sale FROM sales WHERE table_id = ? AND status IN ('ABIERTA', 'EN_ESPERA') ORDER BY created_at DESC LIMIT 1";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, tableId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapSale(rs));
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            throw new IllegalStateException("Error buscando venta de mesa", e);
        }
    }

    public void addLine(long saleId, Product product, BigDecimal quantity, BigDecimal lineTotalWithVat) {
        String sql = "INSERT INTO sale_lines(sale_id, product_id, quantity, line_total_with_vat, iva_type) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, saleId);
            ps.setLong(2, product.id());
            ps.setBigDecimal(3, quantity);
            ps.setBigDecimal(4, lineTotalWithVat);
            ps.setString(5, product.ivaType().name());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Error agregando linea", e);
        }
    }

    public void updateLine(long lineId, BigDecimal quantity, BigDecimal lineTotalWithVat) {
        String sql = "UPDATE sale_lines SET quantity = ?, line_total_with_vat = ? WHERE id = ?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBigDecimal(1, quantity);
            ps.setBigDecimal(2, lineTotalWithVat);
            ps.setLong(3, lineId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Error actualizando linea", e);
        }
    }

    public void removeLine(long lineId) {
        String sql = "DELETE FROM sale_lines WHERE id = ?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, lineId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Error eliminando linea", e);
        }
    }

    public List<SaleLine> linesBySale(long saleId) {
        String sql = """
            SELECT sl.id AS line_id, sl.sale_id, sl.quantity, sl.line_total_with_vat, sl.iva_type,
                   p.id AS pid, p.name AS pname, p.default_price_with_vat, p.iva_type AS piva, p.display_order, p.active,
                   g.id AS gid, g.name AS gname, g.display_order AS gorder, g.active AS gactive
            FROM sale_lines sl
            JOIN products p ON p.id = sl.product_id
            JOIN product_groups g ON g.id = p.group_id
            WHERE sl.sale_id = ?
            ORDER BY sl.id
            """;

        List<SaleLine> lines = new ArrayList<>();
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, saleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ProductGroup group = new ProductGroup(rs.getLong("gid"), rs.getString("gname"), rs.getInt("gorder"), rs.getBoolean("gactive"));
                    Product product = new Product(
                        rs.getLong("pid"),
                        rs.getString("pname"),
                        group,
                        rs.getBigDecimal("default_price_with_vat"),
                        IvaType.valueOf(rs.getString("piva")),
                        rs.getInt("display_order"),
                        rs.getBoolean("active")
                    );
                    lines.add(new SaleLine(
                        rs.getLong("line_id"),
                        rs.getLong("sale_id"),
                        product,
                        rs.getBigDecimal("quantity"),
                        rs.getBigDecimal("line_total_with_vat"),
                        IvaType.valueOf(rs.getString("iva_type"))
                    ));
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error cargando lineas", e);
        }
        return lines;
    }

    public void setStatus(long saleId, SaleStatus status, PaymentMethod paymentMethod) {
        String sql = "UPDATE sales SET status = ?, payment_method = ? WHERE id = ?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setString(2, paymentMethod == null ? null : paymentMethod.name());
            ps.setLong(3, saleId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Error cambiando estado de venta", e);
        }
    }

    public List<Sale> salesBetween(LocalDateTime from, LocalDateTime to) {
        String sql = "SELECT id, created_at, status, payment_method, cash_session_id, table_id, bar_sale FROM sales WHERE created_at BETWEEN ? AND ? ORDER BY created_at DESC";
        List<Sale> sales = new ArrayList<>();
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, from);
            ps.setObject(2, to);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    sales.add(mapSale(rs));
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error cargando ventas", e);
        }
        return sales;
    }

    public Map<PaymentMethod, BigDecimal> paymentSummaryByCash(long cashId) {
        String sql = """
            SELECT payment_method, COALESCE(SUM(sl.line_total_with_vat), 0) AS amount
            FROM sales s
            LEFT JOIN sale_lines sl ON sl.sale_id = s.id
            WHERE cash_session_id = ? AND status = 'COBRADA'
            GROUP BY payment_method
            """;

        Map<PaymentMethod, BigDecimal> map = new EnumMap<>(PaymentMethod.class);
        for (PaymentMethod method : PaymentMethod.values()) {
            map.put(method, BigDecimal.ZERO);
        }

        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, cashId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String pm = rs.getString("payment_method");
                    if (pm != null) {
                        map.put(PaymentMethod.valueOf(pm), rs.getBigDecimal("amount"));
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error calculando resumen de caja", e);
        }

        return map;
    }

    private Sale mapSale(ResultSet rs) throws Exception {
        String payment = rs.getString("payment_method");
        return new Sale(
            rs.getLong("id"),
            rs.getObject("created_at", LocalDateTime.class),
            SaleStatus.valueOf(rs.getString("status")),
            payment == null ? null : PaymentMethod.valueOf(payment),
            rs.getLong("cash_session_id"),
            (Long) rs.getObject("table_id"),
            rs.getBoolean("bar_sale")
        );
    }
}
