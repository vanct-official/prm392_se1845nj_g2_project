package com.example.finalproject.entity;

import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Review {
    private String id;
    private String userId;
    private String userName;
    private String userAvatar;
    private String tourId;
    private String tourName;
    private String comment;
    private Object rating; // có thể là Long, Double, hoặc String
    private Timestamp createdAt;

    public Review() {
        // Firestore cần constructor rỗng
    }

    public Review(String id, String userId, String userName, String userAvatar,
                  String tourId, String tourName, String comment,
                  Object rating, Timestamp createdAt) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.userAvatar = userAvatar;
        this.tourId = tourId;
        this.tourName = tourName;
        this.comment = comment;
        this.rating = rating;
        this.createdAt = createdAt;
    }

    // ===== Getter & Setter =====
    public String getId() {
        return id;
    }

    public void setId(String id) { this.id = id; }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) { this.userName = userName; }

    public String getUserAvatar() {
        return userAvatar;
    }

    public void setUserAvatar(String userAvatar) { this.userAvatar = userAvatar; }

    public String getTourId() {
        return tourId;
    }

    public void setTourId(String tourId) { this.tourId = tourId; }

    public String getTourName() {
        return tourName;
    }

    public void setTourName(String tourName) { this.tourName = tourName; }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) { this.comment = comment; }

    public Object getRating() {
        return rating;
    }

    public void setRating(Object rating) { this.rating = rating; }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    // ===== Xử lý định dạng ngày =====
    public String getFormattedDate() {
        if (createdAt == null) return "";
        Date date = createdAt.toDate();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return sdf.format(date);
    }
}
