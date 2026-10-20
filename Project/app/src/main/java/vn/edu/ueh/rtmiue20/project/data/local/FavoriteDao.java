package vn.edu.ueh.rtmiue20.project.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import vn.edu.ueh.rtmiue20.project.data.model.Book;
import vn.edu.ueh.rtmiue20.project.data.model.Favorite;

@Dao
public interface FavoriteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Favorite favorite);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Favorite> favorites);

    @Query("DELETE FROM favorite_table WHERE favoriteId = :favoriteId")
    void deleteById(String favoriteId);

    @Query("DELETE FROM favorite_table")
    void deleteAll();

    @Query("DELETE FROM favorite_table WHERE userId = :userId")
    void deleteAllByUser(String userId);

    // Lệnh SQL đã được sửa thành book_table.id cho khớp với đồ án của nhóm
    @Query("SELECT book_table.* FROM book_table "
            + "INNER JOIN favorite_table ON book_table.id = favorite_table.bookId "
            + "WHERE favorite_table.userId = :userId "
            + "ORDER BY favorite_table.timestamp DESC")
    LiveData<List<Book>> getFavoritedBooksByUser(String userId);

    @Query("SELECT * FROM favorite_table WHERE userId = :userId AND bookId = :bookId LIMIT 1")
    LiveData<Favorite> getFavorite(String userId, String bookId);
}