package vn.edu.ueh.rtmiue20.project.data.repository;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import vn.edu.ueh.rtmiue20.project.data.model.User;

public class AuthRepository {

    private static AuthRepository instance;
    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;

    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onError(String errorMessage);
    }

    public interface ActionCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    private AuthRepository() {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

    public static synchronized AuthRepository getInstance() {
        if (instance == null) {
            instance = new AuthRepository();
        }
        return instance;
    }

    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    public String getCurrentUserId() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public boolean isLoggedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }

    public void loginWithEmail(String email, String password, AuthCallback callback) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser != null) {
                        checkUserStatusAndComplete(firebaseUser, callback);
                    } else {
                        callback.onError("Không thể lấy thông tin đăng nhập.");
                    }
                })
                .addOnFailureListener(e -> {
                    String msg = e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "Đăng nhập thất bại.";
                    if (msg.contains("password is invalid") || msg.contains("INVALID_LOGIN_CREDENTIALS") || msg.contains("credential is incorrect")) {
                        msg = "Email hoặc Mật khẩu không chính xác. Vui lòng kiểm tra lại.";
                    } else if (msg.contains("no user record")) {
                        msg = "Tài khoản này chưa tồn tại trong hệ thống. Vui lòng đăng ký tài khoản mới.";
                    }
                    callback.onError(msg);
                });
    }

    public void registerWithEmail(String email, String password, String fullName, AuthCallback callback) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser != null) {
                        // Cập nhật displayName vào Firebase Authentication để hiển thị trên console và UI
                        com.google.firebase.auth.UserProfileChangeRequest profileUpdates =
                                new com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                        .setDisplayName(fullName)
                                        .build();

                        firebaseUser.updateProfile(profileUpdates).addOnCompleteListener(profileTask -> {
                            User newUser = new User(
                                    firebaseUser.getUid(),
                                    fullName,
                                    email,
                                    "",
                                    "",
                                    "",
                                    "user",
                                    "active",
                                    System.currentTimeMillis()
                            );

                            firestore.collection("users")
                                    .document(firebaseUser.getUid())
                                    .set(newUser.toMap())
                                    .addOnSuccessListener(unused -> callback.onSuccess(firebaseUser))
                                    .addOnFailureListener(e -> {
                                        android.util.Log.w("AuthRepository", "Firestore document creation failed (check security rules): " + e.getMessage());
                                        // Vẫn hoàn tất đăng ký vì tài khoản Auth và displayName đã được lưu thành công trên Firebase Auth
                                        callback.onSuccess(firebaseUser);
                                    });
                        });
                    } else {
                        callback.onError("Đăng ký thất bại.");
                    }
                })
                .addOnFailureListener(e -> {
                    String msg = e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "Đăng ký thất bại.";
                    if (msg.contains("The email address is already in use")) {
                        msg = "Email này đã được đăng ký tài khoản trước đó. Vui lòng đăng nhập.";
                    }
                    callback.onError(msg);
                });
    }

    public void loginWithGoogle(String idToken, AuthCallback callback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser != null) {
                        syncGoogleUserToFirestore(firebaseUser, callback);
                    } else {
                        callback.onError("Đăng nhập bằng Google thất bại.");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "Đăng nhập Google thất bại."));
    }

    private void syncGoogleUserToFirestore(FirebaseUser firebaseUser, AuthCallback callback) {
        String uid = firebaseUser.getUid();
        firestore.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String status = documentSnapshot.getString("status");
                        if ("locked".equals(status)) {
                            firebaseAuth.signOut();
                            callback.onError("Tài khoản của bạn đã bị khóa bởi Quản trị viên.");
                        } else {
                            callback.onSuccess(firebaseUser);
                        }
                    } else {
                        String fullName = firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "Người dùng BookShare";
                        String email = firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "";
                        String photoUrl = firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : "";

                        User newUser = new User(
                                uid,
                                fullName,
                                email,
                                photoUrl,
                                "",
                                "",
                                "user",
                                "active",
                                System.currentTimeMillis()
                        );

                        firestore.collection("users").document(uid).set(newUser.toMap())
                                .addOnSuccessListener(aVoid -> callback.onSuccess(firebaseUser))
                                .addOnFailureListener(e -> callback.onError("Lỗi lưu dữ liệu Google User: " + e.getLocalizedMessage()));
                    }
                })
                .addOnFailureListener(e -> callback.onError("Không thể xác thực thông tin tài khoản: " + e.getLocalizedMessage()));
    }

    private void checkUserStatusAndComplete(FirebaseUser firebaseUser, AuthCallback callback) {
        firestore.collection("users").document(firebaseUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String status = documentSnapshot.getString("status");
                        if ("locked".equals(status)) {
                            firebaseAuth.signOut();
                            callback.onError("Tài khoản của bạn đã bị khóa bởi Quản trị viên.");
                            return;
                        }
                    } else {
                        // Tự động đồng bộ tạo document người dùng nếu trước đó Firestore bị lỗi/chưa có
                        String name = firebaseUser.getDisplayName() != null && !firebaseUser.getDisplayName().isEmpty()
                                ? firebaseUser.getDisplayName()
                                : (firebaseUser.getEmail() != null ? firebaseUser.getEmail().split("@")[0] : "Thành viên BookShare");
                        User newUser = new User(
                                firebaseUser.getUid(),
                                name,
                                firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "",
                                firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : "",
                                "",
                                "",
                                "user",
                                "active",
                                System.currentTimeMillis()
                        );
                        firestore.collection("users").document(firebaseUser.getUid()).set(newUser.toMap());
                    }
                    callback.onSuccess(firebaseUser);
                })
                .addOnFailureListener(e -> callback.onSuccess(firebaseUser));
    }

    public void sendPasswordResetEmail(String email, ActionCallback callback) {
        firebaseAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "Không thể gửi email khôi phục."));
    }

    public void logout() {
        firebaseAuth.signOut();
    }
}
