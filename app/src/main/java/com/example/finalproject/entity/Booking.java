package com.example.finalproject.entity;

import com.google.firebase.Timestamp;

public class Booking {

    private String id;                // Firestore document ID
    private String tourId;            // ID của tour
    private String userId;            // ID người dùng
    private String promotionId;       // ID khuyến mãi (nếu có)
    private String paymentMethod;     // cash / online
    private String paymentStatus;     // pending / paid / refunded
    private String status;            // confirmed / cancelled / completed
    private String note;              // ghi chú khi đặt

    private Double subtotal;          // giá gốc
    private Double discountAmount;    // số tiền giảm
    private Double discountPercent;   // % giảm
    private Double finalPrice;        // giá sau giảm
    private Double amountPaid;        // số tiền đã trả
    private Double amountRemaining;   // còn lại phải trả
    private Integer quantity;         // số lượng người

    private Timestamp createAt;       // ngày tạo
    private Timestamp updateAt;       // ngày cập nhật

    // Bắt buộc phải có constructor rỗng cho Firestore
    public Booking() {}

    public Booking(String id, String tourId, String userId, String promotionId,
                   String paymentMethod, String paymentStatus, String status,
                   String note, Double subtotal, Double discountAmount,
                   Double discountPercent, Double finalPrice,
                   Double amountPaid, Double amountRemaining, Integer quantity,
                   Timestamp createAt, Timestamp updateAt) {
        this.id = id;
        this.tourId = tourId;
        this.userId = userId;
        this.promotionId = promotionId;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.status = status;
        this.note = note;
        this.subtotal = subtotal;
        this.discountAmount = discountAmount;
        this.discountPercent = discountPercent;
        this.finalPrice = finalPrice;
        this.amountPaid = amountPaid;
        this.amountRemaining = amountRemaining;
        this.quantity = quantity;
        this.createAt = createAt;
        this.updateAt = updateAt;
    }

    // --- GETTERS & SETTERS ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTourId() { return tourId; }
    public void setTourId(String tourId) { this.tourId = tourId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPromotionId() { return promotionId; }
    public void setPromotionId(String promotionId) { this.promotionId = promotionId; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Double getSubtotal() { return subtotal; }
    public void setSubtotal(Double subtotal) { this.subtotal = subtotal; }

    public Double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(Double discountAmount) { this.discountAmount = discountAmount; }

    public Double getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(Double discountPercent) { this.discountPercent = discountPercent; }

    public Double getFinalPrice() { return finalPrice; }
    public void setFinalPrice(Double finalPrice) { this.finalPrice = finalPrice; }

    public Double getAmountPaid() { return amountPaid; }
    public void setAmountPaid(Double amountPaid) { this.amountPaid = amountPaid; }

    public Double getAmountRemaining() { return amountRemaining; }
    public void setAmountRemaining(Double amountRemaining) { this.amountRemaining = amountRemaining; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Timestamp getCreateAt() { return createAt; }
    public void setCreateAt(Timestamp createAt) { this.createAt = createAt; }

    public Timestamp getUpdateAt() { return updateAt; }
    public void setUpdateAt(Timestamp updateAt) { this.updateAt = updateAt; }
}
