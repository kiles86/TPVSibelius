package com.tpv26;

import com.tpv26.application.services.CashService;
import com.tpv26.application.services.CatalogService;
import com.tpv26.application.services.ReportService;
import com.tpv26.application.services.SaleService;
import com.tpv26.application.services.TableService;
import com.tpv26.application.services.TaxCalculator;
import com.tpv26.infrastructure.db.DatabaseManager;
import com.tpv26.infrastructure.repository.CashAndUserRepository;
import com.tpv26.infrastructure.repository.CatalogRepository;
import com.tpv26.infrastructure.repository.SaleRepository;
import com.tpv26.infrastructure.repository.TableRepository;
import com.tpv26.ui.TpvDashboard;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) {
        DatabaseManager db = new DatabaseManager();
        db.initialize();

        CatalogRepository catalogRepository = new CatalogRepository(db);
        TableRepository tableRepository = new TableRepository(db);
        CashAndUserRepository cashAndUserRepository = new CashAndUserRepository(db);
        SaleRepository saleRepository = new SaleRepository(db);

        TaxCalculator taxCalculator = new TaxCalculator();
        CatalogService catalogService = new CatalogService(catalogRepository);
        TableService tableService = new TableService(tableRepository);
        CashService cashService = new CashService(cashAndUserRepository, saleRepository);
        SaleService saleService = new SaleService(saleRepository, cashAndUserRepository, tableService, taxCalculator);
        ReportService reportService = new ReportService(saleRepository, taxCalculator);

        Scene scene = new Scene(new TpvDashboard(catalogService, tableService, cashService, saleService, reportService), 1360, 860);
        scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());

        stage.setTitle("TPV26 - Barra y Comedor");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
