package vn.edu.ueh.rtmiue20.project.data.repository;

import android.app.Application;

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
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vn.edu.ueh.rtmiue20.project.data.local.BookRoomDatabase;
import vn.edu.ueh.rtmiue20.project.data.local.FavoriteDao;
import vn.edu.ueh.rtmiue20.project.data.model.Book;
import vn.edu.ueh.rtmiue20.project.data.model.Favorite;

public class FavoriteRepository {

    private static final String COLLECTION_FAVORITES = "favorites";

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private final FavoriteDao favoriteDao;
    private ListenerRegistration favoriteRegistration;
    private ListenerRegistration listRegistration;

    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final MutableLiveData<Boolean> isFavoritedLiveData = new MutableLiveData<Boolean>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<String>();
    private boolean isToggling = false;

    // CẦN CÓ APPLICATION ĐỂ GỌI ROOM
    public FavoriteRepository(Application application) {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        favoriteDao = BookRoomDatabase.getDatabase(application).favoriteDao();
    }

    private String buildFavoriteId(String userId, String bookId) {
        return userId + "_" + bookId;
    }

    private DocumentReference getFavoriteRef(String userId, String bookId) {
        return db.collection(COLLECTION_FAVORITES).document(buildFavoriteId(userId, bookId));
    }

    // --- LOGIC OFFLINE ROOM ---

    public LiveData<List<Book>> getFavoritedBooksOffline() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            return favoriteDao.getFavoritedBooksByUser(user.getUid());
        }
        return new MutableLiveData<List<Book>>();
    }

    public void startSyncFavorites() {
        final FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;
        if (listRegistration != null) return;

        listRegistration = db.collection(COLLECTION_FAVORITES)
                .whereEqualTo("userId", user.getUid())
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null || value == null) return;

                        final List<Favorite> list = new ArrayList<>();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Favorite fav = doc.toObject(Favorite.class);
                            if (fav != null) {
                                fav.setFavoriteId(doc.getId());
                                list.add(fav);
                            }
                        }

                        executorService.execute(new Runnable() {
                            @Override
                            public void run() {
                                favoriteDao.deleteAllByUser(user.getUid());
                                favoriteDao.insertAll(list);
                            }
                        });
                    }
                });
    }

    public void stopSyncFavorites() {
        if (listRegistration != null) {
            listRegistration.remove();
            listRegistration = null;
        }
    }

    // --- LOGIC ONLINE FIRESTORE ---

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
                    public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            errorLiveData.setValue("Không thể tải trạng thái yêu thích");
                            return;
                        }
                        boolean exists = snapshot != null && snapshot.exists();
                        isFavoritedLiveData.setValue(exists);
                    }
                });

        return isFavoritedLiveData;
    }

    public void toggleFavorite(final String bookId) {
        final FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            errorLiveData.setValue("Bạn cần đăng nhập để lưu sách");
            return;
        }

        if (isToggling) return;
        isToggling = true;

        final String userId = user.getUid();
        final String favoriteId = buildFavoriteId(userId, bookId);
        final DocumentReference favoriteRef = getFavoriteRef(userId, bookId);

        favoriteRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot snapshot) {
                if (snapshot.exists()) {
                    favoriteRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            executorService.execute(new Runnable() {
                                @Override
                                public void run() {
                                    favoriteDao.deleteById(favoriteId);
                                }
                            });
                            isToggling = false;
                        }
                    }).addOnFailureListener(createFailureListener("Bỏ yêu thích thất bại: "));
                } else {
                    final Favorite favorite = new Favorite(favoriteId, userId, bookId, System.currentTimeMillis());
                    favoriteRef.set(favorite).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            executorService.execute(new Runnable() {
                                @Override
                                public void run() {
                                    favoriteDao.insert(favorite);
                                }
                            });
                            isToggling = false;
                        }
                    }).addOnFailureListener(createFailureListener("Lưu yêu thích thất bại: "));
                }
            }
        }).addOnFailureListener(createFailureListener("Lỗi kiểm tra trạng thái: "));
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

    public LiveData<String> getError() { return errorLiveData; }
    public void resetError() { errorLiveData.setValue(null); }

    public void removeListener() {
        if (favoriteRegistration != null) {
            favoriteRegistration.remove();
            favoriteRegistration = null;
        }
    }
}