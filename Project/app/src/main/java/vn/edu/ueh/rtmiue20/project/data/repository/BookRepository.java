package vn.edu.ueh.rtmiue20.project.data.repository;

import android.app.Application;
import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import vn.edu.ueh.rtmiue20.project.data.local.BookDao;
import vn.edu.ueh.rtmiue20.project.data.local.BookRoomDatabase;
import vn.edu.ueh.rtmiue20.project.data.model.Book;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BookRepository {
    private final BookDao dao;
    private final BookRoomDatabase database;
    private final LiveData<List<Book>> allBooks;
    private final CollectionReference remote =
            FirebaseFirestore.getInstance().collection("books");
    private final FirebaseStorage storage = FirebaseStorage.getInstance();
    private ListenerRegistration reg;
    private static final ExecutorService io = Executors.newSingleThreadExecutor();

    public BookRepository(Application app) {
        database = BookRoomDatabase.getDatabase(app);
        dao = database.bookDao();
        allBooks = dao.getAllBooks();
    }

    public LiveData<List<Book>> getAllBooks() { return allBooks; }
    public LiveData<Book> getBook(String id) { return dao.getBook(id); }
    public LiveData<List<Book>> search(String q) { return dao.search(q); }

    // Firestore -> Room (realtime)
    public void startSync() {
        if (reg != null) return;
        reg = remote.addSnapshotListener((snap, e) -> {
            if (e != null) {
                Log.e("FirebaseSync", "Lỗi Firebase: " + e.getMessage());
                return;
            }
            if (snap == null) return;
            List<Book> list = new ArrayList<>();
            for (DocumentSnapshot d : snap.getDocuments()) {
                try {
                    Book b = d.toObject(Book.class);
                    if (b != null) {
                        b.setId(d.getId());
                        if (d.contains("viewCount")) {
                            Long v = d.getLong("viewCount");
                            if (v != null) b.setViewCount(v);
                        }
                        if (d.contains("favoriteCount")) {
                            Long f = d.getLong("favoriteCount");
                            if (f != null) b.setFavoriteCount(f);
                        }
                        list.add(b);
                    }
                } catch (Exception ex) {
                    Log.e("FirebaseSync", "Lỗi parse Book: " + ex.getMessage());
                }
            }
            io.execute(() -> {
                dao.insertAll(list);
            });
        });
    }
    public void stopSync() { if (reg != null) { reg.remove(); reg = null; } }

    public void insert(Book b) {
        remote.document(b.getId()).set(b);
        io.execute(() -> dao.insert(b));
    }
    public void delete(String id) {
        remote.document(id).delete();
        io.execute(() -> dao.deleteById(id));
    }

    public void incrementViewCount(String id) {
        if (id == null || id.trim().isEmpty()) return;
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("viewCount", com.google.firebase.firestore.FieldValue.increment(1));
        remote.document(id).set(map, com.google.firebase.firestore.SetOptions.merge());
        io.execute(() -> dao.incrementViewCount(id));
    }

    public void incrementFavoriteCount(String id) {
        if (id == null || id.trim().isEmpty()) return;
        remote.document(id).get().addOnSuccessListener(snap -> {
            long current = 0;
            if (snap != null && snap.contains("favoriteCount")) {
                Long val = snap.getLong("favoriteCount");
                if (val != null) current = Math.max(0, val);
            }
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("favoriteCount", current + 1);
            remote.document(id).set(map, com.google.firebase.firestore.SetOptions.merge());
        });
        io.execute(() -> dao.incrementFavoriteCount(id));
    }

    public void decrementFavoriteCount(String id) {
        if (id == null || id.trim().isEmpty()) return;
        remote.document(id).get().addOnSuccessListener(snap -> {
            long current = 0;
            if (snap != null && snap.contains("favoriteCount")) {
                Long val = snap.getLong("favoriteCount");
                if (val != null) current = Math.max(0, val);
            }
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("favoriteCount", Math.max(0, current - 1));
            remote.document(id).set(map, com.google.firebase.firestore.SetOptions.merge());
        });
        io.execute(() -> dao.decrementFavoriteCount(id));
    }

    public interface UploadImageCallback {
        void onSuccess(String imageUrl);
        void onError(String errorMessage);
    }

    public interface ActionCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    public void uploadBookImage(Uri imageUri, UploadImageCallback callback) {
        if (imageUri == null) {
            callback.onError("Image URI is null");
            return;
        }
        StorageReference ref = storage.getReference().child("book_images/" + UUID.randomUUID().toString() + ".jpg");
        ref.putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
            ref.getDownloadUrl().addOnSuccessListener(uri -> {
                callback.onSuccess(uri.toString());
            }).addOnFailureListener(e -> callback.onError(e.getMessage()));
        }).addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void saveBook(Book b, ActionCallback callback) {
        if (b.getId() == null || b.getId().isEmpty()) {
            b.setId(remote.document().getId());
        }
        remote.document(b.getId()).set(b)
                .addOnSuccessListener(aVoid -> {
                    io.execute(() -> dao.insert(b));
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public void deleteBook(String id, ActionCallback callback) {
        remote.document(id).delete()
                .addOnSuccessListener(aVoid -> {
                    io.execute(() -> dao.deleteById(id));
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public LiveData<List<Book>> getBooksByCategory(String category) {
        return dao.getBooksByCategory(category);
    }
}