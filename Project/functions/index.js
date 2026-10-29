const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

/**
 * Callable Cloud Function: adminSetUserPassword
 * Cho phép Quản trị viên (Admin) thiết lập trực tiếp mật khẩu mới cho Thành viên (Member).
 *
 * Tham số đầu vào:
 *   - targetUid (string): UID của tài khoản thành viên cần đổi mật khẩu.
 *   - newPassword (string): Mật khẩu mới (tối thiểu 8 ký tự).
 */
exports.adminSetUserPassword = functions
  .region("us-central1")
  .https.onCall(async (data, context) => {
    // 1. Kiểm tra xác thực người gọi (Phải đăng nhập)
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Yêu cầu đăng nhập trước khi thực hiện thao tác quản trị."
      );
    }

    const callerUid = context.auth.uid;

    // 2. Kiểm tra quyền Admin của người gọi từ Firestore users/{callerUid}
    const callerDoc = await admin.firestore().collection("users").document(callerUid).get();
    if (!callerDoc.exists) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Không tìm thấy hồ sơ người dùng của tài khoản đang thao tác."
      );
    }

    const callerRole = callerDoc.data().role;
    if (callerRole !== "admin") {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Chỉ Quản trị viên (Admin) mới có quyền cấp lại mật khẩu cho thành viên."
      );
    }

    // 3. Validate dữ liệu đầu vào
    const { targetUid, newPassword } = data || {};

    if (!targetUid || typeof targetUid !== "string" || targetUid.trim() === "") {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "UID thành viên không hợp lệ hoặc bị để trống."
      );
    }

    if (!newPassword || typeof newPassword !== "string" || newPassword.length < 8) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Mật khẩu mới phải có độ dài tối thiểu 8 ký tự."
      );
    }

    // Không cho phép admin tự đổi mật khẩu qua hàm này
    if (callerUid === targetUid) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Không thể tự đổi mật khẩu quản trị viên qua chức năng này. Vui lòng đổi mật khẩu trong Hồ sơ cá nhân."
      );
    }

    // 4. Kiểm tra tài khoản thành viên mục tiêu
    const targetDoc = await admin.firestore().collection("users").document(targetUid).get();
    if (!targetDoc.exists) {
      throw new functions.https.HttpsError(
        "not-found",
        "Không tìm thấy tài khoản thành viên mục tiêu trên hệ thống."
      );
    }

    // Chặn không cho đổi mật khẩu của tài khoản Admin khác
    if (targetDoc.data().role === "admin") {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Không thể thay đổi mật khẩu của tài khoản Quản trị viên khác."
      );
    }

    try {
      // 5. Cập nhật mật khẩu thật trên Firebase Authentication bằng Firebase Admin SDK
      await admin.auth().updateUser(targetUid, {
        password: newPassword,
      });

      // 6. Ghi audit metadata vào Firestore users/{targetUid}
      // Bật cờ mustChangePassword để bắt buộc đổi mật khẩu khi đăng nhập
      // Tuyệt đối KHÔNG lưu mật khẩu và KHÔNG ghi log mật khẩu!
      await admin.firestore().collection("users").document(targetUid).update({
        mustChangePassword: true,
        passwordResetAt: admin.firestore.FieldValue.serverTimestamp(),
        passwordResetBy: callerUid,
      });

      return {
        success: true,
        message: "Đã cập nhật mật khẩu mới cho thành viên thành công trên Firebase Auth.",
      };
    } catch (error) {
      // Xử lý các mã lỗi phổ biến từ Firebase Auth Admin
      if (error.code === "auth/user-not-found") {
        throw new functions.https.HttpsError(
          "not-found",
          "Tài khoản không tồn tại trên Firebase Authentication."
        );
      }
      if (error.code === "auth/weak-password") {
        throw new functions.https.HttpsError(
          "invalid-argument",
          "Mật khẩu quá yếu theo chính sách bảo mật của Firebase."
        );
      }

      throw new functions.https.HttpsError(
        "internal",
        "Lỗi xử lý máy chủ: " + (error.message || "Không thể cập nhật mật khẩu.")
      );
    }
  });

/**
 * Firestore Trigger: sendRequestNotification
 * Tự động gửi thông báo FCM v1 qua Firebase Admin SDK khi có Yêu cầu nhận sách mới.
 */
exports.sendRequestNotification = functions
  .region("us-central1")
  .firestore.document("requests/{requestId}")
  .onCreate(async (snap, context) => {
    const requestData = snap.data();
    if (!requestData) return;

    const receiverId = requestData.ownerId || requestData.receiverId;
    const senderName = requestData.requesterName || requestData.senderName || "Một thành viên";
    const bookTitle = requestData.bookTitle || "cuốn sách";

    if (!receiverId) return;

    const receiverDoc = await admin.firestore().collection("users").document(receiverId).get();
    if (!receiverDoc.exists) return;

    const fcmToken = receiverDoc.data().fcmToken;
    if (!fcmToken) return;

    const message = {
      token: fcmToken,
      notification: {
        title: "Yêu cầu nhận sách mới! 📚",
        body: `${senderName} muốn nhận cuốn sách "${bookTitle}" của bạn.`,
      },
      data: {
        click_action: "OPEN_REQUESTS_ACTIVITY",
        requestId: context.params.requestId,
      },
    };

    try {
      await admin.messaging().send(message);
      console.log("FCM v1 notification sent successfully for request:", context.params.requestId);
    } catch (error) {
      console.error("Error sending FCM v1 notification:", error);
    }
  });
