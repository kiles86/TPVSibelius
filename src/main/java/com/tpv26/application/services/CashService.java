package com.tpv26.application.services;

import com.tpv26.application.dto.CashCloseSummary;
import com.tpv26.domain.model.CashSession;
import com.tpv26.domain.model.User;
import com.tpv26.domain.model.UserRole;
import com.tpv26.infrastructure.repository.CashAndUserRepository;
import com.tpv26.infrastructure.repository.SaleRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class CashService {
    private final CashAndUserRepository repo;
    private final SaleRepository saleRepository;

    public CashService(CashAndUserRepository repo, SaleRepository saleRepository) {
        this.repo = repo;
        this.saleRepository = saleRepository;
    }

    public List<User> users() {
        return repo.users();
    }

    public User createUser(String username, String password, UserRole role) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Usuario obligatorio");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password obligatorio");
        }
        return repo.createUser(username.trim(), password.trim(), role);
    }

    public CashSession openCash(long userId, BigDecimal openingAmount) {
        if (openingAmount == null || openingAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Importe inicial invalido");
        }
        if (repo.openCashSession().isPresent()) {
            throw new IllegalStateException("Ya hay caja abierta");
        }
        return repo.openCash(userId, openingAmount);
    }

    public Optional<CashSession> openCashSession() {
        return repo.openCashSession();
    }

    public CashCloseSummary closeSummary() {
        CashSession cash = repo.openCashSession().orElseThrow(() -> new IllegalStateException("No hay caja abierta"));
        return new CashCloseSummary(cash.openingAmount(), saleRepository.paymentSummaryByCash(cash.id()));
    }

    public void closeCash() {
        CashSession cash = repo.openCashSession().orElseThrow(() -> new IllegalStateException("No hay caja abierta"));
        repo.closeCash(cash.id());
    }
}
