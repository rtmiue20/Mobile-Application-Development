package vn.edu.ueh.rtmiue20.project.data.repository;

import android.net.Uri;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.DocumentSnapshot;

import vn.edu.ueh.rtmiue20.project.data.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserRepository {

    private static UserRepository instance;
    private final FirebaseFirestore firestore;
    private final FirebaseStorage storage;

    public interface UserCallback {
        void onSuccess(User user);
        void onError(String errorMessage);
    }

    public interface UsersListCallback {
        void onSuccess(List<User> userList);
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

    public void updateUserProfile(String uid, String fullName, String nickname, String phoneNumber, String bio, String city, String district, String ward, String favoriteSpotName, String favoriteSpotAddress, Double latitude, Double longitude, UpdateCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", fullName);
        updates.put("nickname", nickname);
        updates.put("phoneNumber", phoneNumber);
        updates.put("bio", bio);
        updates.put("city", city);
        updates.put("district", district);
        updates.put("ward", ward);
        updates.put("favoriteSpotName", favoriteSpotName);
        updates.put("favoriteSpotAddress", favoriteSpotAddress);
        if (latitude != null) updates.put("latitude", latitude);
        if (longitude != null) updates.put("longitude", longitude);

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            String displayVal = User.getDisplayNameFormatted(fullName, nickname);
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(displayVal)
                    .build();
            firebaseUser.updateProfile(profileUpdates);
        }

        firestore.collection("users").document(uid).set(updates, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError("Cập nhật thông tin thất bại: " + e.getLocalizedMessage()));
    }

    public void updateUserProfile(String uid, String fullName, String nickname, String phoneNumber, String bio, String city, String district, String ward, String favoriteSpotName, String favoriteSpotAddress, UpdateCallback callback) {
        updateUserProfile(uid, fullName, nickname, phoneNumber, bio, city, district, ward, favoriteSpotName, favoriteSpotAddress, null, null, callback);
    }

    public void updateUserLocation(String uid, double latitude, double longitude, UpdateCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("latitude", latitude);
        updates.put("longitude", longitude);
        firestore.collection("users").document(uid).set(updates, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(unused -> { if (callback != null) callback.onSuccess(); })
                .addOnFailureListener(e -> { if (callback != null) callback.onError(e.getLocalizedMessage()); });
    }

    public void updateUserProfile(String uid, String fullName, String phoneNumber, String bio, UpdateCallback callback) {
        updateUserProfile(uid, fullName, "", phoneNumber, bio, "", "", "", "", "", callback);
    }

    public void uploadAvatar(String uid, Uri imageUri, UploadCallback callback) {
        uploadAvatar(null, uid, imageUri, callback);
    }

    public void uploadAvatar(android.content.Context context, String uid, Uri imageUri, UploadCallback callback) {
        if (imageUri == null) {
            callback.onError("Không tìm thấy đường dẫn ảnh.");
            return;
        }

        new Thread(() -> {
            byte[] jpegBytes = context != null
                    ? vn.edu.ueh.rtmiue20.project.util.ImageHelper.compressImageUriToJpegBytes(context, imageUri, 400, 75)
                    : null;
            String fallbackDataUri = vn.edu.ueh.rtmiue20.project.util.ImageHelper.toBase64DataUri(jpegBytes);
            final java.util.concurrent.atomic.AtomicBoolean completed = new java.util.concurrent.atomic.AtomicBoolean(false);

            try {
                storage.setMaxUploadRetryTimeMillis(1500);
                storage.setMaxOperationRetryTimeMillis(1500);
            } catch (Exception ignored) {}

            StorageReference avatarRef = storage.getReference().child("avatars/" + uid + ".jpg");
            com.google.firebase.storage.UploadTask uploadTask = (jpegBytes != null && jpegBytes.length > 0)
                    ? avatarRef.putBytes(jpegBytes)
                    : avatarRef.putFile(imageUri);

            if (fallbackDataUri != null) {
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    if (completed.compareAndSet(false, true)) {
                        try {
                            uploadTask.cancel();
                        } catch (Exception ignored) {}
                        saveAvatarUrlToUser(uid, fallbackDataUri, callback);
                    }
                }, 1800);
            }

            uploadTask.continueWithTask(task -> {
                if (!task.isSuccessful() && task.getException() != null) {
                    throw task.getException();
                }
                return avatarRef.getDownloadUrl();
            }).addOnCompleteListener(task -> {
                if (!completed.compareAndSet(false, true)) return;
                if (task.isSuccessful() && task.getResult() != null) {
                    saveAvatarUrlToUser(uid, task.getResult().toString(), callback);
                } else if (fallbackDataUri != null) {
                    saveAvatarUrlToUser(uid, fallbackDataUri, callback);
                } else {
                    String errorMsg = task.getException() != null ? task.getException().getLocalizedMessage() : "Tải ảnh lên thất bại.";
                    callback.onError(errorMsg);
                }
            });
        }).start();
    }

    private void saveAvatarUrlToUser(String uid, String avatarUrl, UploadCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("avatarUrl", avatarUrl);

        firestore.collection("users").document(uid)
                .set(data, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    if (avatarUrl != null && avatarUrl.startsWith("http")) {
                        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                        if (firebaseUser != null) {
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setPhotoUri(Uri.parse(avatarUrl))
                                    .build();
                            firebaseUser.updateProfile(profileUpdates);
                        }
                    }
                    callback.onSuccess(avatarUrl);
                })
                .addOnFailureListener(e -> callback.onError("Cập nhật ảnh đại diện thất bại: " + e.getLocalizedMessage()));
    }

    public void removeAvatar(String uid, UpdateCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("avatarUrl", "");

        firestore.collection("users").document(uid)
                .set(data, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (firebaseUser != null) {
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setPhotoUri(null)
                                .build();
                        firebaseUser.updateProfile(profileUpdates);
                    }
                    try {
                        storage.getReference().child("avatars/" + uid + ".jpg").delete();
                    } catch (Exception ignored) {}
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError("Xóa ảnh đại diện thất bại: " + e.getLocalizedMessage());
                });
    }

    /**
     * Lắng nghe danh sách tất cả người dùng realtime từ Firestore collection "users"
     */
    public ListenerRegistration getAllUsersRealtime(UsersListCallback callback) {
        return firestore.collection("users")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        callback.onError("Lỗi realtime Firestore: " + error.getLocalizedMessage());
                        return;
                    }

                    if (snapshots != null) {
                        List<User> list = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            User user = doc.toObject(User.class);
                            if (user != null) {
                                user.setUid(doc.getId());
                                list.add(user);
                            }
                        }
                        callback.onSuccess(list);
                    }
                });
    }

    /**
     * Cập nhật trạng thái người dùng (active / locked) và lý do kỷ luật
     */
    public void updateUserStatus(String uid, String newStatus, String lockReason, UpdateCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        if ("locked".equals(newStatus)) {
            updates.put("lockReason", lockReason != null ? lockReason : "Vi phạm quy định");
            updates.put("lockedAt", System.currentTimeMillis());
        } else {
            updates.put("lockReason", null);
            updates.put("unlockedAt", System.currentTimeMillis());
        }

        firestore.collection("users").document(uid).update(updates)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError("Cập nhật trạng thái người dùng thất bại: " + e.getLocalizedMessage()));
    }
}
