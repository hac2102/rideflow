package com.rideflow.desktop.controller;

import com.rideflow.core.enums.DriverStatus;
import com.rideflow.core.enums.RequestType;
import com.rideflow.core.enums.VehicleType;
import com.rideflow.desktop.client.BackendApiClient;
import com.rideflow.desktop.view.CityCanvasView;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final BackendApiClient apiClient;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // UI Root and Components
    private final BorderPane rootPane = new BorderPane();
    private CityCanvasView cityCanvas;

    // Overview KPIs
    private final Label lblFleetTotal = new Label("0");
    private final Label lblAvailable = new Label("0");
    private final Label lblBusy = new Label("0");
    private final Label lblQueued = new Label("0");
    private final Label lblActive = new Label("0");
    private final Label lblCompleted = new Label("0");
    private final Label lblRevenue = new Label("₹0.00");
    private final Label lblAvgSurge = new Label("1.00x");
    private final Label lblSimClock = new Label("Tick 0 | 08:00 AM");
    private final Label lblSimStatus = new Label("READY");

    // Event Feed
    private final ObservableList<String> eventFeedList = FXCollections.observableArrayList();
    private final ListView<String> eventFeedView = new ListView<>(eventFeedList);

    // Dispatch Queue Table
    private final ObservableList<Map<String, Object>> queuedRidesList = FXCollections.observableArrayList();
    private final TableView<Map<String, Object>> queuedTable = new TableView<>(queuedRidesList);
    private final TextArea txtDispatchExplanation = new TextArea();

    // Driver Management Table
    private final ObservableList<Map<String, Object>> driverList = FXCollections.observableArrayList();
    private final TableView<Map<String, Object>> driverTable = new TableView<>(driverList);

    // Heatmap Grid
    private final GridPane heatmapGrid = new GridPane();

    // Inspector
    private final Label lblInspector = new Label("Click an entity on the canvas to inspect its details.");

    public DashboardController(BackendApiClient apiClient) {
        this.apiClient = apiClient;
        buildUi();
        startPolling();
    }

    public BorderPane getRootPane() {
        return rootPane;
    }

    private void buildUi() {
        try {
            rootPane.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        } catch (Exception ex) {
            log.warn("Could not load styles.css: {}", ex.getMessage());
        }
        rootPane.setStyle("-fx-background-color: #0b0c10;");

        // Top Status Bar
        HBox topBar = createTopBar();
        rootPane.setTop(topBar);

        // Main Tab Pane with the 6 Screens
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getTabs().addAll(
                new Tab("1. Operations Overview", createOverviewScreen()),
                new Tab("2. City Simulator Canvas", createCitySimulatorScreen()),
                new Tab("3. Dispatch Center", createDispatchScreen()),
                new Tab("4. Surge Pricing Monitor", createHeatmapScreen()),
                new Tab("5. Fleet Management", createDriverManagementScreen()),
                new Tab("6. Simulation & Analytics", createSimulationScreen())
        );

        rootPane.setCenter(tabPane);
    }

    private HBox createTopBar() {
        HBox bar = new HBox(16);
        bar.setPadding(new Insets(12, 20, 12, 20));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: #12141e; -fx-border-color: #232738; -fx-border-width: 0 0 1 0;");

        Label brandIcon = new Label("⚡");
        brandIcon.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        brandIcon.setTextFill(Color.web("#00f59b"));

        Label title = new Label("RideFlow");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 19));
        title.setTextFill(Color.WHITE);

        Label badge = new Label("ENTERPRISE OPS");
        badge.setStyle("-fx-background-color: rgba(0, 245, 155, 0.12); -fx-text-fill: #00f59b; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-background-radius: 4; -fx-border-color: rgba(0, 245, 155, 0.3); -fx-border-radius: 4;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox liveIndicator = new HBox(6);
        liveIndicator.setAlignment(Pos.CENTER_LEFT);
        Label dot = new Label("●");
        dot.setTextFill(Color.web("#00f59b"));
        dot.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        Label liveText = new Label("ENGINE ONLINE");
        liveText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        liveText.setTextFill(Color.web("#a0aec0"));
        liveIndicator.getChildren().addAll(dot, liveText);

        HBox clockBox = new HBox(8);
        clockBox.setAlignment(Pos.CENTER_LEFT);
        clockBox.setStyle("-fx-background-color: #191c2b; -fx-padding: 4 12 4 12; -fx-background-radius: 6; -fx-border-color: #2d3347; -fx-border-radius: 6;");
        Label clockIcon = new Label("⏱");
        clockIcon.setTextFill(Color.web("#00a8ff"));
        lblSimClock.setFont(Font.font("Consolas", FontWeight.BOLD, 13));
        lblSimClock.setTextFill(Color.web("#e0e6ed"));
        clockBox.getChildren().addAll(clockIcon, lblSimClock);

        lblSimStatus.setStyle("-fx-background-color: #1f2733; -fx-padding: 4 12 4 12; -fx-background-radius: 6; -fx-border-color: #3b4b60; -fx-border-radius: 6; -fx-font-weight: bold; -fx-font-size: 11px;");
        lblSimStatus.setTextFill(Color.web("#ffaa00"));

        bar.getChildren().addAll(brandIcon, title, badge, spacer, liveIndicator, clockBox, lblSimStatus);
        return bar;
    }

    // Screen 1: Operations Overview
    private Node createOverviewScreen() {
        VBox v = new VBox(18);
        v.setPadding(new Insets(18));

        // KPI Cards Row
        HBox kpiRow = new HBox(12);
        kpiRow.getChildren().addAll(
                createKpiCard("TOTAL DRIVERS", lblFleetTotal, "#00a8ff"),
                createKpiCard("AVAILABLE", lblAvailable, "#00f59b"),
                createKpiCard("BUSY ON TRIPS", lblBusy, "#ffaa00"),
                createKpiCard("QUEUED RIDES", lblQueued, "#ff4757"),
                createKpiCard("ACTIVE TRIPS", lblActive, "#00c8ff"),
                createKpiCard("COMPLETED", lblCompleted, "#a55eea"),
                createKpiCard("SIM REVENUE", lblRevenue, "#00f59b"),
                createKpiCard("AVG SURGE", lblAvgSurge, "#ff7675")
        );

        // Event Feed Section
        VBox eventBox = new VBox(10);
        Label lblEventsTitle = new Label("Live Simulation Event Feed (Real-Time Telemetry)");
        lblEventsTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lblEventsTitle.setTextFill(Color.WHITE);

        VBox.setVgrow(eventFeedView, Priority.ALWAYS);
        eventBox.getChildren().addAll(lblEventsTitle, eventFeedView);
        VBox.setVgrow(eventBox, Priority.ALWAYS);

        v.getChildren().addAll(kpiRow, eventBox);
        return v;
    }

    // Screen 2: Interactive City Simulator
    private Node createCitySimulatorScreen() {
        HBox h = new HBox(16);
        h.setPadding(new Insets(16));

        cityCanvas = new CityCanvasView(620, 620);
        cityCanvas.setSelectionListener(this::onEntitySelected);

        VBox sidePanel = new VBox(14);
        sidePanel.setPrefWidth(320);

        Label lblLegendTitle = new Label("Visual Topology Legend");
        lblLegendTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lblLegendTitle.setTextFill(Color.WHITE);

        VBox legendBox = new VBox(8);
        legendBox.setPadding(new Insets(12));
        legendBox.getStyleClass().add("kpi-card");
        legendBox.getChildren().addAll(
                createLegendItem("● Green Dot", "Available Driver (Ready for dispatch)", "#00f59b"),
                createLegendItem("● Orange Dot", "Reserved Driver (En route to pickup)", "#ffaa00"),
                createLegendItem("● Blue Dot", "On-Trip Driver (Passenger on board)", "#00a8ff"),
                createLegendItem("● Gray Dot", "Offline Driver (Off-duty)", "#718096"),
                createLegendItem("◆ Amber Diamond", "Queued Ride Request (Awaiting match)", "#ffcc00"),
                createLegendItem("— Cyan Glow Vector", "Active Journey Route Vector", "#00c8ff"),
                createLegendItem("■ Red / Coral Tint", "Dynamic Surge Zone (High Demand)", "#ff4757")
        );

        Label lblInspTitle = new Label("Selected Entity Details");
        lblInspTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lblInspTitle.setTextFill(Color.WHITE);

        lblInspector.setWrapText(true);
        lblInspector.setPadding(new Insets(12));
        lblInspector.setStyle("-fx-background-color: #12141f; -fx-text-fill: #00f59b; -fx-font-family: 'Consolas', monospace; -fx-border-color: #232738; -fx-border-radius: 8; -fx-background-radius: 8;");
        lblInspector.setPrefHeight(220);

        sidePanel.getChildren().addAll(lblLegendTitle, legendBox, lblInspTitle, lblInspector);
        h.getChildren().addAll(cityCanvas, sidePanel);
        return h;
    }

    // Screen 3: Dispatch Control Center
    private Node createDispatchScreen() {
        VBox v = new VBox(14);
        v.setPadding(new Insets(16));

        HBox controls = new HBox(12);
        controls.setAlignment(Pos.CENTER_LEFT);

        Button btnCreateRide = new Button("+ Create Ride Request");
        btnCreateRide.getStyleClass().addAll("button", "btn-success");
        btnCreateRide.setOnAction(e -> showCreateRideDialog());

        Button btnDispatchNow = new Button("⚡ Trigger Instant Dispatch Pass");
        btnDispatchNow.getStyleClass().addAll("button", "btn-primary");
        btnDispatchNow.setOnAction(e -> apiClient.triggerTickAsync().thenAccept(res -> Platform.runLater(this::refreshData)));

        controls.getChildren().addAll(btnCreateRide, btnDispatchNow);

        setupQueuedTable();

        Label lblExplTitle = new Label("Algorithm Decision Explanation (Selected Queued Ride)");
        lblExplTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblExplTitle.setTextFill(Color.WHITE);

        txtDispatchExplanation.setEditable(false);
        txtDispatchExplanation.setPrefHeight(130);

        queuedTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && newV.containsKey("requestId")) {
                long rid = ((Number) newV.get("requestId")).longValue();
                apiClient.getCandidateEvaluationAsync(rid).thenAccept(scores -> Platform.runLater(() -> {
                    if (scores.isEmpty()) {
                        txtDispatchExplanation.setText("Ride #" + rid + ": Waiting in priority queue. No driver dispatched yet.");
                    } else {
                        StringBuilder sb = new StringBuilder("Evaluated Candidate Drivers for Ride #" + rid + ":\n");
                        for (Map<String, Object> s : scores) {
                            sb.append("• ").append(s.get("explanation")).append("\n");
                        }
                        txtDispatchExplanation.setText(sb.toString());
                    }
                }));
            }
        });

        v.getChildren().addAll(controls, queuedTable, lblExplTitle, txtDispatchExplanation);
        return v;
    }

    // Screen 4: Dynamic Surge Pricing Heatmap
    private Node createHeatmapScreen() {
        VBox v = new VBox(15);
        v.setPadding(new Insets(15));

        Label title = new Label("5x5 City Geographic Zones Surge & Supply-Demand Heatmap");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        title.setTextFill(Color.WHITE);

        heatmapGrid.setHgap(8);
        heatmapGrid.setVgap(8);
        heatmapGrid.setPadding(new Insets(10));

        v.getChildren().addAll(title, heatmapGrid);
        return v;
    }

    // Screen 5: Fleet & Driver Management
    private Node createDriverManagementScreen() {
        VBox v = new VBox(14);
        v.setPadding(new Insets(16));

        HBox topActions = new HBox(12);
        topActions.setAlignment(Pos.CENTER_LEFT);

        Button btnAddDriver = new Button("+ Register New Driver");
        btnAddDriver.getStyleClass().addAll("button", "btn-success");
        btnAddDriver.setOnAction(e -> showRegisterDriverDialog());

        Button btnToggleOnline = new Button("⚡ Toggle Online / Offline");
        btnToggleOnline.getStyleClass().addAll("button", "btn-warning");
        btnToggleOnline.setOnAction(e -> {
            Map<String, Object> sel = driverTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                long id = ((Number) sel.get("driverId")).longValue();
                String curr = (String) sel.get("status");
                String next = curr.equals("OFFLINE") ? "AVAILABLE" : "OFFLINE";
                apiClient.toggleDriverStatusAsync(id, next).thenAccept(r -> Platform.runLater(this::refreshData));
            }
        });

        topActions.getChildren().addAll(btnAddDriver, btnToggleOnline);

        setupDriverTable();

        v.getChildren().addAll(topActions, driverTable);
        return v;
    }

    // Screen 6: Simulation Controls & Analytics
    private Node createSimulationScreen() {
        VBox v = new VBox(16);
        v.setPadding(new Insets(16));

        Label title = new Label("Simulation Clock & Workload Controls");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        title.setTextFill(Color.WHITE);

        HBox controls = new HBox(12);
        controls.setAlignment(Pos.CENTER_LEFT);

        Button btnStart = new Button("▶ Start Continuous Simulation");
        btnStart.getStyleClass().addAll("button", "btn-success");
        btnStart.setOnAction(e -> apiClient.startSimulationAsync().thenAccept(r -> Platform.runLater(this::refreshData)));

        Button btnPause = new Button("⏸ Pause");
        btnPause.getStyleClass().addAll("button", "btn-warning");
        btnPause.setOnAction(e -> apiClient.pauseSimulationAsync().thenAccept(r -> Platform.runLater(this::refreshData)));

        Button btnStep = new Button("⏭ Step 1 Tick (1 Minute)");
        btnStep.getStyleClass().addAll("button", "btn-primary");
        btnStep.setOnAction(e -> apiClient.triggerTickAsync().thenAccept(r -> Platform.runLater(this::refreshData)));

        Label lblScen = new Label("Scenario:");
        lblScen.setTextFill(Color.web("#a0aec0"));
        lblScen.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));

        ComboBox<String> cbScenario = new ComboBox<>();
        cbScenario.getItems().addAll("NORMAL", "RUSH_HOUR", "LOW_SUPPLY", "HIGH_DEMAND", "BALANCED_MARKET");
        cbScenario.setValue("NORMAL");

        Label lblSd = new Label("Seed:");
        lblSd.setTextFill(Color.web("#a0aec0"));
        lblSd.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));

        TextField txtSeed = new TextField("42");
        txtSeed.setPrefWidth(60);

        Button btnReset = new Button("🔄 Reset");
        btnReset.getStyleClass().addAll("button", "btn-danger");
        btnReset.setOnAction(e -> {
            long seed = 42L;
            try { seed = Long.parseLong(txtSeed.getText().trim()); } catch (Exception ignored) {}
            apiClient.resetSimulationAsync(cbScenario.getValue(), seed).thenAccept(r -> Platform.runLater(this::refreshData));
        });

        Button btnExportCsv = new Button("📥 Export Ride Data (CSV)");
        btnExportCsv.getStyleClass().addAll("button", "btn-purple");
        btnExportCsv.setOnAction(e -> handleExportCsv());

        controls.getChildren().addAll(btnStart, btnPause, btnStep, lblScen, cbScenario, lblSd, txtSeed, btnReset, btnExportCsv);

        v.getChildren().addAll(title, controls);
        return v;
    }

    private void handleExportCsv() {
        apiClient.exportCsvAsync().thenAccept(csv -> Platform.runLater(() -> {
            try {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Save RideFlow CSV Report");
                fileChooser.setInitialFileName("rideflow_report.csv");
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
                File file = fileChooser.showSaveDialog(rootPane.getScene().getWindow());
                if (file != null) {
                    try (FileWriter fw = new FileWriter(file)) {
                        fw.write(csv);
                    }
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Report exported successfully to: " + file.getAbsolutePath());
                    alert.showAndWait();
                }
            } catch (Exception ex) {
                log.error("Failed to save CSV", ex);
            }
        }));
    }

    private void setupQueuedTable() {
        queuedTable.setStyle("-fx-control-inner-background: #1e1e24;");
        queuedTable.setPrefHeight(300);

        TableColumn<Map<String, Object>, String> colId = new TableColumn<>("Request ID");
        colId.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().get("requestId"))));

        TableColumn<Map<String, Object>, String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().get("requestType"))));

        TableColumn<Map<String, Object>, String> colPickup = new TableColumn<>("Pickup (X, Y)");
        colPickup.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                String.format("(%.1f, %.1f) [Z%s]", data.getValue().get("pickupX"), data.getValue().get("pickupY"), data.getValue().get("pickupZoneId"))
        ));

        TableColumn<Map<String, Object>, String> colDest = new TableColumn<>("Destination (X, Y)");
        colDest.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                String.format("(%.1f, %.1f) [Z%s]", data.getValue().get("destinationX"), data.getValue().get("destinationY"), data.getValue().get("destinationZoneId"))
        ));

        TableColumn<Map<String, Object>, String> colFare = new TableColumn<>("Fare");
        colFare.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().get("quotedFare"))));

        TableColumn<Map<String, Object>, String> colSurge = new TableColumn<>("Surge");
        colSurge.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().get("surgeMultiplier")) + "x"));

        TableColumn<Map<String, Object>, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().get("status"))));

        queuedTable.getColumns().addAll(colId, colType, colPickup, colDest, colFare, colSurge, colStatus);
    }

    private void setupDriverTable() {
        driverTable.setStyle("-fx-control-inner-background: #1e1e24;");
        driverTable.setPrefHeight(450);

        TableColumn<Map<String, Object>, String> colId = new TableColumn<>("Driver ID");
        colId.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().get("driverId"))));

        TableColumn<Map<String, Object>, String> colName = new TableColumn<>("Name");
        colName.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().get("name"))));

        TableColumn<Map<String, Object>, String> colType = new TableColumn<>("Vehicle");
        colType.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().get("vehicleType"))));

        TableColumn<Map<String, Object>, String> colNumber = new TableColumn<>("Number Plate");
        colNumber.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().get("vehicleNumber"))));

        TableColumn<Map<String, Object>, String> colPos = new TableColumn<>("Position (X, Y)");
        colPos.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                String.format("(%.1f, %.1f) [Z%s]", data.getValue().get("x"), data.getValue().get("y"), data.getValue().get("zoneId"))
        ));

        TableColumn<Map<String, Object>, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().get("status"))));

        TableColumn<Map<String, Object>, String> colEarnings = new TableColumn<>("Total Earnings");
        colEarnings.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().get("totalEarnings"))));

        TableColumn<Map<String, Object>, String> colTrips = new TableColumn<>("Completed Trips");
        colTrips.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().get("totalTripsCompleted"))));

        driverTable.getColumns().addAll(colId, colName, colType, colNumber, colPos, colStatus, colEarnings, colTrips);
    }

    private void showCreateRideDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Create Ride Request");
        dialog.setHeaderText("Create and queue a simulated ride booking");

        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(10);
        g.setPadding(new Insets(20));

        TextField txtRiderId = new TextField("1");
        TextField txtPx = new TextField("25.0");
        TextField txtPy = new TextField("30.0");
        TextField txtDx = new TextField("60.0");
        TextField txtDy = new TextField("70.0");
        ComboBox<RequestType> cbType = new ComboBox<>(FXCollections.observableArrayList(RequestType.values()));
        cbType.setValue(RequestType.STANDARD);

        g.add(new Label("Rider ID:"), 0, 0); g.add(txtRiderId, 1, 0);
        g.add(new Label("Pickup X (0-100):"), 0, 1); g.add(txtPx, 1, 1);
        g.add(new Label("Pickup Y (0-100):"), 0, 2); g.add(txtPy, 1, 2);
        g.add(new Label("Dest X (0-100):"), 0, 3); g.add(txtDx, 1, 3);
        g.add(new Label("Dest Y (0-100):"), 0, 4); g.add(txtDy, 1, 4);
        g.add(new Label("Request Type:"), 0, 5); g.add(cbType, 1, 5);

        dialog.getDialogPane().setContent(g);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    long riderId = Long.parseLong(txtRiderId.getText().trim());
                    double px = Double.parseDouble(txtPx.getText().trim());
                    double py = Double.parseDouble(txtPy.getText().trim());
                    double dx = Double.parseDouble(txtDx.getText().trim());
                    double dy = Double.parseDouble(txtDy.getText().trim());
                    apiClient.createRideAsync(riderId, px, py, dx, dy, cbType.getValue().name())
                            .thenAccept(res -> Platform.runLater(this::refreshData));
                } catch (Exception ex) {
                    log.error("Invalid ride input", ex);
                }
            }
            return null;
        });
        dialog.showAndWait();
    }

    private void showRegisterDriverDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Register Driver");
        dialog.setHeaderText("Add a new driver to the operational fleet");

        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(10);
        g.setPadding(new Insets(20));

        TextField txtName = new TextField("Ravi Shankar");
        ComboBox<VehicleType> cbVType = new ComboBox<>(FXCollections.observableArrayList(VehicleType.values()));
        cbVType.setValue(VehicleType.SEDAN);
        TextField txtNumber = new TextField("KA-05-RS-" + (1000 + new Random().nextInt(9000)));
        TextField txtX = new TextField("50.0");
        TextField txtY = new TextField("50.0");

        g.add(new Label("Driver Name:"), 0, 0); g.add(txtName, 1, 0);
        g.add(new Label("Vehicle Category:"), 0, 1); g.add(cbVType, 1, 1);
        g.add(new Label("License Plate:"), 0, 2); g.add(txtNumber, 1, 2);
        g.add(new Label("Initial X (0-100):"), 0, 3); g.add(txtX, 1, 3);
        g.add(new Label("Initial Y (0-100):"), 0, 4); g.add(txtY, 1, 4);

        dialog.getDialogPane().setContent(g);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    double x = Double.parseDouble(txtX.getText().trim());
                    double y = Double.parseDouble(txtY.getText().trim());
                    apiClient.registerDriverAsync(txtName.getText().trim(), cbVType.getValue().name(), txtNumber.getText().trim(), x, y)
                            .thenAccept(res -> Platform.runLater(this::refreshData));
                } catch (Exception ex) {
                    log.error("Invalid driver input", ex);
                }
            }
            return null;
        });
        dialog.showAndWait();
    }

    private void onEntitySelected(Map<String, Object> entity) {
        if (entity == null) {
            lblInspector.setText("No entity selected. Click a vehicle or ride on the canvas.");
            return;
        }
        StringBuilder sb = new StringBuilder();
        if (entity.containsKey("driverId")) {
            sb.append("=== DRIVER INSPECTOR ===\n")
                    .append("Driver ID: ").append(entity.get("driverId")).append("\n")
                    .append("Name: ").append(entity.get("name")).append("\n")
                    .append("Vehicle: ").append(entity.get("vehicleType")).append(" (").append(entity.get("vehicleNumber")).append(")\n")
                    .append("Status: ").append(entity.get("status")).append("\n")
                    .append("Coordinates: (").append(entity.get("x")).append(", ").append(entity.get("y")).append(")\n")
                    .append("Zone ID: ").append(entity.get("zoneId")).append("\n")
                    .append("Rating: ★ ").append(entity.get("rating")).append("\n")
                    .append("Trips Completed: ").append(entity.get("totalTripsCompleted")).append("\n")
                    .append("Total Earnings: ").append(entity.get("totalEarnings")).append("\n");
        } else if (entity.containsKey("requestId")) {
            sb.append("=== RIDE REQUEST INSPECTOR ===\n")
                    .append("Request ID: ").append(entity.get("requestId")).append("\n")
                    .append("Type: ").append(entity.get("requestType")).append("\n")
                    .append("Status: ").append(entity.get("status")).append("\n")
                    .append("Pickup: (").append(entity.get("pickupX")).append(", ").append(entity.get("pickupY")).append(")\n")
                    .append("Destination: (").append(entity.get("destinationX")).append(", ").append(entity.get("destinationY")).append(")\n")
                    .append("Fare Quote: ").append(entity.get("quotedFare")).append("\n")
                    .append("Surge Multiplier: ").append(entity.get("surgeMultiplier")).append("x\n")
                    .append("Assigned Driver: ").append(entity.get("assignedDriverId") != null ? entity.get("assignedDriverId") : "None (Waiting)").append("\n");
        }
        lblInspector.setText(sb.toString());
    }

    private void startPolling() {
        // Poll every 1000ms
        scheduler.scheduleAtFixedRate(() -> {
            try {
                refreshData();
            } catch (Exception e) {
                log.debug("Polling attempt failed: {}", e.getMessage());
            }
        }, 500, 1000, TimeUnit.MILLISECONDS);
    }

    private void refreshData() {
        // 1. Overview KPIs
        apiClient.getOverviewAsync().thenAccept(ov -> Platform.runLater(() -> {
            lblFleetTotal.setText(String.valueOf(ov.get("totalDrivers")));
            lblAvailable.setText(String.valueOf(ov.get("availableDrivers")));
            lblBusy.setText(String.valueOf(ov.get("busyDrivers")));
            lblQueued.setText(String.valueOf(ov.get("queuedRequests")));
            lblActive.setText(String.valueOf(ov.get("activeTrips")));
            lblCompleted.setText(String.valueOf(ov.get("completedTrips")));
            lblRevenue.setText("₹" + String.valueOf(ov.get("totalRevenue")));
            lblAvgSurge.setText(String.valueOf(ov.get("averageSurgeMultiplier")) + "x");
        })).exceptionally(ex -> null);

        // 2. Simulation State
        apiClient.getSimulationStateAsync().thenAccept(state -> Platform.runLater(() -> {
            lblSimClock.setText("Tick " + state.get("currentTick") + " | " + state.get("simulatedTime"));
            lblSimStatus.setText(String.valueOf(state.get("status")));
        })).exceptionally(ex -> null);

        // 3. Queued and Active Rides
        apiClient.getQueuedRidesAsync().thenAccept(queued -> Platform.runLater(() -> {
            queuedRidesList.setAll(queued);
        })).exceptionally(ex -> null);

        // 4. Drivers
        apiClient.getAllDriversAsync().thenAccept(drivers -> Platform.runLater(() -> {
            driverList.setAll(drivers);
        })).exceptionally(ex -> null);

        // 5. Active Rides for canvas
        apiClient.getActiveRidesAsync().thenAccept(active -> {
            apiClient.getAllDriversAsync().thenAccept(drivers -> {
                apiClient.getHeatmapAsync().thenAccept(heatmap -> {
                    Platform.runLater(() -> {
                        cityCanvas.updateState(drivers, active, queuedRidesList, heatmap);
                        renderHeatmapGrid(heatmap);
                    });
                });
            });
        }).exceptionally(ex -> null);

        // 6. Simulation Events Feed
        apiClient.getSimulationEventsAsync(30).thenAccept(events -> Platform.runLater(() -> {
            eventFeedList.clear();
            for (Map<String, Object> ev : events) {
                eventFeedList.add(String.format("[TICK %s] %s | Entity #%s | %s",
                        ev.get("simulationTick"), ev.get("eventType"), ev.get("entityId"), ev.get("payloadJson")));
            }
        })).exceptionally(ex -> null);
    }

    private void renderHeatmapGrid(List<Map<String, Object>> heatmap) {
        heatmapGrid.getChildren().clear();
        for (Map<String, Object> z : heatmap) {
            int zoneId = ((Number) z.get("zoneId")).intValue();
            int row = (zoneId - 1) / 5;
            int col = (zoneId - 1) % 5;

            double surge = ((Number) z.get("surgeMultiplier")).doubleValue();
            long avail = ((Number) z.get("availableDrivers")).longValue();
            long queued = ((Number) z.get("queuedRequests")).longValue();

            VBox cell = new VBox(4);
            cell.setPadding(new Insets(10));
            cell.setPrefSize(145, 92);
            cell.setAlignment(Pos.CENTER);

            String borderColor = "#232738";
            String bgColor = "#141724";
            String surgeTextColor = "#00f59b";

            if (surge > 1.8) {
                bgColor = "#3d131a";
                borderColor = "#ff4757";
                surgeTextColor = "#ff4757";
            } else if (surge > 1.3) {
                bgColor = "#352410";
                borderColor = "#ffaa00";
                surgeTextColor = "#ffaa00";
            } else if (surge > 1.0) {
                bgColor = "#18261e";
                borderColor = "#00f59b";
                surgeTextColor = "#00f59b";
            }

            cell.setStyle(String.format(
                    "-fx-background-color: %s; -fx-border-color: %s; -fx-border-radius: 8; -fx-background-radius: 8; -fx-border-width: 1.2;",
                    bgColor, borderColor
            ));

            HBox cellHeader = new HBox(8);
            cellHeader.setAlignment(Pos.CENTER);
            Label lblZ = new Label("ZONE " + zoneId);
            lblZ.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
            lblZ.setTextFill(Color.web("#a0aec0"));

            Label lblSurge = new Label(String.format("%.2fx", surge));
            lblSurge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
            lblSurge.setTextFill(Color.web(surgeTextColor));
            cellHeader.getChildren().addAll(lblZ, lblSurge);

            Label lblDS = new Label(String.format("Demand: %d  |  Supply: %d", queued, avail));
            lblDS.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 10));
            lblDS.setTextFill(Color.web("#cbd5e1"));

            // Mini balance bar
            ProgressBar balanceBar = new ProgressBar();
            balanceBar.setPrefWidth(120);
            balanceBar.setPrefHeight(4);
            double ratio = (queued + avail) == 0 ? 0.5 : (double) avail / (queued + avail);
            balanceBar.setProgress(ratio);
            balanceBar.setStyle("-fx-accent: " + surgeTextColor + ";");

            cell.getChildren().addAll(cellHeader, lblDS, balanceBar);
            heatmapGrid.add(cell, col, row);
        }
    }

    private VBox createKpiCard(String title, Label valLabel, String accentColor) {
        VBox box = new VBox(6);
        box.setPadding(new Insets(12, 16, 12, 16));
        box.setPrefWidth(140);
        box.getStyleClass().add("kpi-card");

        Label t = new Label(title);
        t.getStyleClass().add("kpi-title");

        valLabel.getStyleClass().add("kpi-value");
        valLabel.setTextFill(Color.web(accentColor));

        box.getChildren().addAll(t, valLabel);
        return box;
    }

    private HBox createLegendItem(String symbol, String text, String colorHex) {
        HBox h = new HBox(8);
        h.setAlignment(Pos.CENTER_LEFT);
        Label s = new Label(symbol);
        s.setTextFill(Color.web(colorHex));
        s.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));

        Label t = new Label(text);
        t.setTextFill(Color.web("#cbd5e1"));
        t.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 11));

        h.getChildren().addAll(s, t);
        return h;
    }
}
