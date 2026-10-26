package vn.edu.ueh.rtmiue20.project.data.repository;

import android.app.Application;
import android.util.Log;

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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vn.edu.ueh.rtmiue20.project.data.local.BookRoomDatabase;
import vn.edu.ueh.rtmiue20.project.data.local.FavoriteDao;
import vn.edu.ueh.rtmiue20.project.data.model.Book;
import vn.edu.ueh.rtmiue20.project.data.model.Favorite;

public class FavoriteRepository {

    private static final String COLLECTION_FAVORITES = "favorites";
    private static volatile FavoriteRepository instance;

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private final BookRoomDatabase database;
    private final FavoriteDao favoriteDao;
    private ListenerRegistration favoriteRegistration;
    private ListenerRegistration listRegistration;
    private String currentListeningUserId = null;

    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final MutableLiveData<Boolean> isFavoritedLiveData = new MutableLiveData<Boolean>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<String>();
    private final Set<String> togglingBookIds = Collections.synchronizedSet(new HashSet<>());

    public static FavoriteRepository getInstance(Application application) {
        if (instance == null) {
            synchronized (FavoriteRepository.class) {
                if (instance == null) {
                    instance = new FavoriteRepository(application);
                }
            }
        }
        return instance;
    }

    public FavoriteRepository(Application application) {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        database = BookRoomDatabase.getDatabase(application);
        favoriteDao = database.favoriteDao();
    }

    private String buildFavoriteId(String userId, String bookId) {
        return userId + "_" + bookId;
    }

    private DocumentReference getFavoriteRef(String userId, String bookId) {
        return db.collection(COLLECTION_FAVORITES).document(buildFavoriteId(userId, bookId));
    }

    public LiveData<List<Book>> getFavoritedBooksOffline() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            return favoriteDao.getFavoritedBooksByUser(user.getUid());
        }
        return new MutableLiveData<List<Book>>();
    }

    public LiveData<List<String>> getFavoriteBookIds() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            return favoriteDao.getFavoriteBookIdsByUser(user.getUid());
        }
        return new MutableLiveData<List<String>>(new ArrayList<String>());
    }

    public void startSyncFavorites() {
        final FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;
        if (listRegistration != null && user.getUid().equals(currentListeningUserId)) return;
        if (listRegistration != null) {
            listRegistration.remove();
            listRegistration = null;
        }
        currentListeningUserId = user.getUid();

        listRegistration = db.collection(COLLECTION_FAVORITES)
                .whereEqualTo("userId", user.getUid())
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null || value == null) return;

                        final List<Favorite> list = new ArrayList<>();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            try {
                                Favorite fav = doc.toObject(Favorite.class);
                                if (fav != null) {
                                    fav.setFavoriteId(doc.getId());
                                    list.add(fav);

                                    // Đảm bảo sách có trong Room để INNER JOIN không bị miss
                                    final String bId = fav.getBookId();
                                    if (bId != null && !bId.isEmpty()) {
                                        db.collection("books").document(bId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                            @Override
                                            public void onSuccess(DocumentSnapshot bDoc) {
                                                if (bDoc != null && bDoc.exists()) {
                                                    try {
                                                        final Book b = bDoc.toObject(Book.class);
                                                        if (b != null) {
                                                            b.setId(bDoc.getId());
                                                            if (bDoc.contains("viewCount")) {
                                                                Long v = bDoc.getLong("viewCount");
                                                                if (v != null) b.setViewCount(v.intValue());
                                                            }
                                                            if (bDoc.contains("favoriteCount")) {
                                                                Long f = bDoc.getLong("favoriteCount");
                                                                if (f != null) b.setFavoriteCount(f.intValue());
                                                            }
                                                            executorService.execute(new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                    database.bookDao().insert(b);
                                                                }
                                                            });
                                                        }
                                                    } catch (Exception ex) {
                                                        Log.e("FirebaseSync", "Lỗi parse Book cho Favorite: " + ex.getMessage());
                                                    }
                                                }
                                            }
                                        });
                                    }
                                }
                            } catch (Exception e) {
                                Log.e("FirebaseSync", "Lỗi parse Favorite: " + e.getMessage());
                            }
                        }

                        executorService.execute(new Runnable() {
                            @Override
                            public void run() {
                                database.runInTransaction(new Runnable() {
                                    @Override
                                    public void run() {
                                        favoriteDao.deleteAllByUser(user.getUid());
                                        favoriteDao.insertAll(list);
                                    }
                                });
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
        currentListeningUserId = null;
    }

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
            errorLiveData.postValue("Bạn cần đăng nhập để lưu sách");
            return;
        }

        if (bookId == null || bookId.trim().isEmpty()) return;

        if (togglingBookIds.contains(bookId)) return;
        togglingBookIds.add(bookId);

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
                            final DocumentReference bookRef = db.collection("books").document(bookId);
                            bookRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                @Override
                                public void onSuccess(DocumentSnapshot bSnap) {
                                    long current = 0;
                                    if (bSnap != null && bSnap.exists() && bSnap.contains("favoriteCount")) {
                                        Long val = bSnap.getLong("favoriteCount");
                                        if (val != null) current = Math.max(0, val);
                                    }
                                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                                    map.put("favoriteCount", Math.max(0, current - 1));
                                    bookRef.set(map, com.google.firebase.firestore.SetOptions.merge());
                                }
                            });
                            executorService.execute(new Runnable() {
                                @Override
                                public void run() {
                                    favoriteDao.deleteById(favoriteId);
                                    database.bookDao().decrementFavoriteCount(bookId);
                                }
                            });
                            togglingBookIds.remove(bookId);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            togglingBookIds.remove(bookId);
                            errorLiveData.postValue("Bỏ yêu thích thất bại: " + e.getMessage());
                        }
                    });
                } else {
                    final Favorite favorite = new Favorite(favoriteId, userId, bookId, System.currentTimeMillis());
                    favoriteRef.set(favorite).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            final DocumentReference bookRef = db.collection("books").document(bookId);
                            bookRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                @Override
                                public void onSuccess(DocumentSnapshot bSnap) {
                                    long current = 0;
                                    if (bSnap != null && bSnap.exists() && bSnap.contains("favoriteCount")) {
                                        Long val = bSnap.getLong("favoriteCount");
                                        if (val != null) current = Math.max(0, val);
                                    }
                                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                                    map.put("favoriteCount", current + 1);
                                    bookRef.set(map, com.google.firebase.firestore.SetOptions.merge());
                                }
                            });
                            executorService.execute(new Runnable() {
                                @Override
                                public void run() {
                                    favoriteDao.insert(favorite);
                                    database.bookDao().incrementFavoriteCount(bookId);
                                }
                            });
                            // Tải chi tiết sách để đảm bảo Room có dữ liệu
                            db.collection("books").document(bookId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                @Override
                                public void onSuccess(DocumentSnapshot bDoc) {
                                    if (bDoc != null && bDoc.exists()) {
                                        try {
                                            final Book b = bDoc.toObject(Book.class);
                                            if (b != null) {
                                                b.setId(bDoc.getId());
                                                if (bDoc.contains("viewCount")) {
                                                    Long v = bDoc.getLong("viewCount");
                                                    if (v != null) b.setViewCount(v.intValue());
                                                }
                                                if (bDoc.contains("favoriteCount")) {
                                                    Long f = bDoc.getLong("favoriteCount");
                                                    if (f != null) b.setFavoriteCount(f.intValue());
                                                }
                                                executorService.execute(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        database.bookDao().insert(b);
                                                    }
                                                });
                                            }
                                        } catch (Exception ex) {
                                            Log.e("FavoriteSync", "Lỗi cache book: " + ex.getMessage());
                                        }
                                    }
                                }
                            });
                            togglingBookIds.remove(bookId);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            togglingBookIds.remove(bookId);
                            errorLiveData.postValue("Lưu yêu thích thất bại: " + e.getMessage());
                        }
                    });
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                togglingBookIds.remove(bookId);
                errorLiveData.postValue("Lỗi kiểm tra trạng thái: " + e.getMessage());
            }
        });
    }

    private OnFailureListener createFailureListener(final String prefix) {
        return new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                errorLiveData.postValue(prefix + e.getMessage());
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
