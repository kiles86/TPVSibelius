package com.tpv26.application.services;

import com.tpv26.domain.model.IvaType;
import com.tpv26.domain.model.Product;
import com.tpv26.domain.model.ProductGroup;
import com.tpv26.infrastructure.repository.CatalogRepository;

import java.math.BigDecimal;
import java.util.List;

public class CatalogService {
    private final CatalogRepository repo;

    public CatalogService(CatalogRepository repo) {
        this.repo = repo;
    }

    public List<ProductGroup> groups() {
        return repo.activeGroups();
    }

    public List<ProductGroup> allGroups() {
        return repo.allGroups();
    }

    public List<Product> productsByGroup(long groupId) {
        return repo.activeProductsByGroup(groupId);
    }

    public List<Product> allProducts() {
        return repo.allProducts();
    }

    public ProductGroup createGroup(String name, int order) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Nombre de grupo obligatorio");
        }
        return repo.createGroup(name.trim(), order);
    }

    public Product createProduct(String name, long groupId, BigDecimal priceWithVat, IvaType ivaType, int order) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Nombre de producto obligatorio");
        }
        if (priceWithVat == null || priceWithVat.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Precio con IVA invalido");
        }
        return repo.createProduct(name.trim(), groupId, priceWithVat, ivaType, order);
    }

    public void setGroupActive(long groupId, boolean active) {
        repo.setGroupActive(groupId, active);
    }

    public void setProductActive(long productId, boolean active) {
        repo.setProductActive(productId, active);
    }
}
