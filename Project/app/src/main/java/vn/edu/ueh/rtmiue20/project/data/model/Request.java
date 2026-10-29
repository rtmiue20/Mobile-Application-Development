package vn.edu.ueh.rtmiue20.project.data.model;

import com.google.firebase.firestore.Exclude;

public class Request {
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_ACCEPTED = "accepted";
    public static final String STATUS_REJECTED = "rejected";
    public static final String STATUS_COMPLETED = "completed";

    private String requestId;
    private String bookId;
    private String bookTitle;
    private String bookImageUrl; // Mới: Ảnh bìa sách
    private String bookAuthor;
    private String requesterId;
    private String requesterName;
    private String requesterAvatar; // Mới: Ảnh người xin
    private String ownerId;
    private String ownerName; // Mới: Tên chủ sách (dùng cho tab Đã gửi)
    private String message; // Mới: Lời nhắn
    private String status;
    private long timestamp;
    private boolean rated = false;
    private float ratingValue = 5.0f;
    private String ratingComment;
    private long completedTimestamp;
    private long receivedAt;
    private boolean receiveReportedNotYet = false;
    private String notReceivedNote;
    private long notReceivedTimestamp;

    public Request() {}

    public Request(String requestId, String bookId, String bookTitle, String bookImageUrl,
                   String requesterId, String requesterName, String requesterAvatar,
                   String ownerId, String ownerName, String message, String status, long timestamp) {
        this.requestId = requestId;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.bookImageUrl = bookImageUrl;
        this.requesterId = requesterId;
        this.requesterName = requesterName;
        this.requesterAvatar = requesterAvatar;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.message = message;
        this.status = status;
        this.timestamp = timestamp;
    }

    public String getBookAuthor() { return bookAuthor; }
    public void setBookAuthor(String bookAuthor) { this.bookAuthor = bookAuthor; }

    @Exclude
    public String getRequestId() { return requestId; }
    @Exclude
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }

    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

    public String getBookImageUrl() { return bookImageUrl; }
    public void setBookImageUrl(String bookImageUrl) { this.bookImageUrl = bookImageUrl; }

    public String getRequesterId() { return requesterId; }
    public void setRequesterId(String requesterId) { this.requesterId = requesterId; }

    public String getRequesterName() { return requesterName; }
    public void setRequesterName(String requesterName) { this.requesterName = requesterName; }

    public String getRequesterAvatar() { return requesterAvatar; }
    public void setRequesterAvatar(String requesterAvatar) { this.requesterAvatar = requesterAvatar; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isRated() { return rated; }
    public void setRated(boolean rated) { this.rated = rated; }

    public float getRatingValue() { return ratingValue; }
    public void setRatingValue(float ratingValue) { this.ratingValue = ratingValue; }

    public String getRatingComment() { return ratingComment; }
    public void setRatingComment(String ratingComment) { this.ratingComment = ratingComment; }

    public long getCompletedTimestamp() { return completedTimestamp; }
    public void setCompletedTimestamp(long completedTimestamp) { this.completedTimestamp = completedTimestamp; }

    public long getReceivedAt() { return receivedAt; }
    public void setReceivedAt(long receivedAt) { this.receivedAt = receivedAt; }

    public boolean isReceiveReportedNotYet() { return receiveReportedNotYet; }
    public void setReceiveReportedNotYet(boolean receiveReportedNotYet) { this.receiveReportedNotYet = receiveReportedNotYet; }

    public String getNotReceivedNote() { return notReceivedNote; }
    public void setNotReceivedNote(String notReceivedNote) { this.notReceivedNote = notReceivedNote; }

    public long getNotReceivedTimestamp() { return notReceivedTimestamp; }
    public void setNotReceivedTimestamp(long notReceivedTimestamp) { this.notReceivedTimestamp = notReceivedTimestamp; }
}
