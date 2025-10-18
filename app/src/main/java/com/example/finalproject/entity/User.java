package com.example.finalproject.entity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

// Đây là entity về User, bao gồm các thuộc tính cơ bản của user

public class User {
    private String userid;
    private String username;
    private String firstname;
    private String lastname;
    private String email;
    private String phone;
    private String passwordHash;
    private Timestamp dob;
    private boolean gender;
    private boolean isActive;
    private boolean isEmailVerify;
    private String role;
    private String imageUrl;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public User() {
    }

    public User(String userid, String username, String firstname, String lastname, String email,
                String phone, String passwordHash, Timestamp dob, boolean gender, boolean isActive,
                boolean isEmailVerify, String role, String imageUrl, Timestamp createdAt, Timestamp updatedAt) {
        this.userid = userid;
        this.username = username;
        this.firstname = firstname;
        this.lastname = lastname;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.dob = dob;
        this.gender = gender;
        this.isActive = isActive;
        this.isEmailVerify = isEmailVerify;
        this.role = role;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    @PropertyName("username")
    public String getUsername() {
        return username;
    }

    @PropertyName("username")
    public void setUsername(String username) {
        this.username = username;
    }

    @PropertyName("firstname")
    public String getFirstname() {
        return firstname;
    }

    @PropertyName("firstname")
    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    @PropertyName("lastname")
    public String getLastname() {
        return lastname;
    }

    @PropertyName("lastname")
    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    @PropertyName("email")
    public String getEmail() {
        return email;
    }

    @PropertyName("email")
    public void setEmail(String email) {
        this.email = email;
    }

    @PropertyName("phone")
    public String getPhone() {
        return phone;
    }

    @PropertyName("phone")
    public void setPhone(String phone) {
        this.phone = phone;
    }

    @PropertyName("passwordHash")
    public String getPasswordHash() {
        return passwordHash;
    }

    @PropertyName("passwordHash")
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    @PropertyName("dob")
    public Timestamp getDob() {
        return dob;
    }

    @PropertyName("dob")
    public void setDob(Timestamp dob) {
        this.dob = dob;
    }

    @PropertyName("gender")
    public boolean isGender() {
        return gender;
    }

    @PropertyName("gender")
    public void setGender(boolean gender) {
        this.gender = gender;
    }

    @PropertyName("isActive")
    public boolean isActive() {
        return isActive;
    }

    @PropertyName("isActive")
    public void setActive(boolean active) {
        isActive = active;
    }

    @PropertyName("isEmailVerify")
    public boolean isEmailVerify() {
        return isEmailVerify;
    }

    @PropertyName("isEmailVerify")
    public void setEmailVerify(boolean emailVerify) {
        isEmailVerify = emailVerify;
    }

    @PropertyName("role")
    public String getRole() {
        return role;
    }

    @PropertyName("role")
    public void setRole(String role) {
        this.role = role;
    }

    @PropertyName("imageUrl")
    public String getImageUrl() {
        return imageUrl;
    }

    @PropertyName("imageUrl")
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @PropertyName("createdAt")
    public Timestamp getCreatedAt() {
        return createdAt;
    }

    @PropertyName("createdAt")
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @PropertyName("updatedAt")
    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    @PropertyName("updatedAt")
    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}