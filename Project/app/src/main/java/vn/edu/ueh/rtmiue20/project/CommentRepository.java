package vn.edu.ueh.rtmiue20.project;

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

import java.util.ArrayList;
import java.util.List;

public class CommentRepository {

    private static final String COLLECTION_BOOKS = "books";
    private static final String COLLECTION_COMMENTS = "comments";

    private final FirebaseFirestore db;
    private ListenerRegistration commentsRegistration;

    private final MutableLiveData<List<Comment>> commentsLiveData = new MutableLiveData<List<Comment>>();
    private final MutableLiveData<Boolean> addSuccessLiveData = new MutableLiveData<Boolean>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<String>();

    public CommentRepository() {
        db = FirebaseFirestore.getInstance();
    }

    // Lang nghe realtime cac binh luan cua mot cuon sach
    public LiveData<List<Comment>> getComments(String bookId) {
        removeListener();

        commentsRegistration = db.collection(COLLECTION_BOOKS)
                .document(bookId)
                .collection(COLLECTION_COMMENTS)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value,
                                        @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            errorLiveData.setValue("Không thể tải bình luận: " + error.getMessage());
                            return;
                        }

                        List<Comment> list = new ArrayList<Comment>();
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

    // Them mot binh luan moi vao books/{bookId}/comments
    public void addComment(String bookId, Comment comment) {
        db.collection(COLLECTION_BOOKS)
                .document(bookId)
                .collection(COLLECTION_COMMENTS)
                .add(comment)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        addSuccessLiveData.setValue(true);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        errorLiveData.setValue("Gửi bình luận thất bại: " + e.getMessage());
                    }
                });
    }

    public LiveData<Boolean> getAddSuccess() {
        return addSuccessLiveData;
    }

    public LiveData<String> getError() {
        return errorLiveData;
    }

    public void resetAddSuccess() {
        addSuccessLiveData.setValue(null);
    }

    public void resetError() {
        errorLiveData.setValue(null);
    }

    public void removeListener() {
        if (commentsRegistration != null) {
            commentsRegistration.remove();
            commentsRegistration = null;
        }
    }
}