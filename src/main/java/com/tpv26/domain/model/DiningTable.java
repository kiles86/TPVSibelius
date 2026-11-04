package com.tpv26.domain.model;

public record DiningTable(Long id, String name, int rowIndex, int colIndex, TableStatus status) {
    @Override
    public String toString() {
        return name;
    }
}
