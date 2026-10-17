package vn.edu.ueh.rtmiue20.project;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface BookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) void insert(Book b);
    @Insert(onConflict = OnConflictStrategy.REPLACE) void insertAll(List<Book> list);
    @Query("SELECT * FROM book_table ORDER BY createdAt DESC") LiveData<List<Book>> getAllBooks();
    @Query("SELECT * FROM book_table WHERE id = :id")
    LiveData<Book> getBook(String id);
    @Query("SELECT * FROM book_table WHERE (title LIKE '%' || :q || '%' OR author LIKE '%' || :q || '%') ORDER BY createdAt DESC") LiveData<List<Book>> search(String q);
    @Query("SELECT * FROM book_table WHERE category = :category ORDER BY createdAt DESC") LiveData<List<Book>> getBooksByCategory(String category);
    @Query("DELETE FROM book_table WHERE id = :id") void deleteById(String id);
    @Query("DELETE FROM book_table") void deleteAll();
}