package com.rideflow.desktop.view;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.*;
import java.util.function.Consumer;

/**
 * Enhanced, hardware-accelerated JavaFX Canvas rendering the 100x100 city grid
 * with glowing vector routes, dynamic surge heatmap gradients, and real-time vehicle kinematics.
 */
public class CityCanvasView extends Canvas {

    private List<Map<String, Object>> drivers = new ArrayList<>();
    private List<Map<String, Object>> activeRides = new ArrayList<>();
    private List<Map<String, Object>> queuedRides = new ArrayList<>();
    private List<Map<String, Object>> heatmap = new ArrayList<>();

    private Map<String, Object> selectedEntity = null;
    private Consumer<Map<String, Object>> selectionListener = null;

    private double hoverGridX = -1;
    private double hoverGridY = -1;

    public CityCanvasView(double width, double height) {
        super(width, height);
        initInteractions();
    }

    private void initInteractions() {
        setOnMouseMoved(event -> {
            hoverGridX = Math.min(100.0, Math.max(0.0, (event.getX() / getWidth()) * 100.0));
            hoverGridY = Math.min(100.0, Math.max(0.0, (event.getY() / getHeight()) * 100.0));
            redraw();
        });

        setOnMouseExited(event -> {
            hoverGridX = -1;
            hoverGridY = -1;
            redraw();
        });

        setOnMouseClicked(event -> {
            double gridX = (event.getX() / getWidth()) * 100.0;
            double gridY = (event.getY() / getHeight()) * 100.0;

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

        // 1. Deep Obsidian Background
        gc.setFill(Color.web("#0e1017"));
        gc.fillRect(0, 0, w, h);

        // 2. Subtle Grid Background lines & Crosshairs
        drawSubtleGrid(gc, w, h);

        // 3. Render 5x5 Zones with Surge Glow Heatmap
        drawZones(gc, w, h);

        // 4. Render Active Trip Vectors (route paths with glow)
        drawTripVectors(gc, w, h);

        // 5. Render Queued Rides
        drawQueuedRides(gc, w, h);

        // 6. Render Drivers
        drawDrivers(gc, w, h);

        // 7. Render Coordinates Overlay HUD
        drawHud(gc, w, h);
    }

    private void drawSubtleGrid(GraphicsContext gc, double w, double h) {
        gc.setStroke(Color.web("#171a26"));
        gc.setLineWidth(0.5);

        // 10x10 coordinate subdivisions
        for (int i = 1; i < 10; i++) {
            double x = (i * 10.0) * (w / 100.0);
            double y = (i * 10.0) * (h / 100.0);
            gc.strokeLine(x, 0, x, h);
            gc.strokeLine(0, y, w, y);
        }
    }

    private void drawZones(GraphicsContext gc, double w, double h) {
        double zoneWidth = w / 5.0;
        double zoneHeight = h / 5.0;

        Map<Integer, Double> surgeMap = new HashMap<>();
        Map<Integer, Double> trafficMap = new HashMap<>();
        for (Map<String, Object> z : heatmap) {
            int zid = ((Number) z.get("zoneId")).intValue();
            double surge = ((Number) z.get("surgeMultiplier")).doubleValue();
            double traffic = ((Number) z.get("trafficMultiplier")).doubleValue();
            surgeMap.put(zid, surge);
            trafficMap.put(zid, traffic);
        }

        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                int zoneId = row * 5 + col + 1;
                double zx = col * zoneWidth;
                double zy = row * zoneHeight;

                double surge = surgeMap.getOrDefault(zoneId, 1.0);
                double traffic = trafficMap.getOrDefault(zoneId, 1.0);

                // Surge Heat Wash
                if (surge > 1.05) {
                    double intensity = Math.min(1.0, (surge - 1.0) / 1.5);
                    Color glowStart = Color.color(0.95, 0.20, 0.25, 0.35 * intensity);
                    Color glowEnd = Color.color(0.95, 0.40, 0.10, 0.10 * intensity);
                    LinearGradient lg = new LinearGradient(zx, zy, zx + zoneWidth, zy + zoneHeight, false, CycleMethod.NO_CYCLE,
                            new Stop(0, glowStart), new Stop(1, glowEnd));
                    gc.setFill(lg);
                    gc.fillRect(zx, zy, zoneWidth, zoneHeight);
                }

                // Zone Border
                gc.setStroke(Color.web("#222738"));
                gc.setLineWidth(1.0);
                gc.strokeRect(zx, zy, zoneWidth, zoneHeight);

                // Zone Header Tag
                gc.setFill(Color.web("#141724", 0.85));
                gc.fillRoundRect(zx + 4, zy + 4, 52, 16, 4, 4);

                gc.setFill(Color.web("#a0aec0"));
                gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
                gc.fillText("ZONE " + zoneId, zx + 8, zy + 15);

                // Traffic / Surge Indicator Pill
                if (surge > 1.05) {
                    gc.setFill(Color.web("#ff4757"));
                    gc.fillRoundRect(zx + 60, zy + 4, 38, 16, 4, 4);
                    gc.setFill(Color.WHITE);
                    gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
                    gc.fillText(String.format("%.1fx", surge), zx + 65, zy + 15);
                } else if (traffic > 1.2) {
                    gc.setFill(Color.web("#ffaa00", 0.4));
                    gc.fillRoundRect(zx + 60, zy + 4, 28, 16, 4, 4);
                    gc.setFill(Color.web("#ffaa00"));
                    gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 8));
                    gc.fillText("BUSY", zx + 64, zy + 15);
                }
            }
        }
    }

    private void drawTripVectors(GraphicsContext gc, double w, double h) {
        for (Map<String, Object> r : activeRides) {
            double px = ((Number) r.get("pickupX")).doubleValue() * (w / 100.0);
            double py = ((Number) r.get("pickupY")).doubleValue() * (h / 100.0);
            double dx = ((Number) r.get("destinationX")).doubleValue() * (w / 100.0);
            double dy = ((Number) r.get("destinationY")).doubleValue() * (h / 100.0);

            // Outer Glow Path
            gc.setStroke(Color.web("#00a8ff", 0.20));
            gc.setLineWidth(3.0);
            gc.strokeLine(px, py, dx, dy);

            // Core Route Line
            gc.setStroke(Color.web("#00a8ff", 0.85));
            gc.setLineWidth(1.2);
            gc.strokeLine(px, py, dx, dy);

            // Pickup Origin Node (Green Ring)
            gc.setFill(Color.web("#00f59b", 0.4));
            gc.fillOval(px - 5, py - 5, 10, 10);
            gc.setFill(Color.web("#00f59b"));
            gc.fillOval(px - 2, py - 2, 4, 4);

            // Destination Target Pin (Red/Coral Diamond)
            gc.setFill(Color.web("#ff4757"));
            gc.fillOval(dx - 4, dy - 4, 8, 8);
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(1.0);
            gc.strokeOval(dx - 4, dy - 4, 8, 8);
        }
    }

    private void drawQueuedRides(GraphicsContext gc, double w, double h) {
        for (Map<String, Object> r : queuedRides) {
            double px = ((Number) r.get("pickupX")).doubleValue() * (w / 100.0);
            double py = ((Number) r.get("pickupY")).doubleValue() * (h / 100.0);

            // Amber Pulsing Diamond for queued passenger
            gc.setFill(Color.web("#ffaa00", 0.35));
            gc.fillOval(px - 7, py - 7, 14, 14);

            gc.setFill(Color.web("#ffaa00"));
            gc.fillOval(px - 4, py - 4, 8, 8);
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(1.2);
            gc.strokeOval(px - 4, py - 4, 8, 8);

            // Priority Badge Indicator
            String type = String.valueOf(r.get("requestType"));
            if ("PRIORITY".equalsIgnoreCase(type)) {
                gc.setFill(Color.web("#ff4757"));
                gc.fillOval(px + 3, py - 7, 5, 5);
            }
        }
    }

    private void drawDrivers(GraphicsContext gc, double w, double h) {
        for (Map<String, Object> d : drivers) {
            double dx = ((Number) d.get("x")).doubleValue() * (w / 100.0);
            double dy = ((Number) d.get("y")).doubleValue() * (h / 100.0);
            String status = (String) d.get("status");

            Color coreColor;
            Color auraColor;

            switch (status) {
                case "AVAILABLE" -> {
                    coreColor = Color.web("#00f59b"); // Emerald
                    auraColor = Color.web("#00f59b", 0.25);
                }
                case "RESERVED" -> {
                    coreColor = Color.web("#ffaa00"); // Amber
                    auraColor = Color.web("#ffaa00", 0.30);
                }
                case "ON_TRIP" -> {
                    coreColor = Color.web("#00a8ff"); // Electric Blue
                    auraColor = Color.web("#00a8ff", 0.30);
                }
                default -> {
                    coreColor = Color.web("#64748b"); // Slate Gray
                    auraColor = Color.TRANSPARENT;
                }
            }

            // Outer Aura
            if (auraColor != Color.TRANSPARENT) {
                gc.setFill(auraColor);
                gc.fillOval(dx - 8, dy - 8, 16, 16);
            }

            // Vehicle Core
            gc.setFill(coreColor);
            gc.fillOval(dx - 5, dy - 5, 10, 10);
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(1.0);
            gc.strokeOval(dx - 5, dy - 5, 10, 10);

            // Highlight selected entity with double neon ring
            if (selectedEntity != null && d.get("driverId").equals(selectedEntity.get("driverId"))) {
                gc.setStroke(Color.web("#00f59b"));
                gc.setLineWidth(2.0);
                gc.strokeOval(dx - 12, dy - 12, 24, 24);

                gc.setStroke(Color.WHITE);
                gc.setLineWidth(1.0);
                gc.strokeOval(dx - 15, dy - 15, 30, 30);
            }
        }
    }

    private void drawHud(GraphicsContext gc, double w, double h) {
        // Bottom Coordinate Pill
        if (hoverGridX >= 0 && hoverGridY >= 0) {
            int zoneId = ((int) (hoverGridY / 20.0)) * 5 + ((int) (hoverGridX / 20.0)) + 1;
            String hudText = String.format("COORDINATE: (%.1f, %.1f) | ZONE %d", hoverGridX, hoverGridY, zoneId);

            gc.setFill(Color.web("#12141f", 0.90));
            gc.fillRoundRect(8, h - 28, 220, 22, 6, 6);
            gc.setStroke(Color.web("#282e42"));
            gc.setLineWidth(1);
            gc.strokeRoundRect(8, h - 28, 220, 22, 6, 6);

            gc.setFill(Color.web("#00f59b"));
            gc.setFont(Font.font("Consolas", FontWeight.BOLD, 10));
            gc.fillText(hudText, 16, h - 13);
        }
    }

    private Map<String, Object> findEntityNear(double gridX, double gridY) {
        double threshold = 4.0;
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
