package vn.edu.ueh.rtmiue20.project.data.model;

import java.util.HashMap;
import java.util.Map;

public class User {
    private String uid;
    private String fullName;
    private String email;
    private String avatarUrl;
    private String phoneNumber;
    private String bio;
    private String role; // "user" or "admin"
    private String status; // "active" or "locked"
    private long createdAt;

    // Default constructor required for Firestore
    public User() {
    }

    public User(String uid, String fullName, String email, String avatarUrl, String phoneNumber, String bio, String role, String status, long createdAt) {
        this.uid = uid;
        this.fullName = fullName;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.phoneNumber = phoneNumber;
        this.bio = bio;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("uid", uid);
        map.put("fullName", fullName);
        map.put("email", email);
        map.put("avatarUrl", avatarUrl);
        map.put("phoneNumber", phoneNumber);
        map.put("bio", bio);
        map.put("role", role);
        map.put("status", status);
        map.put("createdAt", createdAt);
        return map;
    }
}
