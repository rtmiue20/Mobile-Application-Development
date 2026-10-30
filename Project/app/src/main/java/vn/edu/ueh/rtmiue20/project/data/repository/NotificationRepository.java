package vn.edu.ueh.rtmiue20.project.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vn.edu.ueh.rtmiue20.project.data.model.NotificationItem;
import vn.edu.ueh.rtmiue20.project.data.model.Request;

public class NotificationRepository {

    private static final String TAG = "NotificationRepo";
    private static final String COLLECTION_NOTIFICATIONS = "notifications";

    private static NotificationRepository instance;
    private final FirebaseFirestore db;
    private final CollectionReference notificationsRef;

    private ListenerRegistration activeListener;
    private String activeListeningUserId;
    private final MutableLiveData<List<NotificationItem>> notificationsLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Integer> unreadCountLiveData = new MutableLiveData<>(0);

    public static synchronized NotificationRepository getInstance() {
        if (instance == null) {
            instance = new NotificationRepository();
        }
        return instance;
    }

    private NotificationRepository() {
        db = FirebaseFirestore.getInstance();
        notificationsRef = db.collection(COLLECTION_NOTIFICATIONS);
    }

    /**
     * Lắng nghe Realtime toàn bộ thông báo của người dùng từ Firestore.
     * Tự động tính toán số lượng thông báo chưa đọc (Unread Count).
     */
    public synchronized LiveData<List<NotificationItem>> getNotifications(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            notificationsLiveData.setValue(new ArrayList<>());
            unreadCountLiveData.setValue(0);
            return notificationsLiveData;
        }

        if (userId.equals(activeListeningUserId) && activeListener != null) {
            return notificationsLiveData;
        }

        if (activeListener != null) {
            activeListener.remove();
            activeListener = null;
        }
        activeListeningUserId = userId;

        // Query Firestore: whereEqualTo("userId", userId)
        activeListener = notificationsRef
                .whereEqualTo("userId", userId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Lỗi lắng nghe thông báo Firestore: " + error.getMessage());
                        return;
                    }

                    List<NotificationItem> list = new ArrayList<>();
                    int unread = 0;

                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            try {
                                NotificationItem item = doc.toObject(NotificationItem.class);
                                if (item != null) {
                                    item.setId(doc.getId());
                                    list.add(item);
                                    if (!item.isRead()) {
                                        unread++;
                                    }
                                }
                            } catch (Exception ex) {
                                Log.e(TAG, "Lỗi parse NotificationItem: " + ex.getMessage());
                            }
                        }
                    }

                    // Sắp xếp: Ưu tiên thông báo cần hành động (actionRequired) và thông báo mới nhất
                    Collections.sort(list, (n1, n2) -> {
                        if (n1.isActionRequired() != n2.isActionRequired()) {
                            return n1.isActionRequired() ? -1 : 1;
                        }
                        return Long.compare(n2.getTimestamp(), n1.getTimestamp());
                    });

                    notificationsLiveData.setValue(list);
                    unreadCountLiveData.setValue(unread);
                });

        return notificationsLiveData;
    }

    public LiveData<Integer> getUnreadCount(String userId) {
        getNotifications(userId);
        return unreadCountLiveData;
    }

    /**
     * Tạo và lưu một thông báo mới lên Firestore.
     */
    public void sendNotification(NotificationItem item) {
        if (item == null || item.getUserId() == null || item.getUserId().trim().isEmpty()) {
            return;
        }

        DocumentReference docRef;
        if (item.getId() != null && !item.getId().trim().isEmpty()) {
            docRef = notificationsRef.document(item.getId());
        } else {
            docRef = notificationsRef.document();
            item.setId(docRef.getId());
        }

        if (item.getTimestamp() <= 0) {
            item.setTimestamp(System.currentTimeMillis());
        }

        docRef.set(item)
                .addOnSuccessListener(unused -> Log.d(TAG, "Lưu thông báo Firestore thành công: " + item.getId()))
                .addOnFailureListener(e -> Log.e(TAG, "Lỗi lưu thông báo Firestore: " + e.getMessage()));
    }

    /**
     * Đánh dấu một thông báo đã được đọc (isRead = true).
     */
    public void markAsRead(String notificationId) {
        if (notificationId == null || notificationId.trim().isEmpty()) return;
        notificationsRef.document(notificationId)
                .update("read", true, "actionRequired", false)
                .addOnFailureListener(e -> Log.w(TAG, "Lỗi cập nhật trạng thái đã đọc: " + e.getMessage()));
    }

    /**
     * Đánh dấu tất cả thông báo của người dùng là đã đọc.
     */
    public void markAllAsRead(String userId) {
        if (userId == null || userId.trim().isEmpty()) return;
        notificationsRef.whereEqualTo("userId", userId)
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots == null || snapshots.isEmpty()) return;
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        batch.update(doc.getReference(), "read", true, "actionRequired", false);
                    }
                    batch.commit().addOnFailureListener(e -> Log.e(TAG, "Lỗi batch markAllAsRead: " + e.getMessage()));
                });
    }

    /**
     * Xóa một thông báo.
     */
    public void deleteNotification(String notificationId) {
        if (notificationId == null || notificationId.trim().isEmpty()) return;
        notificationsRef.document(notificationId).delete()
                .addOnFailureListener(e -> Log.e(TAG, "Lỗi xóa thông báo: " + e.getMessage()));
    }

    // --- CÁC HÀM TIỆN ÍCH TẠO THÔNG BÁO CHO CÁC HÀNH ĐỘNG NGHIỆP VỤ ---

    public void createRequestReceivedNotification(Request request) {
        if (request == null || request.getOwnerId() == null || request.getOwnerId().isEmpty()) return;
        String rName = request.getRequesterName() != null ? request.getRequesterName() : "Một bạn đọc";
        String bTitle = request.getBookTitle() != null ? request.getBookTitle() : "sách";
        String avatar = request.getRequesterAvatar() != null ? request.getRequesterAvatar() : request.getBookImageUrl();

        NotificationItem item = new NotificationItem(
                "req_recv_" + request.getRequestId(),
                request.getOwnerId(),
                request.getRequesterId(),
                rName,
                NotificationItem.TYPE_REQUEST_RECEIVED,
                "Yêu cầu nhận sách mới",
                rName + " đã gửi yêu cầu nhận cuốn \"" + bTitle + "\"",
                request.getMessage() != null && !request.getMessage().trim().isEmpty() ? "\"" + request.getMessage().trim() + "\"" : null,
                request.getTimestamp() > 0 ? request.getTimestamp() : System.currentTimeMillis(),
                avatar,
                "Chờ phản hồi",
                true,
                false,
                "REQUESTS",
                "received",
                request.getRequestId(),
                request.getBookId()
        );
        sendNotification(item);
    }

    public void createRequestAcceptedNotification(Request request) {
        if (request == null || request.getRequesterId() == null || request.getRequesterId().isEmpty()) return;
        String oName = request.getOwnerName() != null ? request.getOwnerName() : "Chủ sách";
        String bTitle = request.getBookTitle() != null ? request.getBookTitle() : "sách";

        NotificationItem item = new NotificationItem(
                "req_acc_" + request.getRequestId(),
                request.getRequesterId(),
                request.getOwnerId(),
                oName,
                NotificationItem.TYPE_REQUEST_ACCEPTED,
                "Yêu cầu được chấp nhận! 🎉",
                oName + " đã đồng ý tặng cuốn \"" + bTitle + "\" cho bạn. Hãy liên hệ hẹn nhận sách!",
                "Điểm hẹn: Đại học Kinh Tế TP.HCM (UEH Cơ sở B)",
                System.currentTimeMillis(),
                request.getBookImageUrl(),
                "Đã chấp nhận",
                true,
                false,
                "REQUESTS",
                "sent",
                request.getRequestId(),
                request.getBookId()
        );
        sendNotification(item);
    }

    public void createRequestRejectedNotification(Request request) {
        if (request == null || request.getRequesterId() == null || request.getRequesterId().isEmpty()) return;
        String oName = request.getOwnerName() != null ? request.getOwnerName() : "Chủ sách";
        String bTitle = request.getBookTitle() != null ? request.getBookTitle() : "sách";

        NotificationItem item = new NotificationItem(
                "req_rej_" + request.getRequestId(),
                request.getRequesterId(),
                request.getOwnerId(),
                oName,
                NotificationItem.TYPE_REQUEST_REJECTED,
                "Yêu cầu không được duyệt",
                "Yêu cầu nhận cuốn \"" + bTitle + "\" của bạn không được chủ sách chấp thuận.",
                null,
                System.currentTimeMillis(),
                request.getBookImageUrl(),
                "Đã từ chối",
                false,
                false,
                "REQUESTS",
                "sent",
                request.getRequestId(),
                request.getBookId()
        );
        sendNotification(item);
    }

    public void createRequestCompletedNotification(Request request, boolean isForOwner) {
        if (request == null) return;
        String targetUserId = isForOwner ? request.getOwnerId() : request.getRequesterId();
        if (targetUserId == null || targetUserId.isEmpty()) return;

        String senderId = isForOwner ? request.getRequesterId() : request.getOwnerId();
        String senderName = isForOwner ? request.getRequesterName() : request.getOwnerName();
        String bTitle = request.getBookTitle() != null ? request.getBookTitle() : "sách";

        String title = isForOwner ? "Giao dịch hoàn tất!" : "Đã nhận sách thành công ✅";
        String message = isForOwner
                ? "Giao dịch tặng cuốn \"" + bTitle + "\" cho " + senderName + " đã hoàn tất thành công."
                : "Bạn đã nhận thành công cuốn \"" + bTitle + "\" từ " + senderName + ".";
        String subMsg = !isForOwner && !request.isRated() ? "Chạm để đánh giá uy tín cho chủ sách" : null;

        NotificationItem item = new NotificationItem(
                "req_comp_" + (isForOwner ? "o_" : "r_") + request.getRequestId(),
                targetUserId,
                senderId,
                senderName,
                NotificationItem.TYPE_REQUEST_COMPLETED,
                title,
                message,
                subMsg,
                request.getCompletedTimestamp() > 0 ? request.getCompletedTimestamp() : System.currentTimeMillis(),
                request.getBookImageUrl(),
                isForOwner ? "Hoàn tất" : "Đã nhận",
                !isForOwner && !request.isRated(),
                false,
                "REQUESTS",
                isForOwner ? "received" : "sent",
                request.getRequestId(),
                request.getBookId()
        );
        sendNotification(item);
    }

    public void createCommentNotification(String targetUserId, String bookId, String bookTitle, String commenterId, String commenterName, String commentContent) {
        if (targetUserId == null || targetUserId.isEmpty() || targetUserId.equals(commenterId)) return;
        String bTitle = bookTitle != null ? bookTitle : "sách của bạn";
        String cName = commenterName != null ? commenterName : "Một bạn đọc";

        NotificationItem item = new NotificationItem(
                "cmt_" + System.currentTimeMillis() + "_" + (commenterId != null ? commenterId.substring(0, Math.min(5, commenterId.length())) : "x"),
                targetUserId,
                commenterId,
                cName,
                NotificationItem.TYPE_NEW_COMMENT,
                "Bình luận mới về sách 💬",
                cName + " đã bình luận về cuốn \"" + bTitle + "\"",
                commentContent != null ? "\"" + commentContent + "\"" : null,
                System.currentTimeMillis(),
                null,
                "Bình luận",
                true,
                false,
                "BOOK_DETAIL",
                "",
                null,
                bookId
        );
        sendNotification(item);
    }
}
