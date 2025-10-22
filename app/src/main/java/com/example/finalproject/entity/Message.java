package com.example.finalproject.entity;

import com.google.firebase.Timestamp;

import java.util.List;

public class Message {
    private String id;
    private String senderId;
    private String content;
    private String imageUrl;
    private Timestamp timestamp;
    private List<String> seenBy;

    public Message() {
    }

    public Message(String id, String senderId, String content, String imageUrl, Timestamp timestamp, List<String> seenBy) {
        this.id = id;
        this.senderId = senderId;
        this.content = content;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
        this.seenBy = seenBy;
    }

    // Getters & Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public List<String> getSeenBy() {
        return seenBy;
    }

    public void setSeenBy(List<String> seenBy) {
        this.seenBy = seenBy;
    }
}
