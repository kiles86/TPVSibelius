package com.tpv26.application.services;

import com.tpv26.domain.model.DiningTable;
import com.tpv26.domain.model.TableStatus;
import com.tpv26.infrastructure.repository.TableRepository;

import java.util.List;

public class TableService {
    private final TableRepository repo;

    public TableService(TableRepository repo) {
        this.repo = repo;
    }

    public List<DiningTable> tables() {
        return repo.all();
    }

    public DiningTable createTable(String name, int row, int col) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Nombre de mesa obligatorio");
        }
        if (row < 0 || col < 0) {
            throw new IllegalArgumentException("Fila y columna deben ser >= 0");
        }
        return repo.create(name.trim(), row, col);
    }

    public void moveTable(long tableId, int row, int col) {
        repo.move(tableId, row, col);
    }

    public void markOccupied(long tableId) {
        repo.setStatus(tableId, TableStatus.OCUPADA);
    }

    public void markFree(long tableId) {
        repo.setStatus(tableId, TableStatus.LIBRE);
    }
}
