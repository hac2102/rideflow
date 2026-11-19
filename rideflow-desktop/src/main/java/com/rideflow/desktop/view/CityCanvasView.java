package com.rideflow.desktop.view;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.*;
import java.util.function.Consumer;

/**
 * High-performance JavaFX Canvas rendering the simulated 100x100 city grid in real-time.
 */
public class CityCanvasView extends Canvas {

    private List<Map<String, Object>> drivers = new ArrayList<>();
    private List<Map<String, Object>> activeRides = new ArrayList<>();
    private List<Map<String, Object>> queuedRides = new ArrayList<>();
    private List<Map<String, Object>> heatmap = new ArrayList<>();

    private Map<String, Object> selectedEntity = null;
    private Consumer<Map<String, Object>> selectionListener = null;

    public CityCanvasView(double width, double height) {
        super(width, height);
        initInteractions();
    }

    private void initInteractions() {
        setOnMouseClicked(event -> {
            double mouseX = event.getX();
            double mouseY = event.getY();
            double gridX = (mouseX / getWidth()) * 100.0;
            double gridY = (mouseY / getHeight()) * 100.0;

            Map<String, Object> clicked = findEntityNear(gridX, gridY);
            this.selectedEntity = clicked;
            if (selectionListener != null) {
                selectionListener.accept(clicked);
            }
            redraw();
        });
    }

    public void setSelectionListener(Consumer<Map<String, Object>> listener) {
        this.selectionListener = listener;
    }

    public void updateState(List<Map<String, Object>> drivers,
                            List<Map<String, Object>> activeRides,
                            List<Map<String, Object>> queuedRides,
                            List<Map<String, Object>> heatmap) {
        this.drivers = drivers != null ? drivers : Collections.emptyList();
        this.activeRides = activeRides != null ? activeRides : Collections.emptyList();
        this.queuedRides = queuedRides != null ? queuedRides : Collections.emptyList();
        this.heatmap = heatmap != null ? heatmap : Collections.emptyList();
        redraw();
    }

    public void redraw() {
        GraphicsContext gc = getGraphicsContext2D();
        double w = getWidth();
        double h = getHeight();

        // 1. Dark Theme Background
        gc.setFill(Color.web("#1e1e24"));
        gc.fillRect(0, 0, w, h);

        // 2. Render 5x5 Zones & Surge Heatmap
        drawZones(gc, w, h);

        // 3. Render Active Trip Vectors (route paths)
        drawTripVectors(gc, w, h);

        // 4. Render Queued Rides
        drawQueuedRides(gc, w, h);

        // 5. Render Drivers
        drawDrivers(gc, w, h);
    }

    private void drawZones(GraphicsContext gc, double w, double h) {
        double zoneWidth = w / 5.0;
        double zoneHeight = h / 5.0;

        Map<Integer, Double> surgeMap = new HashMap<>();
        for (Map<String, Object> z : heatmap) {
            int zid = ((Number) z.get("zoneId")).intValue();
            double surge = ((Number) z.get("surgeMultiplier")).doubleValue();
            surgeMap.put(zid, surge);
        }

        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                int zoneId = row * 5 + col + 1;
                double zx = col * zoneWidth;
                double zy = row * zoneHeight;

                double surge = surgeMap.getOrDefault(zoneId, 1.0);
                if (surge > 1.05) {
                    // Surge heat tint: soft orange/red with alpha proportional to surge
                    double alpha = Math.min(0.35, (surge - 1.0) / 1.5 * 0.35);
                    gc.setFill(Color.color(0.9, 0.2, 0.1, alpha));
                    gc.fillRect(zx, zy, zoneWidth, zoneHeight);
                }

                // Grid boundary
                gc.setStroke(Color.web("#33333f"));
                gc.setLineWidth(1.0);
                gc.strokeRect(zx, zy, zoneWidth, zoneHeight);

                // Zone Label
                gc.setFill(Color.web("#666677"));
                gc.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 10));
                gc.fillText("Z" + zoneId + (surge > 1.0 ? String.format(" (%.1fx)", surge) : ""), zx + 4, zy + 14);
            }
        }
    }

    private void drawTripVectors(GraphicsContext gc, double w, double h) {
        gc.setLineWidth(1.5);
        for (Map<String, Object> r : activeRides) {
            double px = ((Number) r.get("pickupX")).doubleValue() * (w / 100.0);
            double py = ((Number) r.get("pickupY")).doubleValue() * (h / 100.0);
            double dx = ((Number) r.get("destinationX")).doubleValue() * (w / 100.0);
            double dy = ((Number) r.get("destinationY")).doubleValue() * (h / 100.0);

            // Vector line connecting pickup to dropoff
            gc.setStroke(Color.web("#4da6ff", 0.5));
            gc.strokeLine(px, py, dx, dy);

            // Dropoff target flag
            gc.setFill(Color.web("#ff4d4d"));
            gc.fillOval(dx - 3, dy - 3, 6, 6);
        }
    }

    private void drawQueuedRides(GraphicsContext gc, double w, double h) {
        for (Map<String, Object> r : queuedRides) {
            double px = ((Number) r.get("pickupX")).doubleValue() * (w / 100.0);
            double py = ((Number) r.get("pickupY")).doubleValue() * (h / 100.0);

            // Yellow Diamond for queued requests
            gc.setFill(Color.web("#ffcc00"));
            gc.fillOval(px - 4, py - 4, 8, 8);
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(1);
            gc.strokeOval(px - 4, py - 4, 8, 8);
        }
    }

    private void drawDrivers(GraphicsContext gc, double w, double h) {
        for (Map<String, Object> d : drivers) {
            double dx = ((Number) d.get("x")).doubleValue() * (w / 100.0);
            double dy = ((Number) d.get("y")).doubleValue() * (h / 100.0);
            String status = (String) d.get("status");

            Color color;
            switch (status) {
                case "AVAILABLE" -> color = Color.web("#00e676"); // Vibrant Green
                case "RESERVED" -> color = Color.web("#ff9100");  // Orange
                case "ON_TRIP" -> color = Color.web("#2979ff");   // Vibrant Blue
                default -> color = Color.web("#757575");          // Gray (Offline)
            }

            gc.setFill(color);
            gc.fillOval(dx - 5, dy - 5, 10, 10);
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(1.0);
            gc.strokeOval(dx - 5, dy - 5, 10, 10);

            // Highlight selected entity
            if (selectedEntity != null && d.get("driverId").equals(selectedEntity.get("driverId"))) {
                gc.setStroke(Color.YELLOW);
                gc.setLineWidth(2.0);
                gc.strokeOval(dx - 9, dy - 9, 18, 18);
            }
        }
    }

    private Map<String, Object> findEntityNear(double gridX, double gridY) {
        double threshold = 4.0; // 4 grid units
        for (Map<String, Object> d : drivers) {
            double dx = ((Number) d.get("x")).doubleValue();
            double dy = ((Number) d.get("y")).doubleValue();
            if (Math.hypot(dx - gridX, dy - gridY) <= threshold) {
                return d;
            }
        }
        for (Map<String, Object> r : queuedRides) {
            double rx = ((Number) r.get("pickupX")).doubleValue();
            double ry = ((Number) r.get("pickupY")).doubleValue();
            if (Math.hypot(rx - gridX, ry - gridY) <= threshold) {
                return r;
            }
        }
        return null;
    }
}
