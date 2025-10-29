package com.example.finalproject.entity;

import com.google.firebase.Timestamp;

public class Booking {
    private String id;
    private String tourId;
    private String userId;
    private String status;
    private Timestamp bookingDate;

    private Timestamp createAt;

    // ✅ Field tạm để hiển thị tiêu đề tour mà không ghi đè tourId
    private transient String tourTitle;

    public Booking() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTourId() { return tourId; }
    public void setTourId(String tourId) { this.tourId = tourId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Timestamp getBookingDate() { return bookingDate; }
    public void setBookingDate(Timestamp bookingDate) { this.bookingDate = bookingDate; }

    public Timestamp getCreateAt() { return createAt; }
    public void setCreateAt(Timestamp createAt) { this.createAt = createAt; }

    public String getTourTitle() { return tourTitle; }
    public void setTourTitle(String tourTitle) { this.tourTitle = tourTitle; }
}
