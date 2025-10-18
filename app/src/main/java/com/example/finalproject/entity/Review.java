package com.example.finalproject.entity;

public class Review {
    private String id;
    private String userId;
    private String userName;
    private String tourId;
    private String tourName;
    private String comment;
    private double rating;

    public Review() {
        // Bắt buộc có constructor rỗng để Firestore map dữ liệu
    }

    // --- ID ---
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    // --- USER ---
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName != null ? userName : "";
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    // --- TOUR ---
    public String getTourId() {
        return tourId;
    }

    public void setTourId(String tourId) {
        this.tourId = tourId;
    }

    public String getTourName() {
        return tourName != null ? tourName : "";
    }

    public void setTourName(String tourName) {
        this.tourName = tourName;
    }

    // --- COMMENT ---
    public String getComment() {
        return comment != null ? comment : "";
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    // --- RATING ---
    public double getRating() {
        return rating;
    }

    public void setRating(Object ratingObj) {
        try {
            if (ratingObj instanceof Number) {
                this.rating = ((Number) ratingObj).doubleValue();
            } else if (ratingObj instanceof String) {
                this.rating = Double.parseDouble((String) ratingObj);
            }
        } catch (Exception e) {
            this.rating = 0;
        }
    }
}
