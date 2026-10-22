package vn.edu.ueh.rtmiue20.project.data.repository;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
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
                .addOnFailureListener(e -> callback.onError(formatAuthError(e)));
    }

    public void registerWithEmail(String email, String password, String fullName, AuthCallback callback) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser != null) {
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
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
                                    .addOnFailureListener(e -> callback.onError("Tạo tài khoản thành công nhưng không thể lưu thông tin: " + formatAuthError(e)));
                        });
                    } else {
                        callback.onError("Đăng ký thất bại.");
                    }
                })
                .addOnFailureListener(e -> callback.onError(formatAuthError(e)));
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
                .addOnFailureListener(e -> callback.onError(formatAuthError(e)));
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
                                .addOnFailureListener(e -> callback.onError("Lỗi lưu dữ liệu Google User: " + formatAuthError(e)));
                    }
                })
                .addOnFailureListener(e -> callback.onError("Không thể xác thực thông tin tài khoản: " + formatAuthError(e)));
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
                    }
                    callback.onSuccess(firebaseUser);
                })
                .addOnFailureListener(e -> callback.onSuccess(firebaseUser));
    }

    public void sendPasswordResetEmail(String email, ActionCallback callback) {
        firebaseAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(formatAuthError(e)));
    }

    public void logout() {
        firebaseAuth.signOut();
    }

    private String formatAuthError(Exception e) {
        if (e == null) return "Đã xảy ra lỗi không xác định.";

        String message = e.getMessage() != null ? e.getMessage() : "";
        String localizedMessage = e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "";

        if (e instanceof FirebaseAuthException) {
            String errorCode = ((FirebaseAuthException) e).getErrorCode();
            switch (errorCode) {
                case "ERROR_INVALID_EMAIL":
                case "INVALID_EMAIL":
                    return "Địa chỉ email không đúng định dạng hoặc không hợp lệ.";
                case "ERROR_EMAIL_ALREADY_IN_USE":
                case "EMAIL_EXISTS":
                    return "Email này đã được đăng ký bởi một tài khoản khác.";
                case "ERROR_WRONG_PASSWORD":
                case "INVALID_PASSWORD":
                    return "Mật khẩu không chính xác. Vui lòng kiểm tra lại.";
                case "ERROR_USER_NOT_FOUND":
                case "USER_NOT_FOUND":
                    return "Không tìm thấy tài khoản tương ứng với email này.";
                case "ERROR_USER_DISABLED":
                case "USER_DISABLED":
                    return "Tài khoản của bạn đã bị vô hiệu hóa.";
                case "ERROR_WEAK_PASSWORD":
                case "WEAK_PASSWORD":
                    return "Mật khẩu quá yếu. Mật khẩu phải có tối thiểu 6-8 ký tự.";
                case "ERROR_TOO_MANY_REQUESTS":
                case "TOO_MANY_ATTEMPTS_TRY_LATER":
                    return "Hệ thống ghi nhận quá nhiều lần thử không thành công. Vui lòng thử lại sau.";
                case "ERROR_OPERATION_NOT_ALLOWED":
                case "OPERATION_NOT_ALLOWED":
                    return "Phương thức đăng nhập/đăng ký bằng Email/Password chưa được bật trên Firebase Console.";
            }
        }

        if (message.contains("CONFIGURATION_NOT_FOUND") || localizedMessage.contains("CONFIGURATION_NOT_FOUND")) {
            return "Chưa bật dịch vụ Email/Password Authentication trên Firebase Console. Vui lòng bật Email/Password Provider trong Firebase Console -> Authentication.";
        }

        if (message.contains("EMAIL_EXISTS") || message.contains("email address is already in use")) {
            return "Email này đã được đăng ký bởi một tài khoản khác.";
        }

        if (message.contains("INVALID_EMAIL") || message.contains("email address is badly formatted")) {
            return "Địa chỉ Email không đúng định dạng.";
        }

        if (message.contains("WEAK_PASSWORD") || message.contains("Password should be at least")) {
            return "Mật khẩu quá yếu (cần tối thiểu 6-8 ký tự).";
        }

        if (message.contains("USER_NOT_FOUND") || message.contains("no user record")) {
            return "Không tìm thấy thông tin tài khoản.";
        }

        if (message.contains("WRONG_PASSWORD") || message.contains("password is invalid") || message.contains("INVALID_LOGIN_CREDENTIALS")) {
            return "Email hoặc mật khẩu không chính xác.";
        }

        if (message.contains("network") || message.contains("NetworkError") || message.contains("UNAVAILABLE")) {
            return "Lỗi kết nối mạng. Vui lòng kiểm tra lại kết nối Internet.";
        }

        return !localizedMessage.isEmpty() ? localizedMessage : "Lỗi xác thực: " + message;
    }
}