package com.tpv26.domain.model;

public record ProductGroup(Long id, String name, int displayOrder, boolean active) {
    @Override
    public String toString() {
        return name;
    }
}
