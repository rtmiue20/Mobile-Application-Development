package vn.edu.ueh.rtmiue20.project.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;

import vn.edu.ueh.rtmiue20.project.data.model.Favorite;

public class FavoriteRepository {

    private static final String COLLECTION_FAVORITES = "favorites";

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private ListenerRegistration favoriteRegistration;

    private final MutableLiveData<Boolean> isFavoritedLiveData = new MutableLiveData<Boolean>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<String>();

    // Chong bam lien tuc trong luc request truoc chua xong
    private boolean isToggling = false;

    public FavoriteRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    // Document id = {userId}_{bookId} de dam bao moi user chi yeu thich mot sach mot lan
    private String buildFavoriteId(String userId, String bookId) {
        return userId + "_" + bookId;
    }

    private DocumentReference getFavoriteRef(String userId, String bookId) {
        return db.collection(COLLECTION_FAVORITES).document(buildFavoriteId(userId, bookId));
    }

    // Kiem tra realtime user hien tai da yeu thich sach nay chua
    public LiveData<Boolean> checkIsFavorited(String bookId) {
        removeListener();

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            isFavoritedLiveData.setValue(false);
            return isFavoritedLiveData;
        }

        favoriteRegistration = getFavoriteRef(user.getUid(), bookId)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot snapshot,
                                        @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            errorLiveData.setValue("Không thể tải trạng thái yêu thích: " + error.getMessage());
                            return;
                        }
                        boolean exists = snapshot != null && snapshot.exists();
                        isFavoritedLiveData.setValue(exists);
                    }
                });

        return isFavoritedLiveData;
    }

    // Chua yeu thich thi them, da yeu thich thi xoa
    public void toggleFavorite(final String bookId) {
        final FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            errorLiveData.setValue("Bạn cần đăng nhập để lưu sách yêu thích");
            return;
        }

        if (isToggling) {
            return;
        }
        isToggling = true;

        final String userId = user.getUid();
        final String favoriteId = buildFavoriteId(userId, bookId);
        final DocumentReference favoriteRef = getFavoriteRef(userId, bookId);

        favoriteRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot snapshot) {
                if (snapshot.exists()) {
                    favoriteRef.delete()
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    isToggling = false;
                                }
                            })
                            .addOnFailureListener(createFailureListener("Bỏ yêu thích thất bại: "));
                } else {
                    Favorite favorite = new Favorite(
                            favoriteId, userId, bookId, System.currentTimeMillis());
                    favoriteRef.set(favorite)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    isToggling = false;
                                }
                            })
                            .addOnFailureListener(createFailureListener("Lưu yêu thích thất bại: "));
                }
            }
        }).addOnFailureListener(createFailureListener("Không thể kiểm tra trạng thái: "));
    }

    private OnFailureListener createFailureListener(final String prefix) {
        return new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                isToggling = false;
                errorLiveData.setValue(prefix + e.getMessage());
            }
        };
    }

    public LiveData<String> getError() {
        return errorLiveData;
    }

    public void resetError() {
        errorLiveData.setValue(null);
    }

    public void removeListener() {
        if (favoriteRegistration != null) {
            favoriteRegistration.remove();
            favoriteRegistration = null;
        }
    }
}
