package com.tpv26.application.services;

import com.tpv26.application.dto.SaleTotals;
import com.tpv26.domain.model.PaymentMethod;
import com.tpv26.domain.model.Product;
import com.tpv26.domain.model.Sale;
import com.tpv26.domain.model.SaleLine;
import com.tpv26.domain.model.SaleStatus;
import com.tpv26.infrastructure.repository.CashAndUserRepository;
import com.tpv26.infrastructure.repository.SaleRepository;

import java.math.BigDecimal;
import java.util.List;

public class SaleService {
    private final SaleRepository repo;
    private final CashAndUserRepository cashRepo;
    private final TableService tableService;
    private final TaxCalculator taxCalculator;

    public SaleService(SaleRepository repo, CashAndUserRepository cashRepo, TableService tableService, TaxCalculator taxCalculator) {
        this.repo = repo;
        this.cashRepo = cashRepo;
        this.tableService = tableService;
        this.taxCalculator = taxCalculator;
    }

    public Sale startBarSale() {
        long cashId = currentCash();
        return repo.createSale(cashId, null, true);
    }

    public Sale loadOrStartTableSale(long tableId) {
        long cashId = currentCash();
        Sale sale = repo.openSaleByTable(tableId).orElseGet(() -> repo.createSale(cashId, tableId, false));
        tableService.markOccupied(tableId);
        return sale;
    }

    public void addProduct(long saleId, Product product) {
        repo.addLine(saleId, product, BigDecimal.ONE, product.defaultPriceWithVat());
    }

    public void updateLine(long lineId, BigDecimal qty, BigDecimal lineTotalWithVat) {
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Cantidad invalida");
        }
        if (lineTotalWithVat == null || lineTotalWithVat.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Importe total invalido");
        }
        repo.updateLine(lineId, qty, lineTotalWithVat);
    }

    public void removeLine(long lineId) {
        repo.removeLine(lineId);
    }

    public List<SaleLine> lines(long saleId) {
        return repo.linesBySale(saleId);
    }

    public SaleTotals totals(long saleId) {
        return taxCalculator.calculate(lines(saleId));
    }

    public void hold(Sale sale) {
        ensureHasLines(sale.id());
        repo.setStatus(sale.id(), SaleStatus.EN_ESPERA, null);
    }

    public void cancel(Sale sale) {
        repo.setStatus(sale.id(), SaleStatus.CANCELADA, null);
        if (sale.tableId() != null) {
            tableService.markFree(sale.tableId());
        }
    }

    public void charge(Sale sale, PaymentMethod paymentMethod) {
        ensureHasLines(sale.id());
        repo.setStatus(sale.id(), SaleStatus.COBRADA, paymentMethod);
        if (sale.tableId() != null) {
            tableService.markFree(sale.tableId());
        }
    }

    private long currentCash() {
        return cashRepo.openCashSession().orElseThrow(() -> new IllegalStateException("No se puede vender sin caja abierta")).id();
    }

    private void ensureHasLines(long saleId) {
        if (repo.linesBySale(saleId).isEmpty()) {
            throw new IllegalStateException("La venta no tiene lineas");
        }
    }
}
