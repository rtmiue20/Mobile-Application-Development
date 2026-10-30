package vn.edu.ueh.rtmiue20.project.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import vn.edu.ueh.rtmiue20.project.data.model.Book;
import vn.edu.ueh.rtmiue20.project.data.model.Favorite;
import vn.edu.ueh.rtmiue20.project.data.model.User;

// Cập nhật version lên 9 hỗ trợ tọa độ và vị trí thực tế của Book (latitude, longitude, city, district, ward)
@Database(entities = {Book.class, Favorite.class, User.class}, version = 9, exportSchema = false)
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
