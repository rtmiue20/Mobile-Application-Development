package vn.edu.ueh.rtmiue20.project.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import vn.edu.ueh.rtmiue20.project.data.model.Book;
import vn.edu.ueh.rtmiue20.project.data.model.Favorite;
import vn.edu.ueh.rtmiue20.project.data.model.User;

// Tăng version lên 3 và thêm Favorite.class
@Database(entities = {Book.class, Favorite.class, User.class}, version = 5)
public abstract class BookRoomDatabase extends RoomDatabase {

    public abstract BookDao bookDao();

    public abstract FavoriteDao favoriteDao();

    public abstract UserDao userDao();
    private static BookRoomDatabase INSTANCE;

    public static BookRoomDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (BookRoomDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    BookRoomDatabase.class, "book_database")
                            .fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }
}