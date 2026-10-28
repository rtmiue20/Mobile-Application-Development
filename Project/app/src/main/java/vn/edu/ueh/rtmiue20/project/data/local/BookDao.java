package vn.edu.ueh.rtmiue20.project.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import vn.edu.ueh.rtmiue20.project.data.model.Book;

import java.util.List;

@Dao
public interface BookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) void insert(Book b);
    @Insert(onConflict = OnConflictStrategy.REPLACE) void insertAll(List<Book> list);
    @Query("SELECT * FROM book_table ORDER BY createdAt DESC") LiveData<List<Book>> getAllBooks();
    @Query("SELECT * FROM book_table WHERE id = :id")
    LiveData<Book> getBook(String id);
    @Query("SELECT * FROM book_table WHERE ownerId = :ownerId ORDER BY createdAt DESC")
    LiveData<List<Book>> getBooksByOwner(String ownerId);
    @Query("SELECT * FROM book_table WHERE (title LIKE '%' || :q || '%' OR author LIKE '%' || :q || '%') ORDER BY createdAt DESC") LiveData<List<Book>> search(String q);
    @Query("SELECT * FROM book_table WHERE category = :category ORDER BY createdAt DESC") LiveData<List<Book>> getBooksByCategory(String category);
    @Query("UPDATE book_table SET viewCount = COALESCE(viewCount, 0) + 1 WHERE id = :id") void incrementViewCount(String id);
    @Query("UPDATE book_table SET favoriteCount = CASE WHEN COALESCE(favoriteCount, 0) < 0 THEN 1 ELSE COALESCE(favoriteCount, 0) + 1 END WHERE id = :id") void incrementFavoriteCount(String id);
    @Query("UPDATE book_table SET favoriteCount = CASE WHEN COALESCE(favoriteCount, 0) > 0 THEN COALESCE(favoriteCount, 0) - 1 ELSE 0 END WHERE id = :id") void decrementFavoriteCount(String id);
    @Query("UPDATE book_table SET favoriteCount = :count WHERE id = :id") void updateFavoriteCount(String id, int count);
    @Query("DELETE FROM book_table WHERE id = :id") void deleteById(String id);
    @Query("DELETE FROM book_table") void deleteAll();
}
