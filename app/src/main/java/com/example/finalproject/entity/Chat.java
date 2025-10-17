package com.example.finalproject.entity;

import com.google.firebase.Timestamp;
import java.util.List;

public class Chat {
    private String id;
    private String type;
    private List<String> members;
    private String lastMessage;
    private Timestamp lastMessageTime;

    public Chat() {
    }

    public Chat(String id, String type, List<String> members, String lastMessage, Timestamp lastMessageTime) {
        this.id = id;
        this.type = type;
        this.members = members;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
    }

    // Getters & Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getMembers() {
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public Timestamp getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(Timestamp lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }
}
