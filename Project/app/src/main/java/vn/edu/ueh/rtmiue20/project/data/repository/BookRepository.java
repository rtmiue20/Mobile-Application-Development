package vn.edu.ueh.rtmiue20.project.data.repository;

import android.app.Application;
import android.net.Uri;

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
    private final LiveData<List<Book>> allBooks;
    private final CollectionReference remote =
            FirebaseFirestore.getInstance().collection("books");
    private final FirebaseStorage storage = FirebaseStorage.getInstance();
    private ListenerRegistration reg;
    private static final ExecutorService io = Executors.newSingleThreadExecutor();

    public BookRepository(Application app) {
        dao = BookRoomDatabase.getDatabase(app).bookDao();
        allBooks = dao.getAllBooks();
    }

    public LiveData<List<Book>> getAllBooks() { return allBooks; }
    public LiveData<Book> getBook(String id) { return dao.getBook(id); }
    public LiveData<List<Book>> search(String q) { return dao.search(q); }

    // Firestore -> Room (realtime)
    public void startSync() {
        if (reg != null) return;
        reg = remote.addSnapshotListener((snap, e) -> {
            if (snap == null || snap.getMetadata().isFromCache()) return;
            List<Book> list = new ArrayList<>();
            for (DocumentSnapshot d : snap.getDocuments()) {
                Book b = d.toObject(Book.class);
                if (b != null) { b.setId(d.getId()); list.add(b); }
            }
            io.execute(() -> { dao.deleteAll(); dao.insertAll(list); });
        });
    }
    public void stopSync() { if (reg != null) { reg.remove(); reg = null; } }

    // Ghi: lên Firestore và lưu Room luôn để UI thấy ngay
    public void insert(Book b) {
        remote.document(b.getId()).set(b);
        io.execute(() -> dao.insert(b));
    }
    public void delete(String id) {
        remote.document(id).delete();
        io.execute(() -> dao.deleteById(id));
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
