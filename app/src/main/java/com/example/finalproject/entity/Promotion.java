package com.example.finalproject.entity;

public class Promotion {
    private String id;
    private String description;
    private int discountPercent;
    private boolean isActive;
    private double minimumValue;
    private String validFrom;
    private String validTo;

    public Promotion() {} // Bắt buộc cho Firestore

    public Promotion(String id, String description, int discountPercent, boolean isActive,
                     double minimumValue, String validFrom, String validTo) {
        this.id = id;
        this.description = description;
        this.discountPercent = discountPercent;
        this.isActive = isActive;
        this.minimumValue = minimumValue;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }

    // Getter
    public String getId() { return id; }
    public String getDescription() { return description; }
    public int getDiscountPercent() { return discountPercent; }
    public boolean isActive() { return isActive; }
    public double getMinimumValue() { return minimumValue; }
    public String getValidFrom() { return validFrom; }
    public String getValidTo() { return validTo; }
}
