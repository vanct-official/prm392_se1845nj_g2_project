package com.example.finalproject.entity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

public class Payment {
    private String id;        // ID của thanh toán (Firestore docId)
    private String bookingId;        // Liên kết với booking
    private String userId;           // Ai thanh toán
    private double amount;      // Tổng số tiền cần trả
    private String method;    // cash, visa, paypal, etc.
    private String status;    // pending, partial, completed, refunded
    private Timestamp paymentTime;
    private String transactionId;// ngày thanh toán
    private String note;             // Ghi chú nếu có (VD: "đã cọc 30%")

    // 🔹 Constructor rỗng (Firestore cần)
    public Payment() {}

    // 🔹 Constructor đầy đủ

    public Payment(String id, String bookingId, String userId, double amount, String method, String status, Timestamp paymentTime, String transactionId, String note) {
        this.id = id;
        this.bookingId = bookingId;
        this.userId = userId;
        this.amount = amount;
        this.method = method;
        this.status = status;
        this.paymentTime = paymentTime;
        this.transactionId = transactionId;
        this.note = note;
    }
    
    // 🔹 Getters và Setters

    @PropertyName("id")
    public String getId() {
        return id;
    }

    @PropertyName("id")
    public void setId(String id) {
        this.id = id;
    }

    @PropertyName("bookingId")
    public String getBookingId() {
        return bookingId;
    }

    @PropertyName("bookingId")
    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    @PropertyName("userId")
    public String getUserId() {
        return userId;
    }

    @PropertyName("userId")
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @PropertyName("amount")
    public double getAmount() {
        return amount;
    }

    @PropertyName("amount")
    public void setAmount(double amount) {
        this.amount = amount;
    }

    @PropertyName("method")
    public String getMethod() {
        return method;
    }

    @PropertyName("method")
    public void setMethod(String method) {
        this.method = method;
    }

    @PropertyName("status")
    public String getStatus() {
        return status;
    }

    @PropertyName("status")
    public void setStatus(String status) {
        this.status = status;
    }

    @PropertyName("paymentTime")
    public Timestamp getPaymentTime() {
        return paymentTime;
    }

    @PropertyName("paymentTime")
    public void setPaymentTime(Timestamp paymentTime) {
        this.paymentTime = paymentTime;
    }

    @PropertyName("transaction_ref")
    public String getTransactionId() {
        return transactionId;
    }

    @PropertyName("transaction_ref")
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    @PropertyName("note")
    public String getNote() {
        return note;
    }

    @PropertyName("note")
    public void setNote(String note) {
        this.note = note;
    }
}
