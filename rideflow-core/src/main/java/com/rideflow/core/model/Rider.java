package com.rideflow.core.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

public class Rider implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long riderId;
    private String name;
    private String phone;
    private LocalDateTime createdAt;

    public Rider() {
        this.createdAt = LocalDateTime.now();
    }

    public Rider(Long riderId, String name, String phone) {
        this();
        this.riderId = riderId;
        this.name = name;
        this.phone = phone;
    }

    public Rider(Long riderId, String name, String phone, LocalDateTime createdAt) {
        this.riderId = riderId;
        this.name = name;
        this.phone = phone;
        this.createdAt = createdAt;
    }

    public Long getRiderId() { return riderId; }
    public void setRiderId(Long riderId) { this.riderId = riderId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rider rider = (Rider) o;
        return Objects.equals(riderId, rider.riderId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(riderId);
    }

    @Override
    public String toString() {
        return "Rider{" + "id=" + riderId + ", name='" + name + '\'' + ", phone='" + phone + '\'' + '}';
    }
}
