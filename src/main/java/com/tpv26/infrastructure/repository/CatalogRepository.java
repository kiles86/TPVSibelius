package com.tpv26.infrastructure.repository;

import com.tpv26.domain.model.IvaType;
import com.tpv26.domain.model.Product;
import com.tpv26.domain.model.ProductGroup;
import com.tpv26.infrastructure.db.DatabaseManager;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CatalogRepository {
    private final DatabaseManager db;

    public CatalogRepository(DatabaseManager db) {
        this.db = db;
    }

    public List<ProductGroup> activeGroups() {
        String sql = "SELECT id, name, display_order, active FROM product_groups WHERE active = true ORDER BY display_order";
        List<ProductGroup> groups = new ArrayList<>();
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                groups.add(new ProductGroup(rs.getLong("id"), rs.getString("name"), rs.getInt("display_order"), rs.getBoolean("active")));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error cargando grupos", e);
        }
        return groups;
    }

    public List<ProductGroup> allGroups() {
        String sql = "SELECT id, name, display_order, active FROM product_groups ORDER BY display_order";
        List<ProductGroup> groups = new ArrayList<>();
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                groups.add(new ProductGroup(rs.getLong("id"), rs.getString("name"), rs.getInt("display_order"), rs.getBoolean("active")));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error cargando grupos", e);
        }
        return groups;
    }

    public List<Product> activeProductsByGroup(Long groupId) {
        String sql = """
            SELECT p.id, p.name, p.default_price_with_vat, p.iva_type, p.display_order, p.active,
                   g.id AS gid, g.name AS gname, g.display_order AS gorder, g.active AS gactive
            FROM products p
            JOIN product_groups g ON g.id = p.group_id
            WHERE p.active = true AND p.group_id = ?
            ORDER BY p.display_order
            """;

        List<Product> products = new ArrayList<>();
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ProductGroup group = new ProductGroup(rs.getLong("gid"), rs.getString("gname"), rs.getInt("gorder"), rs.getBoolean("gactive"));
                    products.add(new Product(
                        rs.getLong("id"),
                        rs.getString("name"),
                        group,
                        rs.getBigDecimal("default_price_with_vat"),
                        IvaType.valueOf(rs.getString("iva_type")),
                        rs.getInt("display_order"),
                        rs.getBoolean("active")
                    ));
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error cargando productos", e);
        }
        return products;
    }

    public List<Product> allProducts() {
        String sql = """
            SELECT p.id, p.name, p.default_price_with_vat, p.iva_type, p.display_order, p.active,
                   g.id AS gid, g.name AS gname, g.display_order AS gorder, g.active AS gactive
            FROM products p
            JOIN product_groups g ON g.id = p.group_id
            ORDER BY g.display_order, p.display_order
            """;

        List<Product> products = new ArrayList<>();
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ProductGroup group = new ProductGroup(rs.getLong("gid"), rs.getString("gname"), rs.getInt("gorder"), rs.getBoolean("gactive"));
                products.add(new Product(
                    rs.getLong("id"),
                    rs.getString("name"),
                    group,
                    rs.getBigDecimal("default_price_with_vat"),
                    IvaType.valueOf(rs.getString("iva_type")),
                    rs.getInt("display_order"),
                    rs.getBoolean("active")
                ));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error cargando productos", e);
        }
        return products;
    }

    public ProductGroup createGroup(String name, int order) {
        String sql = "INSERT INTO product_groups(name, display_order, active) VALUES (?, ?, true)";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setInt(2, order);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new ProductGroup(keys.getLong(1), name, order, true);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error creando grupo", e);
        }
    }

    public Product createProduct(String name, long groupId, BigDecimal defaultPriceWithVat, IvaType ivaType, int order) {
        String sql = "INSERT INTO products(name, group_id, default_price_with_vat, iva_type, display_order, active) VALUES (?, ?, ?, ?, ?, true)";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setLong(2, groupId);
            ps.setBigDecimal(3, defaultPriceWithVat);
            ps.setString(4, ivaType.name());
            ps.setInt(5, order);
            ps.executeUpdate();
            ProductGroup group = getGroup(c, groupId);
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new Product(keys.getLong(1), name, group, defaultPriceWithVat, ivaType, order, true);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error creando producto", e);
        }
    }

    public void setGroupActive(long groupId, boolean active) {
        String sql = "UPDATE product_groups SET active = ? WHERE id = ?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setLong(2, groupId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Error actualizando estado de grupo", e);
        }
    }

    public void setProductActive(long productId, boolean active) {
        String sql = "UPDATE products SET active = ? WHERE id = ?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setLong(2, productId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Error actualizando estado de producto", e);
        }
    }

    private ProductGroup getGroup(Connection c, long groupId) throws Exception {
        try (PreparedStatement ps = c.prepareStatement("SELECT id, name, display_order, active FROM product_groups WHERE id = ?")) {
            ps.setLong(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new ProductGroup(rs.getLong("id"), rs.getString("name"), rs.getInt("display_order"), rs.getBoolean("active"));
            }
        }
    }
}
