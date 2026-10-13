package vn.edu.ueh.rtmiue20.project.data.repository;

import android.net.Uri;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

import vn.edu.ueh.rtmiue20.project.data.model.User;

public class UserRepository {

    private static UserRepository instance;
    private final FirebaseFirestore firestore;
    private final FirebaseStorage storage;

    public interface UserCallback {
        void onSuccess(User user);
        void onError(String errorMessage);
    }

    public interface UpdateCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    public interface UploadCallback {
        void onSuccess(String avatarUrl);
        void onError(String errorMessage);
    }

    private UserRepository() {
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    public static synchronized UserRepository getInstance() {
        if (instance == null) {
            instance = new UserRepository();
        }
        return instance;
    }

    public void getUserProfile(String uid, UserCallback callback) {
        firestore.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            user.setUid(uid);
                            callback.onSuccess(user);
                        } else {
                            callback.onError("Không thể chuyển đổi dữ liệu hồ sơ.");
                        }
                    } else {
                        callback.onError("Không tìm thấy thông tin người dùng.");
                    }
                })
                .addOnFailureListener(e -> callback.onError("Lỗi khi tải hồ sơ: " + e.getLocalizedMessage()));
    }

    public void updateUserProfile(String uid, String fullName, String phoneNumber, String bio, UpdateCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", fullName);
        updates.put("phoneNumber", phoneNumber);
        updates.put("bio", bio);

        firestore.collection("users").document(uid).update(updates)
                .addOnSuccessListener(unused -> {
                    FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (firebaseUser != null) {
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(fullName)
                                .build();
                        firebaseUser.updateProfile(profileUpdates);
                    }
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> callback.onError("Cập nhật thông tin thất bại: " + e.getLocalizedMessage()));
    }

    public void uploadAvatar(String uid, Uri imageUri, UploadCallback callback) {
        StorageReference avatarRef = storage.getReference().child("avatars/" + uid + ".jpg");

        avatarRef.putFile(imageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful() && task.getException() != null) {
                        throw task.getException();
                    }
                    return avatarRef.getDownloadUrl();
                })
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String downloadUrl = task.getResult().toString();

                        // Update in Firestore
                        firestore.collection("users").document(uid)
                                .update("avatarUrl", downloadUrl)
                                .addOnSuccessListener(unused -> {
                                    // Update Firebase Auth profile photo
                                    FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                                    if (firebaseUser != null) {
                                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                                .setPhotoUri(Uri.parse(downloadUrl))
                                                .build();
                                        firebaseUser.updateProfile(profileUpdates);
                                    }
                                    callback.onSuccess(downloadUrl);
                                })
                                .addOnFailureListener(e -> callback.onError("Cập nhật link ảnh đại diện thất bại: " + e.getLocalizedMessage()));
                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getLocalizedMessage() : "Tải ảnh lên thất bại.";
                        callback.onError(errorMsg);
                    }
                });
    }
}
