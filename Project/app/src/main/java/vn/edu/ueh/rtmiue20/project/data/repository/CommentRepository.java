package vn.edu.ueh.rtmiue20.project.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import vn.edu.ueh.rtmiue20.project.data.model.Comment;
import vn.edu.ueh.rtmiue20.project.util.FCMSender;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommentRepository {

    // Bảng comments là collection độc lập ở tầng root (cùng cấp với books, favorites, requests, users)
    private static final String COLLECTION_COMMENTS = "comments";
    private static final String COLLECTION_BOOKS = "books";

    private final FirebaseFirestore db;
    private ListenerRegistration commentsRegistration;

    private final MutableLiveData<List<Comment>> commentsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> addSuccessLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> updateSuccessLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> deleteSuccessLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    public CommentRepository() {
        db = FirebaseFirestore.getInstance();
    }

    // 1. READ: Lắng nghe realtime các bình luận có khóa ngoại bookId bằng id sách tương ứng
    public LiveData<List<Comment>> getComments(String bookId) {
        removeListener();

        if (bookId == null || bookId.trim().isEmpty()) {
            commentsLiveData.setValue(new ArrayList<>());
            return commentsLiveData;
        }

        // Truy vấn collection root comments theo khóa ngoại bookId
        commentsRegistration = db.collection(COLLECTION_COMMENTS)
                .whereEqualTo("bookId", bookId)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            errorLiveData.setValue("Không thể tải bình luận: " + error.getMessage());
                            return;
                        }

                        List<Comment> list = new ArrayList<>();
                        if (value != null) {
                            for (DocumentSnapshot doc : value.getDocuments()) {
                                try {
                                    Comment comment = doc.toObject(Comment.class);
                                    if (comment != null) {
                                        comment.setCommentId(doc.getId());
                                        list.add(comment);
                                    }
                                } catch (Exception e) {
                                    android.util.Log.e("CommentRepository", "Lỗi parse comment: " + e.getMessage());
                                }
                            }
                        }

                        // Sắp xếp tăng dần theo thời gian tạo (createdAt ASC) trên client
                        // Tránh phụ thuộc composite index trên Firestore
                        list.sort((c1, c2) -> {
                            long t1 = c1.getCreatedAt() != null ? c1.getCreatedAt() : 0L;
                            long t2 = c2.getCreatedAt() != null ? c2.getCreatedAt() : 0L;
                            return Long.compare(t1, t2);
                        });

                        commentsLiveData.setValue(list);
                    }
                });

        return commentsLiveData;
    }

    // 2. CREATE: Thêm bình luận vào root collection comments có khóa ngoại bookId
    public void addComment(String bookId, Comment comment) {
        if (bookId == null || bookId.trim().isEmpty() || comment == null) {
            errorLiveData.setValue("Dữ liệu bình luận không hợp lệ");
            return;
        }

        comment.setBookId(bookId);

        Map<String, Object> data = new HashMap<>();
        data.put("bookId", bookId); // Khóa ngoại REF tới document trong collection books
        data.put("userId", comment.getUserId());
        data.put("userName", comment.getUserName() != null ? comment.getUserName() : "Người dùng");
        data.put("userAvatar", comment.getUserAvatar() != null ? comment.getUserAvatar() : "");
        data.put("content", comment.getContent());
        data.put("createdAt", comment.getCreatedAt() != null ? comment.getCreatedAt() : System.currentTimeMillis());
        data.put("updatedAt", null);

        db.collection(COLLECTION_COMMENTS)
                .add(data)
                .addOnSuccessListener(documentReference -> {
                    addSuccessLiveData.setValue(true);

                    // Gửi thông báo FCM tới chủ sách (nếu người bình luận không phải chủ sách)
                    sendCommentNotificationToBookOwner(bookId, comment);
                })
                .addOnFailureListener(e -> {
                    errorLiveData.setValue("Gửi bình luận thất bại: " + e.getLocalizedMessage());
                });
    }

    // Gửi FCM notification tới chủ sách
    private void sendCommentNotificationToBookOwner(String bookId, Comment comment) {
        db.collection(COLLECTION_BOOKS).document(bookId).get()
                .addOnSuccessListener(bookSnap -> {
                    if (bookSnap != null && bookSnap.exists() && bookSnap.contains("ownerId")) {
                        String ownerId = bookSnap.getString("ownerId");
                        String bookTitle = bookSnap.getString("title");
                        if (bookTitle == null) bookTitle = "sách của bạn";
                        if (ownerId != null && !ownerId.equals(comment.getUserId())) {
                            // Tạo Notification Document trong Firestore cho chủ sách
                            NotificationRepository.getInstance().createCommentNotification(
                                    ownerId,
                                    bookId,
                                    bookTitle,
                                    comment.getUserId(),
                                    comment.getUserName(),
                                    comment.getContent()
                            );

                            final String title = bookTitle;
                            db.collection("users").document(ownerId).get()
                                    .addOnSuccessListener(userSnap -> {
                                        if (userSnap != null && userSnap.exists() && userSnap.contains("fcmToken")) {
                                            String token = userSnap.getString("fcmToken");
                                            if (token != null && !token.isEmpty()) {
                                                FCMSender.pushNotification(
                                                        token,
                                                        "Bình luận mới về: " + title,
                                                        comment.getUserName() + ": " + comment.getContent()
                                                );
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    // 3. UPDATE: Chỉnh sửa bình luận trong root collection comments
    public void updateComment(String bookId, String commentId, String newContent) {
        if (commentId == null || newContent == null) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put("content", newContent.trim());
        updates.put("updatedAt", System.currentTimeMillis());

        db.collection(COLLECTION_COMMENTS)
                .document(commentId)
                .update(updates)
                .addOnSuccessListener(unused -> updateSuccessLiveData.setValue(true))
                .addOnFailureListener(e -> errorLiveData.setValue("Sửa bình luận thất bại: " + e.getLocalizedMessage()));
    }

    // 4. DELETE: Xóa bình luận khỏi root collection comments
    public void deleteComment(String bookId, String commentId) {
        if (commentId == null) return;
        db.collection(COLLECTION_COMMENTS)
                .document(commentId)
                .delete()
                .addOnSuccessListener(unused -> deleteSuccessLiveData.setValue(true))
                .addOnFailureListener(e -> errorLiveData.setValue("Xóa bình luận thất bại: " + e.getLocalizedMessage()));
    }

    public LiveData<List<Comment>> getCommentsLiveData() { return commentsLiveData; }
    public LiveData<Boolean> getAddSuccess() { return addSuccessLiveData; }
    public LiveData<Boolean> getUpdateSuccess() { return updateSuccessLiveData; }
    public LiveData<Boolean> getDeleteSuccess() { return deleteSuccessLiveData; }
    public LiveData<String> getError() { return errorLiveData; }

    public void resetStatus() {
        addSuccessLiveData.setValue(null);
        updateSuccessLiveData.setValue(null);
        deleteSuccessLiveData.setValue(null);
        errorLiveData.setValue(null);
    }

    public void removeListener() {
        if (commentsRegistration != null) {
            commentsRegistration.remove();
            commentsRegistration = null;
        }
    }
}