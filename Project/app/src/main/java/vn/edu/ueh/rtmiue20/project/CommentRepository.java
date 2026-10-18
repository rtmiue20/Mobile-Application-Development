package vn.edu.ueh.rtmiue20.project;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

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
                .addSnapshotListener((value, error) -> {
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
                });

        return commentsLiveData;
    }

    // 2. CREATE: Thêm bình luận
    public void addComment(String bookId, Comment comment) {
        db.collection(COLLECTION_BOOKS)
                .document(bookId)
                .collection(COLLECTION_COMMENTS)
                .add(comment)
                .addOnSuccessListener(documentReference -> addSuccessLiveData.setValue(true))
                .addOnFailureListener(e -> errorLiveData.setValue("Gửi bình luận thất bại: " + e.getMessage()));
    }

    // 3. UPDATE: Chỉnh sửa bình luận (Thêm mới U)
    public void updateComment(String bookId, String commentId, String newContent) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("content", newContent);
        updates.put("updatedAt", System.currentTimeMillis());

        db.collection(COLLECTION_BOOKS)
                .document(bookId)
                .collection(COLLECTION_COMMENTS)
                .document(commentId)
                .update(updates)
                .addOnSuccessListener(unused -> updateSuccessLiveData.setValue(true))
                .addOnFailureListener(e -> errorLiveData.setValue("Sửa bình luận thất bại: " + e.getMessage()));
    }

    // 4. DELETE: Xóa bình luận (Thêm mới D)
    public void deleteComment(String bookId, String commentId) {
        db.collection(COLLECTION_BOOKS)
                .document(bookId)
                .collection(COLLECTION_COMMENTS)
                .document(commentId)
                .delete()
                .addOnSuccessListener(unused -> deleteSuccessLiveData.setValue(true))
                .addOnFailureListener(e -> errorLiveData.setValue("Xóa bình luận thất bại: " + e.getMessage()));
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