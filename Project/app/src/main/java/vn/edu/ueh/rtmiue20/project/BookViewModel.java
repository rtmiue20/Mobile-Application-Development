package vn.edu.ueh.rtmiue20.project;

import android.app.Application;
import android.net.Uri;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

public class BookViewModel extends AndroidViewModel {
    private final BookRepository repo;
    public BookViewModel(Application app) {
        super(app);
        repo = new BookRepository(app);
        repo.startSync();
    }
    public LiveData<List<Book>> getAllBooks() { return repo.getAllBooks(); }
    public LiveData<Book> getBook(String id) { return repo.getBook(id); }
    public LiveData<List<Book>> search(String q) { return repo.search(q); }
    public void insert(Book b) { repo.insert(b); }
    public void delete(String id) { repo.delete(id); }
    
    public LiveData<List<Book>> getBooksByCategory(String category) {
        return repo.getBooksByCategory(category);
    }

    public void uploadBookImage(Uri imageUri, BookRepository.UploadImageCallback callback) {
        repo.uploadBookImage(imageUri, callback);
    }

    public void saveBook(Book b, BookRepository.ActionCallback callback) {
        repo.saveBook(b, callback);
    }

    public void deleteBook(String id, BookRepository.ActionCallback callback) {
        repo.deleteBook(id, callback);
    }

    @Override protected void onCleared() { repo.stopSync(); }
}