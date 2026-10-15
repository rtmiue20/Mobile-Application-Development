package vn.edu.ueh.rtmiue20.project;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BookRepository {
    private final BookDao dao;
    private final LiveData<List<Book>> allBooks;
    private final CollectionReference remote =
            FirebaseFirestore.getInstance().collection("books");
    private ListenerRegistration reg;
    private static final ExecutorService io = Executors.newSingleThreadExecutor();

    BookRepository(Application app) {
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
}