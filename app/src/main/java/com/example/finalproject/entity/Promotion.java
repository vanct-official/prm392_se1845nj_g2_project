package com.example.finalproject.entity;

import com.google.firebase.firestore.PropertyName;

public class Promotion {
    private String id;
    private int discountPercent;
    private String description;
    private String name;
    private boolean isActive;
    private double minimumValue;
    private String validFrom;
    private String validTo;
    private String createdAt;

    public Promotion() {
    }

    public Promotion(String id, int discountPercent, String description, String name, boolean isActive, double minimumValue, String validFrom, String validTo, String createdAt) {
        this.id = id;
        this.discountPercent = discountPercent;
        this.description = description;
        this.name = name;
        this.isActive = isActive;
        this.minimumValue = minimumValue;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(int discountPercent) {
        this.discountPercent = discountPercent;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @PropertyName("isActive")
    public boolean isActive() {
        return isActive;
    }

    @PropertyName("isActive")
    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }

    public double getMinimumValue() {
        return minimumValue;
    }

    public void setMinimumValue(double minimumValue) {
        this.minimumValue = minimumValue;
    }

    public String getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(String validFrom) {
        this.validFrom = validFrom;
    }

    public String getValidTo() {
        return validTo;
    }

    public void setValidTo(String validTo) {
        this.validTo = validTo;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
