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
 * Callable Cloud Function: adminSetUserStatus
 * Cho phép Quản trị viên (Admin) khóa hoặc mở khóa tài khoản thành viên.
 * - Khi khóa: vô hiệu hóa tài khoản trên Firebase Auth (disabled: true) và hủy toàn bộ phiên đăng nhập (revokeRefreshTokens).
 * - Khi mở khóa: kích hoạt lại tài khoản trên Firebase Auth (disabled: false).
 */
exports.adminSetUserStatus = functions
  .region("us-central1")
  .https.onCall(async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError("unauthenticated", "Yêu cầu đăng nhập quản trị viên.");
    }
    const callerUid = context.auth.uid;
    const callerDoc = await admin.firestore().collection("users").document(callerUid).get();
    if (!callerDoc.exists || callerDoc.data().role !== "admin") {
      throw new functions.https.HttpsError("permission-denied", "Chỉ Quản trị viên mới có quyền thay đổi trạng thái tài khoản.");
    }

    const { targetUid, newStatus, lockReason } = data || {};
    if (!targetUid || (newStatus !== "locked" && newStatus !== "active")) {
      throw new functions.https.HttpsError("invalid-argument", "Tham số không hợp lệ.");
    }

    if (callerUid === targetUid && newStatus === "locked") {
      throw new functions.https.HttpsError("invalid-argument", "Không thể tự khóa tài khoản của chính mình.");
    }

    const targetDoc = await admin.firestore().collection("users").document(targetUid).get();
    if (!targetDoc.exists) {
      throw new functions.https.HttpsError("not-found", "Không tìm thấy người dùng.");
    }
    if (targetDoc.data().role === "admin" && newStatus === "locked") {
      throw new functions.https.HttpsError("permission-denied", "Không thể khóa tài khoản Quản trị viên khác.");
    }

    try {
      const isDisabled = (newStatus === "locked");
      await admin.auth().updateUser(targetUid, { disabled: isDisabled });
      if (isDisabled) {
        await admin.auth().revokeRefreshTokens(targetUid);
      }

      const updates = {
        status: newStatus,
        lockReason: isDisabled ? (lockReason || "Vi phạm quy định") : null,
      };
      if (isDisabled) {
        updates.lockedAt = admin.firestore.FieldValue.serverTimestamp();
      } else {
        updates.unlockedAt = admin.firestore.FieldValue.serverTimestamp();
      }

      await admin.firestore().collection("users").document(targetUid).update(updates);
      return { success: true, status: newStatus };
    } catch (err) {
      throw new functions.https.HttpsError("internal", err.message || "Lỗi cập nhật trạng thái người dùng.");
    }
  });

/**
 * Cloud Function Trigger: onRequestCreated
 * Tự động gửi thông báo FCM v1 tới chủ sách khi có người gửi yêu cầu nhận sách mới.
 * Hỗ trợ nhận diện linh hoạt cả 'ownerId' và 'receiverId'.
 */
exports.onRequestCreated = functions
  .region("us-central1")
  .firestore.document("requests/{requestId}")
  .onCreate(async (snapshot, context) => {
    const data = snapshot.data();
    if (!data) return null;

    // Nhận diện linh hoạt cả ownerId (chuẩn) và receiverId
    const ownerId = data.ownerId || data.receiverId;
    const requesterName = data.requesterName || "Một bạn đọc";
    const bookTitle = data.bookTitle || "cuốn sách của bạn";

    if (!ownerId) {
      console.warn("onRequestCreated: Không tìm thấy ownerId trong request", context.params.requestId);
      return null;
    }

    try {
      const ownerDoc = await admin.firestore().collection("users").document(ownerId).get();
      if (!ownerDoc.exists) return null;

      const fcmToken = ownerDoc.data().fcmToken;
      if (!fcmToken) {
        console.log(`Chủ sách ${ownerId} chưa đăng ký fcmToken.`);
        return null;
      }

      const message = {
        token: fcmToken,
        notification: {
          title: "Yêu cầu nhận sách mới! 📚",
          body: `${requesterName} muốn nhận cuốn "${bookTitle}". Bấm để xem và phản hồi!`,
        },
        data: {
          requestId: context.params.requestId,
          bookId: data.bookId || "",
          type: "NEW_REQUEST",
        },
      };

      await admin.messaging().send(message);
      console.log(`Đã gửi FCM thông báo yêu cầu sách tới chủ sách ${ownerId}`);
    } catch (err) {
      console.error("Lỗi gửi FCM trong onRequestCreated:", err);
    }
    return null;
  });

/**
 * Callable Cloud Function: sendNotification
 * Cho phép Client gọi gửi thông báo đẩy FCM v1 qua Firebase Admin SDK mà không cần nhúng secret token trong app.
 */
exports.sendNotification = functions
  .region("us-central1")
  .https.onCall(async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError("unauthenticated", "Yêu cầu đăng nhập.");
    }
    const { token, title, body } = data || {};
    if (!token || !title) {
      throw new functions.https.HttpsError("invalid-argument", "Thiếu FCM token hoặc tiêu đề thông báo.");
    }
    try {
      const message = {
        token: token,
        notification: {
          title: title,
          body: body || "",
        },
      };
      await admin.messaging().send(message);
      return { success: true };
    } catch (err) {
      console.error("Lỗi gửi notification qua sendNotification:", err);
      throw new functions.https.HttpsError("internal", err.message || "Lỗi gửi thông báo FCM.");
    }
  });

/**
 * Cloud Function Trigger: onRequestUpdated
 * Tự động gửi thông báo FCM v1:
 * - Khi status = 'accepted' hoặc 'rejected': Gửi tới người xin sách (requesterId).
 * - Khi status = 'completed': Người xin đã xác nhận nhận sách -> Gửi thông báo tới CHỦ SÁCH (ownerId) biết giao dịch đã xong.
 */
exports.onRequestUpdated = functions
  .region("us-central1")
  .firestore.document("requests/{requestId}")
  .onUpdate(async (change, context) => {
    const before = change.before.data();
    const after = change.after.data();
    if (!before || !after || before.status === after.status) return null;

    const bookTitle = after.bookTitle || "sách";
    const requesterName = after.requesterName || "Người nhận";

    // 1. Khi hoàn tất giao dịch (người nhận xác nhận): Báo cho CHỦ SÁCH (ownerId)
    if (after.status === "completed") {
      const ownerId = after.ownerId || after.receiverId;
      if (!ownerId) {
        console.warn("onRequestUpdated: Không tìm thấy ownerId khi completed", context.params.requestId);
        return null;
      }

      try {
        const ownerDoc = await admin.firestore().collection("users").document(ownerId).get();
        if (!ownerDoc.exists) return null;

        const fcmToken = ownerDoc.data().fcmToken;
        if (!fcmToken) {
          console.log(`Chủ sách ${ownerId} chưa đăng ký fcmToken.`);
          return null;
        }

        const message = {
          token: fcmToken,
          notification: {
            title: "Giao dịch hoàn tất! ✅",
            body: `${requesterName} đã xác nhận nhận sách "${bookTitle}". Giao dịch đã hoàn tất thành công!`,
          },
          data: {
            requestId: context.params.requestId,
            status: "completed",
            type: "REQUEST_COMPLETED",
          },
        };

        await admin.messaging().send(message);
        console.log(`Đã gửi FCM thông báo giao dịch hoàn tất tới chủ sách ${ownerId}`);
      } catch (err) {
        console.error("Lỗi gửi FCM completed tới chủ sách:", err);
      }
      return null;
    }

    // 2. Khi chủ sách duyệt hoặc từ chối: Báo cho NGƯỜI XIN SÁCH (requesterId)
    if (after.status === "accepted" || after.status === "rejected") {
      const requesterId = after.requesterId || after.senderId;
      if (!requesterId) return null;

      try {
        const requesterDoc = await admin.firestore().collection("users").document(requesterId).get();
        if (!requesterDoc.exists) return null;

        const fcmToken = requesterDoc.data().fcmToken;
        if (!fcmToken) return null;

        let title = "Cập nhật yêu cầu sách 📚";
        let body = "";

        if (after.status === "accepted") {
          title = "Yêu cầu được chấp nhận! 🎉";
          body = `Chủ sách đã đồng ý trao cuốn "${bookTitle}" cho bạn. Hãy kiểm tra địa điểm hẹn nhé!`;
        } else {
          title = "Yêu cầu trao đổi sách";
          body = `Yêu cầu nhận cuốn "${bookTitle}" đã bị từ chối.`;
        }

        const message = {
          token: fcmToken,
          notification: { title, body },
          data: {
            requestId: context.params.requestId,
            status: after.status,
            type: "REQUEST_STATUS_CHANGED",
          },
        };

        await admin.messaging().send(message);
        console.log(`Đã gửi FCM cập nhật status '${after.status}' tới requester ${requesterId}`);
      } catch (err) {
        console.error("Lỗi gửi FCM trong onRequestUpdated:", err);
      }
      return null;
    }

    return null;
  });



/**
 * Cloud Function Trigger: onCommentCreated
 * Tự động gửi thông báo FCM v1 tới chủ sách khi có người bình luận mới trên bài đăng.
 */
exports.onCommentCreated = functions
  .region("us-central1")
  .firestore.document("comments/{commentId}")
  .onCreate(async (snapshot, context) => {
    const comment = snapshot.data();
    if (!comment) return null;

    const bookId = comment.bookId;
    if (!bookId) return null;
    const commenterId = comment.userId;
    const commenterName = comment.userName || "Một bạn đọc";
    const content = comment.content || "";

    try {
      const bookDoc = await admin.firestore().collection("books").document(bookId).get();
      if (!bookDoc.exists) return null;

      const bookData = bookDoc.data();
      const ownerId = bookData.ownerId || bookData.userId;
      const bookTitle = bookData.title || "cuốn sách của bạn";

      // Không tự gửi thông báo cho chính mình khi tự bình luận vào bài của mình
      if (!ownerId || ownerId === commenterId) return null;

      const ownerDoc = await admin.firestore().collection("users").document(ownerId).get();
      if (!ownerDoc.exists) return null;

      const fcmToken = ownerDoc.data().fcmToken;
      if (!fcmToken) {
        console.log(`Chủ sách ${ownerId} chưa đăng ký fcmToken.`);
        return null;
      }

      const message = {
        token: fcmToken,
        notification: {
          title: `Bình luận mới về "${bookTitle}" 💬`,
          body: `${commenterName}: ${content}`,
        },
        data: {
          bookId: bookId,
          commentId: context.params.commentId,
          type: "NEW_COMMENT",
        },
      };

      await admin.messaging().send(message);
      console.log(`Đã gửi FCM thông báo bình luận mới tới chủ sách ${ownerId}`);
    } catch (err) {
      console.error("Lỗi gửi FCM trong onCommentCreated:", err);
    }
    return null;
  });
