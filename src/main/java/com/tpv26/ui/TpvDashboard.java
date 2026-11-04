package com.tpv26.ui;

import com.tpv26.application.dto.CashCloseSummary;
import com.tpv26.application.dto.SaleTotals;
import com.tpv26.application.services.CashService;
import com.tpv26.application.services.CatalogService;
import com.tpv26.application.services.ReportService;
import com.tpv26.application.services.SaleService;
import com.tpv26.application.services.TableService;
import com.tpv26.domain.model.CashSession;
import com.tpv26.domain.model.DiningTable;
import com.tpv26.domain.model.IvaType;
import com.tpv26.domain.model.PaymentMethod;
import com.tpv26.domain.model.Product;
import com.tpv26.domain.model.ProductGroup;
import com.tpv26.domain.model.Sale;
import com.tpv26.domain.model.SaleLine;
import com.tpv26.domain.model.TableStatus;
import com.tpv26.domain.model.User;
import com.tpv26.domain.model.UserRole;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class TpvDashboard extends BorderPane {
    private final CatalogService catalogService;
    private final TableService tableService;
    private final CashService cashService;
    private final SaleService saleService;
    private final ReportService reportService;

    private final ObservableList<DiningTable> diningTables = FXCollections.observableArrayList();
    private final ObservableList<ProductGroup> groups = FXCollections.observableArrayList();
    private final ObservableList<ProductGroup> allGroups = FXCollections.observableArrayList();
    private final ObservableList<Product> currentGroupProducts = FXCollections.observableArrayList();
    private final ObservableList<Product> allProducts = FXCollections.observableArrayList();
    private final ObservableList<SaleLine> currentLines = FXCollections.observableArrayList();

    private final FlowPane groupPane = new FlowPane(8, 8);
    private final FlowPane productPane = new FlowPane(8, 8);

    private ComboBox<User> currentUserCombo;
    private Label currentRoleLabel;

    private TableView<SaleLine> linesTable;
    private Label currentSaleLabel;
    private Label totalsLabel;
    private Button tableSelectorButton;
    private ComboBox<PaymentMethod> paymentMethodCombo;

    private Label cashStatusLabel;
    private TextField openingAmountField;

    private DatePicker fromPicker;
    private DatePicker toPicker;
    private TextArea reportArea;

    private TableView<ProductGroup> groupsTable;
    private TableView<Product> productsTable;

    private BorderPane contentPane;
    private Button posNavButton;
    private Button productsNavButton;
    private Button categoriesNavButton;
    private Button usersNavButton;
    private Button cashNavButton;
    private Button reportsNavButton;
    private String currentView = "pos";

    private Sale currentSale;
    private DiningTable selectedTable;

    public TpvDashboard(
        CatalogService catalogService,
        TableService tableService,
        CashService cashService,
        SaleService saleService,
        ReportService reportService
    ) {
        this.catalogService = catalogService;
        this.tableService = tableService;
        this.cashService = cashService;
        this.saleService = saleService;
        this.reportService = reportService;

        setTop(buildHeader());
        setCenter(buildMainLayout());
        setPadding(new Insets(10));

        reloadMasters();
        refreshCashStatus();
        applyRolePermissions();
    }

    private VBox buildHeader() {
        Label title = new Label("TPV26 | Barra y Comedor");
        title.getStyleClass().add("header-title");

        tableSelectorButton = new Button();
        tableSelectorButton.getStyleClass().add("action-primary");
        tableSelectorButton.setOnAction(e -> openTableSelector());
        refreshTableSelectorButton();

        currentUserCombo = new ComboBox<>(FXCollections.observableArrayList(cashService.users()));
        if (!currentUserCombo.getItems().isEmpty()) {
            currentUserCombo.getSelectionModel().select(0);
        }
        currentUserCombo.setOnAction(e -> applyRolePermissions());

        currentRoleLabel = new Label();

        HBox row = new HBox(12,
            title,
            tableSelectorButton,
            new Region(),
            new Label("Usuario activo:"),
            currentUserCombo,
            currentRoleLabel
        );
        HBox.setHgrow(row.getChildren().get(2), Priority.ALWAYS);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("header");

        return new VBox(row);
    }

    private HBox buildMainLayout() {
        VBox sidebar = buildSidebar();
        contentPane = new BorderPane();
        contentPane.getStyleClass().add("content-pane");

        HBox layout = new HBox(10, sidebar, contentPane);
        HBox.setHgrow(contentPane, Priority.ALWAYS);
        switchView("pos");
        return layout;
    }

    private VBox buildSidebar() {
        Label menuTitle = new Label("MENU");
        menuTitle.getStyleClass().add("menu-title");

        posNavButton = createNavButton("TPV", "pos");
        productsNavButton = createNavButton("Productos", "products");
        categoriesNavButton = createNavButton("Categorias", "categories");
        usersNavButton = createNavButton("Usuarios", "users");
        cashNavButton = createNavButton("Caja", "cash");
        reportsNavButton = createNavButton("Reportes", "reports");

        VBox sidebar = new VBox(4,
            menuTitle,
            posNavButton,
            productsNavButton,
            categoriesNavButton,
            usersNavButton,
            cashNavButton,
            reportsNavButton
        );
        sidebar.getStyleClass().add("nav-sidebar");
        sidebar.setPrefWidth(Region.USE_COMPUTED_SIZE);
        sidebar.setMinWidth(Region.USE_PREF_SIZE);
        sidebar.setMaxWidth(Region.USE_PREF_SIZE);
        return sidebar;
    }

    private Button createNavButton(String text, String view) {
        Button button = new Button(text);
        button.getStyleClass().add("nav-btn");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(e -> switchView(view));
        return button;
    }

    private void switchView(String view) {
        currentView = view;
        Node node = switch (view) {
            case "products" -> buildProductsAdminView();
            case "categories" -> buildCategoriesAdminView();
            case "users" -> buildUsersAdminView();
            case "cash" -> buildCajaView();
            case "reports" -> buildInformesView();
            default -> buildVentaView();
        };
        contentPane.setCenter(node);
        updateSidebarSelection();
    }

    private void updateSidebarSelection() {
        posNavButton.getStyleClass().remove("nav-btn-active");
        productsNavButton.getStyleClass().remove("nav-btn-active");
        categoriesNavButton.getStyleClass().remove("nav-btn-active");
        usersNavButton.getStyleClass().remove("nav-btn-active");
        cashNavButton.getStyleClass().remove("nav-btn-active");
        reportsNavButton.getStyleClass().remove("nav-btn-active");

        switch (currentView) {
            case "products" -> productsNavButton.getStyleClass().add("nav-btn-active");
            case "categories" -> categoriesNavButton.getStyleClass().add("nav-btn-active");
            case "users" -> usersNavButton.getStyleClass().add("nav-btn-active");
            case "cash" -> cashNavButton.getStyleClass().add("nav-btn-active");
            case "reports" -> reportsNavButton.getStyleClass().add("nav-btn-active");
            default -> posNavButton.getStyleClass().add("nav-btn-active");
        }
    }

    private BorderPane buildVentaView() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(12));
        root.getStyleClass().add("venta-root");

        Button barSaleButton = new Button("Venta en barra");
        barSaleButton.getStyleClass().add("action-primary");
        barSaleButton.setOnAction(e -> {
            try {
                currentSale = saleService.startBarSale();
                selectedTable = null;
                refreshCurrentSale();
            } catch (Exception ex) {
                warning(ex.getMessage());
            }
        });

        /*
        Label tablesTitle = new Label("1) ZONA MESA / BARRA");
        tablesTitle.getStyleClass().add("section-title");
        */
        HBox selectorActions = new HBox(8, barSaleButton);

        /*
        VBox zoneTablesBar = new VBox(10,
            tablesTitle,
            selectorActions
        );

        zoneTablesBar.getStyleClass().add("zone-card");
        */
        groupPane.setPrefWrapLength(1040);
        Label groupsTitle = new Label("2) ZONA GRUPOS DE PRODUCTOS");
        groupsTitle.getStyleClass().add("section-title");
        VBox zoneGroups = new VBox(10,
            groupsTitle,
            wrap(groupPane, 124)
        );
        zoneGroups.getStyleClass().add("zone-card");

        productPane.setPrefWrapLength(1040);
        Label productsTitle = new Label("3) ZONA PRODUCTOS");
        productsTitle.getStyleClass().add("section-title");
        VBox zoneProducts = new VBox(10,
            productsTitle,
            wrap(productPane, 620)
        );
        zoneProducts.getStyleClass().add("zone-card");
        VBox.setVgrow(zoneProducts, Priority.ALWAYS);

        // VBox left = new VBox(10, zoneTablesBar, zoneGroups, zoneProducts);
        VBox left = new VBox(10, zoneGroups, zoneProducts);
        left.setPrefWidth(1080);
        VBox.setVgrow(left, Priority.ALWAYS);

        currentSaleLabel = new Label("Venta actual: ninguna");
        currentSaleLabel.getStyleClass().add("sale-context");
        totalsLabel = new Label("Base: 0.00 | IVA: 0.00 | Total: 0.00");
        totalsLabel.getStyleClass().add("total");

        linesTable = new TableView<>(currentLines);
        TableColumn<SaleLine, String> pCol = new TableColumn<>("Producto");
        pCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().product().name()));
        TableColumn<SaleLine, String> qCol = new TableColumn<>("Cantidad");
        qCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().quantity().toPlainString()));
        TableColumn<SaleLine, String> tCol = new TableColumn<>("Importe total linea (IVA incl.)");
        tCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().lineTotalWithVat().toPlainString()));
        linesTable.getColumns().setAll(List.of(pCol, qCol, tCol));
        linesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        Button editLineButton = new Button("Editar linea");
        editLineButton.getStyleClass().add("action-secondary");
        editLineButton.setOnAction(e -> editLine());

        Button removeLineButton = new Button("Eliminar linea");
        removeLineButton.getStyleClass().add("action-secondary");
        removeLineButton.setOnAction(e -> {
            SaleLine selected = linesTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                warning("Seleccione una linea");
                return;
            }
            saleService.removeLine(selected.id());
            refreshCurrentSale();
        });

        Button holdButton = new Button("En espera");
        holdButton.getStyleClass().add("action-secondary");
        holdButton.setOnAction(e -> {
            if (currentSale == null) {
                warning("No hay venta activa");
                return;
            }
            try {
                saleService.hold(currentSale);
                currentSale = null;
                selectedTable = null;
                refreshCurrentSale();
                reloadTables();
            } catch (Exception ex) {
                warning(ex.getMessage());
            }
        });

        Button cancelButton = new Button("Cancelar");
        cancelButton.getStyleClass().add("action-secondary");
        cancelButton.setOnAction(e -> {
            if (currentSale == null) {
                warning("No hay venta activa");
                return;
            }
            saleService.cancel(currentSale);
            currentSale = null;
            selectedTable = null;
            refreshCurrentSale();
            reloadTables();
        });

        paymentMethodCombo = new ComboBox<>(FXCollections.observableArrayList(PaymentMethod.values()));
        paymentMethodCombo.getSelectionModel().select(PaymentMethod.EFECTIVO);

        Button chargeButton = new Button("Cobrar");
        chargeButton.getStyleClass().add("action-primary");
        chargeButton.setOnAction(e -> {
            if (currentSale == null) {
                warning("No hay venta activa");
                return;
            }
            try {
                saleService.charge(currentSale, paymentMethodCombo.getSelectionModel().getSelectedItem());
                currentSale = null;
                selectedTable = null;
                refreshCurrentSale();
                reloadTables();
            } catch (Exception ex) {
                warning(ex.getMessage());
            }
        });

        HBox actions = new HBox(8,
            editLineButton,
            removeLineButton,
            holdButton,
            cancelButton,
            new Label("Pago:"),
            paymentMethodCombo,
            chargeButton
        );
        actions.getStyleClass().add("sale-actions");

        Label saleTitle = new Label("4) ZONA VENTA ACTUAL");
        saleTitle.getStyleClass().add("section-title");
        VBox zoneSale = new VBox(10,
            saleTitle,
            currentSaleLabel,
            linesTable,
            actions,
            totalsLabel
        );
        zoneSale.getStyleClass().add("cart-panel");
        zoneSale.setPrefWidth(460);
        VBox.setVgrow(linesTable, Priority.ALWAYS);

        HBox body = new HBox(10, left, zoneSale);
        HBox.setHgrow(left, Priority.ALWAYS);
        root.setCenter(body);
        return root;
    }

    private VBox buildProductsAdminView() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(12));
        root.getChildren().add(buildProductosGestion());
        return root;
    }

    private VBox buildCategoriesAdminView() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(12));

        VBox mesasCard = buildMesasGestion();
        VBox gruposCard = buildGruposGestion();

        root.getChildren().addAll(mesasCard, gruposCard);
        return root;
    }

    private VBox buildUsersAdminView() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(12));
        root.getChildren().add(buildUsuariosGestion());
        return root;
    }

    private VBox buildMesasGestion() {
        TextField tableName = new TextField();
        tableName.setPromptText("Nombre mesa");
        TextField row = new TextField();
        row.setPromptText("Fila");
        TextField col = new TextField();
        col.setPromptText("Columna");

        Button create = new Button("Crear mesa");
        create.getStyleClass().add("secondary-btn");
        create.setOnAction(e -> {
            try {
                tableService.createTable(tableName.getText(), Integer.parseInt(row.getText()), Integer.parseInt(col.getText()));
                tableName.clear();
                row.clear();
                col.clear();
                reloadTables();
            } catch (Exception ex) {
                warning(ex.getMessage());
            }
        });

        Button move = new Button("Mover mesa seleccionada");
        move.getStyleClass().add("secondary-btn");
        move.setOnAction(e -> moveSelectedTable());

        VBox card = new VBox(8,
            new Label("Mesas"),
            new HBox(8, tableName, row, col, create, move)
        );
        card.getStyleClass().add("card");
        return card;
    }

    private VBox buildGruposGestion() {
        groupsTable = new TableView<>(allGroups);
        TableColumn<ProductGroup, String> nameCol = new TableColumn<>("Grupo");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().name()));
        TableColumn<ProductGroup, String> ordCol = new TableColumn<>("Orden");
        ordCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().displayOrder())));
        TableColumn<ProductGroup, String> actCol = new TableColumn<>("Activo");
        actCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().active() ? "SI" : "NO"));
        groupsTable.getColumns().setAll(List.of(nameCol, ordCol, actCol));
        groupsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TextField groupName = new TextField();
        groupName.setPromptText("Nombre grupo");
        TextField groupOrder = new TextField();
        groupOrder.setPromptText("Orden");

        Button add = new Button("Crear grupo");
        add.getStyleClass().add("secondary-btn");
        add.setOnAction(e -> {
            try {
                catalogService.createGroup(groupName.getText(), Integer.parseInt(groupOrder.getText()));
                groupName.clear();
                groupOrder.clear();
                reloadGroups();
            } catch (Exception ex) {
                warning(ex.getMessage());
            }
        });

        Button toggle = new Button("Activar/Desactivar grupo");
        toggle.getStyleClass().add("secondary-btn");
        toggle.setOnAction(e -> {
            ProductGroup selected = groupsTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                warning("Seleccione un grupo");
                return;
            }
            catalogService.setGroupActive(selected.id(), !selected.active());
            reloadGroups();
        });

        VBox card = new VBox(8,
            new Label("Grupos de productos"),
            groupsTable,
            new HBox(8, groupName, groupOrder, add, toggle)
        );
        card.getStyleClass().add("card");
        return card;
    }

    private VBox buildProductosGestion() {
        productsTable = new TableView<>(allProducts);
        TableColumn<Product, String> nameCol = new TableColumn<>("Producto");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().name()));
        TableColumn<Product, String> groupCol = new TableColumn<>("Grupo");
        groupCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().group().name()));
        TableColumn<Product, String> orderCol = new TableColumn<>("Orden");
        orderCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().displayOrder())));
        TableColumn<Product, String> activeCol = new TableColumn<>("Activo");
        activeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().active() ? "SI" : "NO"));
        productsTable.getColumns().setAll(List.of(nameCol, groupCol, orderCol, activeCol));
        productsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        ComboBox<ProductGroup> groupCombo = new ComboBox<>(allGroups);
        TextField name = new TextField();
        name.setPromptText("Nombre producto");
        TextField price = new TextField();
        price.setPromptText("Precio IVA incl.");
        TextField order = new TextField();
        order.setPromptText("Orden");
        ComboBox<IvaType> ivaCombo = new ComboBox<>(FXCollections.observableArrayList(IvaType.values()));
        ivaCombo.getSelectionModel().select(IvaType.REDUCIDO_10);

        Button add = new Button("Crear producto");
        add.getStyleClass().add("secondary-btn");
        add.setOnAction(e -> {
            try {
                ProductGroup group = groupCombo.getSelectionModel().getSelectedItem();
                if (group == null) {
                    warning("Seleccione grupo");
                    return;
                }
                catalogService.createProduct(
                    name.getText(),
                    group.id(),
                    new BigDecimal(price.getText()),
                    ivaCombo.getSelectionModel().getSelectedItem(),
                    Integer.parseInt(order.getText())
                );
                name.clear();
                price.clear();
                order.clear();
                reloadGroups();
            } catch (Exception ex) {
                warning(ex.getMessage());
            }
        });

        Button toggle = new Button("Activar/Desactivar producto");
        toggle.getStyleClass().add("secondary-btn");
        toggle.setOnAction(e -> {
            Product selected = productsTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                warning("Seleccione un producto");
                return;
            }
            catalogService.setProductActive(selected.id(), !selected.active());
            reloadGroups();
        });

        VBox card = new VBox(8,
            new Label("Productos"),
            productsTable,
            new HBox(8, groupCombo, name, price, order, ivaCombo, add, toggle)
        );
        card.getStyleClass().add("card");
        return card;
    }

    private VBox buildUsuariosGestion() {
        TextField username = new TextField();
        username.setPromptText("Usuario");
        TextField password = new TextField();
        password.setPromptText("Password");
        ComboBox<UserRole> roleCombo = new ComboBox<>(FXCollections.observableArrayList(UserRole.values()));
        roleCombo.getSelectionModel().select(UserRole.EMPLEADO);

        Button create = new Button("Crear usuario");
        create.getStyleClass().add("secondary-btn");
        create.setOnAction(e -> {
            try {
                cashService.createUser(username.getText(), password.getText(), roleCombo.getSelectionModel().getSelectedItem());
                username.clear();
                password.clear();
                currentUserCombo.setItems(FXCollections.observableArrayList(cashService.users()));
                if (!currentUserCombo.getItems().isEmpty()) {
                    currentUserCombo.getSelectionModel().select(0);
                }
                applyRolePermissions();
            } catch (Exception ex) {
                warning(ex.getMessage());
            }
        });

        VBox card = new VBox(8,
            new Label("Usuarios"),
            new HBox(8, username, password, roleCombo, create)
        );
        card.getStyleClass().add("card");
        return card;
    }

    private VBox buildCajaView() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(12));

        openingAmountField = new TextField("0.00");

        Button openButton = new Button("Abrir caja");
        openButton.getStyleClass().add("big-btn");
        openButton.setOnAction(e -> {
            User user = currentUserCombo.getSelectionModel().getSelectedItem();
            if (user == null) {
                warning("Seleccione usuario activo");
                return;
            }
            try {
                cashService.openCash(user.id(), new BigDecimal(openingAmountField.getText()));
                refreshCashStatus();
            } catch (Exception ex) {
                warning(ex.getMessage());
            }
        });

        Button closeButton = new Button("Cerrar caja");
        closeButton.getStyleClass().add("big-btn");
        closeButton.setOnAction(e -> {
            try {
                CashCloseSummary summary = cashService.closeSummary();
                info("""
                    Cierre de caja
                    Apertura: %.2f EUR
                    Efectivo: %.2f EUR
                    Tarjeta: %.2f EUR
                    Bizum: %.2f EUR
                    Total vendido: %.2f EUR
                    """.formatted(
                    summary.openingAmount(),
                    summary.paymentTotals().get(PaymentMethod.EFECTIVO),
                    summary.paymentTotals().get(PaymentMethod.TARJETA),
                    summary.paymentTotals().get(PaymentMethod.BIZUM),
                    summary.totalSales()
                ));
                cashService.closeCash();
                refreshCashStatus();
            } catch (Exception ex) {
                warning(ex.getMessage());
            }
        });

        cashStatusLabel = new Label("Caja cerrada");
        cashStatusLabel.getStyleClass().add("total");

        VBox card = new VBox(10,
            new HBox(8, new Label("Importe apertura"), openingAmountField, openButton, closeButton),
            cashStatusLabel
        );
        card.getStyleClass().add("card");

        root.getChildren().add(card);
        return root;
    }

    private VBox buildInformesView() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(12));

        fromPicker = new DatePicker(LocalDate.now().minusDays(7));
        toPicker = new DatePicker(LocalDate.now());

        Button daily = new Button("Diario");
        daily.getStyleClass().add("secondary-btn");
        daily.setOnAction(e -> {
            LocalDate today = LocalDate.now();
            fromPicker.setValue(today);
            toPicker.setValue(today);
            generateReport();
        });

        Button weekly = new Button("Semanal");
        weekly.getStyleClass().add("secondary-btn");
        weekly.setOnAction(e -> {
            LocalDate today = LocalDate.now();
            fromPicker.setValue(today.minusDays(6));
            toPicker.setValue(today);
            generateReport();
        });

        Button monthly = new Button("Mensual");
        monthly.getStyleClass().add("secondary-btn");
        monthly.setOnAction(e -> {
            LocalDate today = LocalDate.now();
            fromPicker.setValue(today.minusMonths(1).plusDays(1));
            toPicker.setValue(today);
            generateReport();
        });

        Button quarterly = new Button("Trimestral");
        quarterly.getStyleClass().add("secondary-btn");
        quarterly.setOnAction(e -> {
            LocalDate today = LocalDate.now();
            fromPicker.setValue(today.minusMonths(3).plusDays(1));
            toPicker.setValue(today);
            generateReport();
        });

        Button generate = new Button("Generar");
        generate.getStyleClass().add("big-btn");
        generate.setOnAction(e -> generateReport());

        reportArea = new TextArea();
        reportArea.setEditable(false);
        reportArea.setPrefRowCount(18);
        VBox.setVgrow(reportArea, Priority.ALWAYS);

        VBox card = new VBox(10,
            new HBox(8, new Label("Desde"), fromPicker, new Label("Hasta"), toPicker, daily, weekly, monthly, quarterly, generate),
            reportArea
        );
        card.getStyleClass().add("card");

        root.getChildren().add(card);
        return root;
    }

    private ScrollPane wrap(FlowPane pane, double height) {
        ScrollPane sp = new ScrollPane(pane);
        sp.setFitToWidth(true);
        sp.setPrefHeight(height);
        return sp;
    }

    private void reloadMasters() {
        reloadGroups();
        reloadTables();
    }

    private void reloadGroups() {
        groups.setAll(catalogService.groups());
        allGroups.setAll(catalogService.allGroups());
        allProducts.setAll(catalogService.allProducts());

        groupPane.getChildren().clear();
        for (ProductGroup group : groups) {
            Button b = new Button(group.name());
            b.getStyleClass().add("group-btn");
            b.setOnAction(e -> {
                currentGroupProducts.setAll(catalogService.productsByGroup(group.id()));
                renderProducts();
            });
            groupPane.getChildren().add(b);
        }

        if (!groups.isEmpty()) {
            currentGroupProducts.setAll(catalogService.productsByGroup(groups.get(0).id()));
            renderProducts();
        } else {
            productPane.getChildren().clear();
        }

        if (groupsTable != null) {
            groupsTable.setItems(allGroups);
        }
        if (productsTable != null) {
            productsTable.setItems(allProducts);
        }
    }

    private void renderProducts() {
        productPane.getChildren().clear();
        for (Product product : currentGroupProducts) {
            Button b = new Button(product.name() + "\n" + product.defaultPriceWithVat() + " EUR");
            b.setMinWidth(180);
            b.setMinHeight(86);
            b.getStyleClass().add("product-btn");
            b.setOnAction(e -> {
                if (currentSale == null) {
                    warning("Seleccione mesa o barra para empezar venta");
                    return;
                }
                try {
                    saleService.addProduct(currentSale.id(), product);
                    refreshCurrentSale();
                } catch (Exception ex) {
                    warning(ex.getMessage());
                }
            });
            productPane.getChildren().add(b);
        }
    }

    private void reloadTables() {
        diningTables.setAll(tableService.tables());
        if (selectedTable != null) {
            selectedTable = diningTables.stream()
                .filter(t -> t.id().equals(selectedTable.id()))
                .findFirst()
                .orElse(null);
        }
        refreshTableSelectorButton();
    }

    private void refreshCurrentSale() {
        if (currentSale == null) {
            currentSaleLabel.setText("Venta actual: ninguna");
            currentLines.clear();
            totalsLabel.setText("Base: 0.00 | IVA: 0.00 | Total: 0.00");
            refreshTableSelectorButton();
            return;
        }

        currentSaleLabel.setText(selectedTable == null ? "Venta actual: Barra" : "Venta actual: " + selectedTable.name());
        currentLines.setAll(saleService.lines(currentSale.id()));
        SaleTotals totals = saleService.totals(currentSale.id());
        totalsLabel.setText("Base: " + totals.base() + " | IVA: " + totals.vat() + " | Total: " + totals.total());
        refreshTableSelectorButton();
    }

    private void openTableSelector() {
        if (diningTables.isEmpty()) {
            warning("No hay mesas creadas");
            return;
        }

        int minRow = diningTables.stream().mapToInt(DiningTable::rowIndex).min().orElse(0);
        int maxRow = diningTables.stream().mapToInt(DiningTable::rowIndex).max().orElse(0);
        int minCol = diningTables.stream().mapToInt(DiningTable::colIndex).min().orElse(0);
        int maxCol = diningTables.stream().mapToInt(DiningTable::colIndex).max().orElse(0);

        Map<String, DiningTable> tablesByCell = diningTables.stream()
            .collect(Collectors.toMap(
                table -> table.rowIndex() + ":" + table.colIndex(),
                table -> table,
                (first, ignored) -> first
            ));

        GridPane grid = new GridPane();
        grid.getStyleClass().add("table-selector-grid");

        Dialog<DiningTable> dialog = new Dialog<>();
        dialog.setTitle("Seleccion de mesa");
        dialog.setHeaderText("Pulsa una mesa para empezar o continuar la venta");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);

        if (getScene() != null) {
            dialog.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
        }

        for (int row = minRow; row <= maxRow; row++) {
            for (int col = minCol; col <= maxCol; col++) {
                DiningTable table = tablesByCell.get(row + ":" + col);
                if (table == null) {
                    Region emptyCell = new Region();
                    emptyCell.getStyleClass().add("table-empty-cell");
                    emptyCell.setMinSize(130, 130);
                    grid.add(emptyCell, col - minCol, row - minRow);
                    continue;
                }

                String statusText = table.status() == TableStatus.OCUPADA ? "Ocupada" : "Libre";
                Button tableButton = new Button(table.name() + "\n" + statusText);
                tableButton.setWrapText(true);
                tableButton.setMinSize(130, 130);
                tableButton.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                tableButton.getStyleClass().addAll("table-selector-btn", "table-btn");
                tableButton.getStyleClass().add(table.status() == TableStatus.OCUPADA ? "table-busy" : "table-free");
                if (selectedTable != null && selectedTable.id().equals(table.id())) {
                    tableButton.getStyleClass().add("table-selector-btn-selected");
                }

                tableButton.setOnAction(e -> {
                    dialog.setResult(table);
                    dialog.close();
                });
                grid.add(tableButton, col - minCol, row - minRow);
            }
        }

        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportWidth(760);
        scrollPane.setPrefViewportHeight(560);
        dialog.getDialogPane().setContent(scrollPane);

        Optional<DiningTable> choice = dialog.showAndWait();
        if (choice.isEmpty()) {
            return;
        }

        try {
            selectedTable = choice.get();
            currentSale = saleService.loadOrStartTableSale(selectedTable.id());
            refreshCurrentSale();
            reloadTables();
        } catch (Exception ex) {
            warning(ex.getMessage());
        }
    }

    private void refreshTableSelectorButton() {
        if (tableSelectorButton == null) {
            return;
        }
        tableSelectorButton.setText(
            selectedTable == null ? "Seleccionar mesa" : "Mesa: " + selectedTable.name()
        );
    }

    private void refreshCashStatus() {
        if (cashStatusLabel == null) {
            return;
        }
        Optional<CashSession> openCash = cashService.openCashSession();
        if (openCash.isPresent()) {
            CashSession cash = openCash.get();
            cashStatusLabel.setText("Caja abierta | Sesion " + cash.id() + " | Apertura " + cash.openingAmount() + " EUR");
        } else {
            cashStatusLabel.setText("Caja cerrada");
        }
    }

    private void applyRolePermissions() {
        User user = currentUserCombo.getSelectionModel().getSelectedItem();
        if (user == null) {
            currentRoleLabel.setText("Sin usuario");
            productsNavButton.setDisable(true);
            categoriesNavButton.setDisable(true);
            usersNavButton.setDisable(true);
            reportsNavButton.setDisable(true);
            if (!"pos".equals(currentView) && !"cash".equals(currentView)) {
                switchView("pos");
            }
            return;
        }

        currentRoleLabel.setText("Rol: " + user.role());
        boolean isAdmin = user.role() == UserRole.ADMINISTRADOR;
        productsNavButton.setDisable(!isAdmin);
        categoriesNavButton.setDisable(!isAdmin);
        usersNavButton.setDisable(!isAdmin);
        reportsNavButton.setDisable(!isAdmin);
        if (!isAdmin && ("products".equals(currentView)
            || "categories".equals(currentView)
            || "users".equals(currentView)
            || "reports".equals(currentView))) {
            switchView("pos");
        }
    }

    private void moveSelectedTable() {
        if (selectedTable == null) {
            warning("Seleccione una mesa desde Punto de venta");
            return;
        }

        TextInputDialog rowDialog = new TextInputDialog(String.valueOf(selectedTable.rowIndex()));
        rowDialog.setTitle("Mover mesa");
        rowDialog.setHeaderText("Nueva fila para " + selectedTable.name());
        Optional<String> row = rowDialog.showAndWait();
        if (row.isEmpty()) {
            return;
        }

        TextInputDialog colDialog = new TextInputDialog(String.valueOf(selectedTable.colIndex()));
        colDialog.setTitle("Mover mesa");
        colDialog.setHeaderText("Nueva columna para " + selectedTable.name());
        Optional<String> col = colDialog.showAndWait();
        if (col.isEmpty()) {
            return;
        }

        try {
            tableService.moveTable(selectedTable.id(), Integer.parseInt(row.get()), Integer.parseInt(col.get()));
            reloadTables();
        } catch (Exception ex) {
            warning(ex.getMessage());
        }
    }

    private void editLine() {
        SaleLine selected = linesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            warning("Seleccione una linea");
            return;
        }

        TextInputDialog qtyDialog = new TextInputDialog(selected.quantity().toPlainString());
        qtyDialog.setTitle("Editar cantidad");
        qtyDialog.setHeaderText("Cantidad para " + selected.product().name());
        Optional<String> qty = qtyDialog.showAndWait();
        if (qty.isEmpty()) {
            return;
        }

        TextInputDialog totalDialog = new TextInputDialog(selected.lineTotalWithVat().toPlainString());
        totalDialog.setTitle("Editar importe");
        totalDialog.setHeaderText("Importe total con IVA para " + selected.product().name());
        Optional<String> total = totalDialog.showAndWait();
        if (total.isEmpty()) {
            return;
        }

        try {
            saleService.updateLine(selected.id(), new BigDecimal(qty.get()), new BigDecimal(total.get()));
            refreshCurrentSale();
        } catch (Exception ex) {
            warning(ex.getMessage());
        }
    }

    private void generateReport() {
        LocalDate from = fromPicker.getValue();
        LocalDate to = toPicker.getValue();
        if (from == null || to == null || from.isAfter(to)) {
            warning("Rango invalido");
            return;
        }
        reportArea.setText(reportService.report(from, to));
    }

    private void warning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Aviso");
        alert.setHeaderText("TPV26");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void info(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Informacion");
        alert.setHeaderText("TPV26");
        alert.setContentText(message);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.showAndWait();
    }
}
