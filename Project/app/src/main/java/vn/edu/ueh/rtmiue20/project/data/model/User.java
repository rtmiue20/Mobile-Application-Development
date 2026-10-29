package vn.edu.ueh.rtmiue20.project.data.model;

import java.util.HashMap;
import java.util.Map;
import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_table")
public class User {
    @PrimaryKey
    @NonNull
    private String uid = "";
    private String fullName;
    private String nickname;
    private String email;
    private String avatarUrl;
    private String phoneNumber;
    private String bio;
    private String city;
    private String district;
    private String ward;
    private String favoriteSpotName;
    private String favoriteSpotAddress;
    private Double latitude;
    private Double longitude;
    private String role; // "user" or "admin"
    private String status; // "active" or "locked"
    private String lockReason;
    private long createdAt;

    @Ignore
    private java.util.List<String> interests;

    @Ignore
    private Boolean showPhone = true;
    @Ignore
    private Boolean directRequest = true;
    @Ignore
    private Boolean bookAlerts = true;

    // Default constructor required for Firestore
    public User() {
    }

    @Ignore
    public User(@NonNull String uid, String fullName, String email, String avatarUrl, String phoneNumber, String bio, String role, String status, long createdAt) {
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

    public static String getDisplayNameFormatted(String fullName, String nickname) {
        if (nickname != null && !nickname.trim().isEmpty()) {
            return (fullName != null ? fullName : "") + " (" + nickname.trim() + ")";
        }
        return fullName != null ? fullName : "";
    }

    @NonNull
    public String getUid() {
        return uid;
    }

    public void setUid(@NonNull String uid) {
        this.uid = uid;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
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

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getWard() {
        return ward;
    }

    public void setWard(String ward) {
        this.ward = ward;
    }

    public String getFavoriteSpotName() {
        return favoriteSpotName;
    }

    public void setFavoriteSpotName(String favoriteSpotName) {
        this.favoriteSpotName = favoriteSpotName;
    }

    public String getFavoriteSpotAddress() {
        return favoriteSpotAddress;
    }

    public void setFavoriteSpotAddress(String favoriteSpotAddress) {
        this.favoriteSpotAddress = favoriteSpotAddress;
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

    public String getLockReason() {
        return lockReason;
    }

    public void setLockReason(String lockReason) {
        this.lockReason = lockReason;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public java.util.List<String> getInterests() {
        return interests != null ? interests : new java.util.ArrayList<>();
    }

    public void setInterests(java.util.List<String> interests) {
        this.interests = interests;
    }

    public Boolean isShowPhone() {
        return showPhone != null ? showPhone : true;
    }

    public void setShowPhone(Boolean showPhone) {
        this.showPhone = showPhone;
    }

    public Boolean isDirectRequest() {
        return directRequest != null ? directRequest : true;
    }

    public void setDirectRequest(Boolean directRequest) {
        this.directRequest = directRequest;
    }

    public Boolean isBookAlerts() {
        return bookAlerts != null ? bookAlerts : true;
    }

    public void setBookAlerts(Boolean bookAlerts) {
        this.bookAlerts = bookAlerts;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("uid", uid);
        map.put("fullName", fullName);
        map.put("nickname", nickname);
        map.put("email", email);
        map.put("avatarUrl", avatarUrl);
        map.put("phoneNumber", phoneNumber);
        map.put("bio", bio);
        map.put("city", city);
        map.put("district", district);
        map.put("ward", ward);
        map.put("favoriteSpotName", favoriteSpotName);
        map.put("favoriteSpotAddress", favoriteSpotAddress);
        map.put("latitude", latitude);
        map.put("longitude", longitude);
        map.put("role", role);
        map.put("status", status);
        map.put("lockReason", lockReason);
        map.put("createdAt", createdAt);
        if (interests != null) {
            map.put("interests", interests);
        }
        if (showPhone != null) map.put("showPhone", showPhone);
        if (directRequest != null) map.put("directRequest", directRequest);
        if (bookAlerts != null) map.put("bookAlerts", bookAlerts);
        return map;
    }
}
