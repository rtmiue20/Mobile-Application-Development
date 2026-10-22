package vn.edu.ueh.rtmiue20.project.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import vn.edu.ueh.rtmiue20.project.data.model.Comment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommentRepository {

    private static final String COLLECTION_BOOKS = "books";
    private static final String COLLECTION_COMMENTS = "comments";

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

    // 1. READ: Lắng nghe realtime các bình luận
    public LiveData<List<Comment>> getComments(String bookId) {
        removeListener();

        commentsRegistration = db.collection(COLLECTION_BOOKS)
                .document(bookId)
                .collection(COLLECTION_COMMENTS)
                .orderBy("createdAt", Query.Direction.ASCENDING)
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
                                Comment comment = doc.toObject(Comment.class);
                                if (comment != null) {
                                    comment.setCommentId(doc.getId());
                                    list.add(comment);
                                }
                            }
                        }
                        commentsLiveData.setValue(list);
                    }
                });

        return commentsLiveData;
    }

    // 2. CREATE: Thêm bình luận
    public void addComment(String bookId, Comment comment) {
        db.collection(COLLECTION_BOOKS)
                .document(bookId)
                .collection(COLLECTION_COMMENTS)
                .add(comment)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        addSuccessLiveData.setValue(true);

                        // --- GỌI FCM: TÌM CHỦ SÁCH -> TÌM TOKEN -> BẮN THÔNG BÁO ---
                        db.collection(COLLECTION_BOOKS).document(bookId).get()
                                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                    @Override
                                    public void onSuccess(DocumentSnapshot bookSnap) {
                                        if (bookSnap.exists() && bookSnap.contains("ownerId")) {
                                            String ownerId = bookSnap.getString("ownerId");
                                            // Đảm bảo không tự gửi thông báo cho chính mình
                                            if (ownerId != null && !ownerId.equals(comment.getUserId())) {
                                                db.collection("users").document(ownerId).get()
                                                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                            @Override
                                                            public void onSuccess(DocumentSnapshot userSnap) {
                                                                if (userSnap.exists() && userSnap.contains("fcmToken")) {
                                                                    String token = userSnap.getString("fcmToken");
                                                                    if (token != null && !token.isEmpty()) {
                                                                        vn.edu.ueh.rtmiue20.project.util.FCMSender.pushNotification(
                                                                                token,
                                                                                "Bình luận mới",
                                                                                comment.getUserName() + " đã bình luận: " + comment.getContent()
                                                                        );
                                                                    }
                                                                }
                                                            }
                                                        });
                                            }
                                        }
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        errorLiveData.setValue("Gửi bình luận thất bại: " + e.getMessage());
                    }
                });
    }

    // 3. UPDATE: Chỉnh sửa bình luận
    public void updateComment(String bookId, String commentId, String newContent) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("content", newContent);
        updates.put("updatedAt", System.currentTimeMillis());

        db.collection(COLLECTION_BOOKS)
                .document(bookId)
                .collection(COLLECTION_COMMENTS)
                .document(commentId)
                .update(updates)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        updateSuccessLiveData.setValue(true);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        errorLiveData.setValue("Sửa bình luận thất bại: " + e.getMessage());
                    }
                });
    }

    // 4. DELETE: Xóa bình luận
    public void deleteComment(String bookId, String commentId) {
        db.collection(COLLECTION_BOOKS)
                .document(bookId)
                .collection(COLLECTION_COMMENTS)
                .document(commentId)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        deleteSuccessLiveData.setValue(true);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        errorLiveData.setValue("Xóa bình luận thất bại: " + e.getMessage());
                    }
                });
    }

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