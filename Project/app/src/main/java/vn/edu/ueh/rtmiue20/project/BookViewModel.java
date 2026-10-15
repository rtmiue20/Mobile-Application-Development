package vn.edu.ueh.rtmiue20.project;

import android.app.Application;

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
    @Override protected void onCleared() { repo.stopSync(); }
}