package vn.edu.ueh.rtmiue20.project.data.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import vn.edu.ueh.rtmiue20.project.R;

public class NotificationItem {

    public static final String TYPE_REQUEST_RECEIVED = "REQUEST_RECEIVED";
    public static final String TYPE_REQUEST_ACCEPTED = "REQUEST_ACCEPTED";
    public static final String TYPE_REQUEST_COMPLETED = "REQUEST_COMPLETED";
    public static final String TYPE_REQUEST_REJECTED = "REQUEST_REJECTED";
    public static final String TYPE_REQUEST_SENT = "REQUEST_SENT";
    public static final String TYPE_BOOK_PENDING = "BOOK_PENDING";
    public static final String TYPE_BOOK_HIDDEN = "BOOK_HIDDEN";
    public static final String TYPE_NEW_COMMENT = "NEW_COMMENT";

    @DocumentId
    private String id;
    private String userId;        // UID người nhận thông báo
    private String recipientId;   // Alias của userId
    private String senderId;     // UID người tạo hành động (người xin/chủ sách/bình luận)
    private String senderName;
    private String senderAvatar;
    private String type;
    private String title;
    private String message;
    private String subMessage;
    private long timestamp;
    private String imageUrl;
    private String statusTag;
    private boolean isActionRequired;
    private boolean read = false;
    private String targetScreen; // "REQUESTS" | "BOOK_DETAIL"
    private String targetTab;    // "received" | "sent"
    private String requestId;
    private String bookId;

    public NotificationItem() {}

    public NotificationItem(String id, String userId, String senderId, String senderName, String type,
                            String title, String message, String subMessage, long timestamp,
                            String imageUrl, String statusTag, boolean isActionRequired, boolean read,
                            String targetScreen, String targetTab, String requestId, String bookId) {
        this.id = id;
        this.userId = userId;
        this.recipientId = userId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.type = type;
        this.title = title;
        this.message = message;
        this.subMessage = subMessage;
        this.timestamp = timestamp;
        this.imageUrl = imageUrl;
        this.statusTag = statusTag;
        this.isActionRequired = isActionRequired;
        this.read = read;
        this.targetScreen = targetScreen;
        this.targetTab = targetTab;
        this.requestId = requestId;
        this.bookId = bookId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) {
        this.userId = userId;
        this.recipientId = userId;
    }

    public String getRecipientId() { return recipientId != null ? recipientId : userId; }
    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
        if (this.userId == null) this.userId = recipientId;
    }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getSenderAvatar() { return senderAvatar; }
    public void setSenderAvatar(String senderAvatar) { this.senderAvatar = senderAvatar; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getSubMessage() { return subMessage; }
    public void setSubMessage(String subMessage) { this.subMessage = subMessage; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getStatusTag() { return statusTag; }
    public void setStatusTag(String statusTag) { this.statusTag = statusTag; }

    public boolean isActionRequired() { return isActionRequired; }
    public void setActionRequired(boolean actionRequired) { isActionRequired = actionRequired; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public String getTargetScreen() { return targetScreen; }
    public void setTargetScreen(String targetScreen) { this.targetScreen = targetScreen; }

    public String getTargetTab() { return targetTab; }
    public void setTargetTab(String targetTab) { this.targetTab = targetTab; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }

    @Exclude
    public int getStatusBgRes() {
        if (TYPE_REQUEST_RECEIVED.equals(type) || TYPE_BOOK_PENDING.equals(type) || TYPE_REQUEST_SENT.equals(type) || "Chờ phản hồi".equalsIgnoreCase(statusTag) || "Chờ duyệt".equalsIgnoreCase(statusTag)) {
            return R.drawable.bg_db_pill_amber;
        } else if (TYPE_REQUEST_ACCEPTED.equals(type) || "Đã chấp nhận".equalsIgnoreCase(statusTag) || "Đã đồng ý".equalsIgnoreCase(statusTag)) {
            return R.drawable.bg_db_pill_green;
        } else if (TYPE_REQUEST_COMPLETED.equals(type) || "Hoàn tất".equalsIgnoreCase(statusTag) || "Đã nhận".equalsIgnoreCase(statusTag)) {
            return R.drawable.bg_db_pill_blue;
        } else if (TYPE_REQUEST_REJECTED.equals(type) || TYPE_BOOK_HIDDEN.equals(type) || "Đã từ chối".equalsIgnoreCase(statusTag) || "Đã ẩn".equalsIgnoreCase(statusTag)) {
            return R.drawable.bg_db_pill_red;
        }
        return R.drawable.bg_tag_secondary;
    }

    @Exclude
    public int getStatusTextColorRes() {
        if (TYPE_REQUEST_RECEIVED.equals(type) || TYPE_BOOK_PENDING.equals(type) || TYPE_REQUEST_SENT.equals(type) || "Chờ phản hồi".equalsIgnoreCase(statusTag) || "Chờ duyệt".equalsIgnoreCase(statusTag)) {
            return R.color.badge_pending_text;
        } else if (TYPE_REQUEST_ACCEPTED.equals(type) || "Đã chấp nhận".equalsIgnoreCase(statusTag) || "Đã đồng ý".equalsIgnoreCase(statusTag)) {
            return R.color.badge_accepted_text;
        } else if (TYPE_REQUEST_COMPLETED.equals(type) || "Hoàn tất".equalsIgnoreCase(statusTag) || "Đã nhận".equalsIgnoreCase(statusTag)) {
            return R.color.badge_completed_text;
        } else if (TYPE_REQUEST_REJECTED.equals(type) || TYPE_BOOK_HIDDEN.equals(type) || "Đã từ chối".equalsIgnoreCase(statusTag) || "Đã ẩn".equalsIgnoreCase(statusTag)) {
            return R.color.badge_rejected_text;
        }
        return R.color.secondary;
    }

    @Exclude
    public String getTimeAgo() {
        if (timestamp <= 0) return "Mới đây";
        long diff = System.currentTimeMillis() - timestamp;
        if (diff < 0) return "Vừa xong";
        long seconds = diff / 1000;
        if (seconds < 60) return "Vừa xong";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + " phút trước";
        long hours = minutes / 60;
        if (hours < 24) return hours + " giờ trước";
        long days = hours / 24;
        if (days == 1) return "Hôm qua";
        if (days < 7) return days + " ngày trước";
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(timestamp));
    }
}
