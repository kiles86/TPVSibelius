package com.tpv26.application.services;

import com.tpv26.application.dto.SaleTotals;
import com.tpv26.domain.model.Sale;
import com.tpv26.domain.model.SaleStatus;
import com.tpv26.infrastructure.repository.SaleRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ReportService {
    private final SaleRepository repo;
    private final TaxCalculator taxCalculator;

    public ReportService(SaleRepository repo, TaxCalculator taxCalculator) {
        this.repo = repo;
        this.taxCalculator = taxCalculator;
    }

    public String report(LocalDate fromDate, LocalDate toDate) {
        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = toDate.plusDays(1).atStartOfDay().minusNanos(1);

        int charged = 0;
        int onHold = 0;
        int cancelled = 0;
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal vat = BigDecimal.ZERO;

        for (Sale sale : repo.salesBetween(from, to)) {
            if (sale.status() == SaleStatus.COBRADA) {
                charged++;
                SaleTotals totals = taxCalculator.calculate(repo.linesBySale(sale.id()));
                total = total.add(totals.total());
                vat = vat.add(totals.vat());
            } else if (sale.status() == SaleStatus.EN_ESPERA) {
                onHold++;
            } else if (sale.status() == SaleStatus.CANCELADA) {
                cancelled++;
            }
        }

        return """
            Informe de ventas
            Periodo: %s - %s

            Ventas cobradas: %d
            Ventas en espera: %d
            Ventas canceladas: %d

            Total cobrado (IVA incl.): %.2f EUR
            IVA repercutido: %.2f EUR
            """.formatted(fromDate, toDate, charged, onHold, cancelled, total, vat);
    }
}
