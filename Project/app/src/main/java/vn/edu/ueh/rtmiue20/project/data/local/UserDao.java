package vn.edu.ueh.rtmiue20.project.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import vn.edu.ueh.rtmiue20.project.data.model.User;

@Dao
public interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(User user);

    @Query("SELECT * FROM user_table WHERE uid = :uid LIMIT 1")
    LiveData<User> getUserById(String uid);

    // Hàm này cho phép trả về đối tượng trực tiếp dưới luồng nền thay vì LiveData
    @Query("SELECT * FROM user_table WHERE uid = :uid LIMIT 1")
    User getUserByIdSync(String uid);
}