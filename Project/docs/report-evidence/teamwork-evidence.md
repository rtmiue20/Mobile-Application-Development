# Minh chứng Làm việc Nhóm & Tiến độ Dự án (Teamwork & Technical Evidence)
**Dự án: Ứng dụng Di động Chia sẻ và Trao đổi Sách - UEH BookShare**

---

## 1. Phân công Vai trò & Trách nhiệm Thành viên (Team Roles)

Dự án được triển khai bởi nhóm 4 thành viên với sự phân định rõ ràng về trách nhiệm kỹ thuật và quản trị:

| Thành viên | Vai trò chính | Trách nhiệm & Module đảm nhiệm | Tỷ lệ đóng góp |
| :--- | :--- | :--- | :---: |
| **Nguyễn Văn Minh Triều**<br>(MSSV: 31241027455) | **Nhóm trưởng (Leader)** | - Khởi tạo dự án, thiết kế khung kiến trúc MVVM, cấu hình Firebase và quản lý repo.<br>- Quản lý và duyệt toàn bộ Pull Request (#22 - #69), kiểm thử hợp nhất mã nguồn.<br>- Xây dựng Chế độ xem cho Khách (Guest mode) và Luồng chuông thông báo trong app.<br>- Xây dựng kiểm duyệt bài đăng sách (Admin Posts) và tối ưu hóa logic yêu cầu Request. | **100% (Hoàn thành)** |
| **Triệu Nguyễn Huỳnh Khang**<br>(MSSV: 31241022785) | **Thành viên (Member)** | - Phát triển Module Xác thực (Auth: Đăng ký, Đăng nhập, Google Sign-In Credential Manager).<br>- Phát triển Màn hình Quên mật khẩu và Hồ sơ cá nhân (Edit Profile) theo bản thiết kế chuẩn.<br>- Thiết kế hệ thống Vector Icons chuẩn Material (`ic_lock_reset`, huy hiệu thư).<br>- Triển khai cơ chế Tự động đăng nhập Remember Me & đồng bộ xóa sách trong Room. | **100% (Hoàn thành)** |
| **Nguyễn Phúc Vượng**<br>(MSSV: 31241025017) | **Thành viên (Member)** | - Phát triển tính năng Bình luận cộng đồng (thêm, sửa, xóa comments) & Sách yêu thích.<br>- Phát triển Quản lý yêu cầu mượn/đổi sách, Hộp thoại xác nhận & Đánh giá Rating.<br>- Tích hợp thông báo đẩy Firebase Cloud Messaging (FCM Push) & token trigger.<br>- Lập trình tính toán khoảng cách động GPS Haversine và giao diện xoay ngang (`layout-land`). | **100% (Hoàn thành)** |
| **Ngô Trọng Phúc**<br>(MSSV: 31241020074) | **Thành viên (Member)** | - Phát triển Trang chủ, Tìm kiếm thời gian thực & Lọc danh mục trượt ngang.<br>- Phát triển Màn hình Đăng bài chia sẻ sách (`CreatePostActivity`) & Chi tiết sách.<br>- Xây dựng Bảng điều khiển Quản trị viên (Admin Dashboard, Quản lý & Khóa tài khoản, DB Health).<br>- Thiết lập & chuẩn hóa hệ thống phân quyền Cloud Firestore Security Rules v2 (8 collections).<br>- Phát triển Cài đặt ứng dụng (`AppSettingsDialog`), Trung tâm trợ giúp FAQ & Toàn bộ tài liệu đồ án. | **100% (Hoàn thành)** |

---

## 2. Quy trình Quản lý Dự án & Kỹ thuật (Engineering Workflow)

### 2.1. Quản lý Phiên bản Mã nguồn (Git & GitHub Workflow)
- **Mô hình nhánh**:
  - Nhánh `main`: Nhánh ổn định, chứa các phiên bản phát hành chính thức.
  - Nhánh `TrongPhuc` & nhánh tính năng: Nhánh làm việc trực tiếp, thực hiện commit từng phần, đồng bộ thường xuyên với `origin/main`.
- **Thực hành Pull Request & Code Review**:
  - Mọi thay đổi lớn (Security Rules, Refactor Room v9, Dọn rác layout) đều được kiểm tra biên dịch (`assembleDebug`) trước khi tạo Pull Request và merge.
  - Xử lý xung đột (Conflict resolution) chặt chẽ bằng cách thực hiện `git merge origin/main` định kỳ.

### 2.2. Cấu hình Dịch vụ Backend (Firebase Cloud Platform)
- **Firebase Authentication**:
  - Hỗ trợ đăng nhập Email/Password với quy trình xác thực mạnh mẽ.
  - Tích hợp **Google Sign-In** thông qua `androidx.credentials:credentials:1.3.0` và `GetSignInWithGoogleOption`, cấu hình đầy đủ mã băm chứng chỉ SHA-1 và SHA-256 từ keystore phát triển vào Firebase Console.
- **Cloud Firestore NoSQL Database**:
  - Tổ chức 8 collections gốc chuẩn hóa: `users`, `books`, `requests`, `comments`, `favorites`, `notifications`, `reviews`, `feedbacks`.
  - Bộ quy tắc bảo mật **Firestore Security Rules v2** được kiểm thử nghiêm ngặt, ngăn chặn leo quyền Admin, bảo vệ dữ liệu người dùng bị khóa và kiểm soát quyền xóa bài viết/bình luận.
- **Firebase Cloud Messaging (FCM)**:
  - Cấu hình Service nền `MyFirebaseMessagingService` tiếp nhận và phân phối thông báo đẩy tức thì.

### 2.3. Môi trường Phát triển & Thư viện
- **IDE**: Android Studio Ladybug / Meerkat (Windows x64).
- **Ngôn ngữ**: Java 17, Android SDK 37 (UpsideDownCake/Baklava), Min SDK 26 (Android 8.0 Oreo).
- **Thư viện cốt lõi**:
  - `androidx.room:room-runtime:2.6.1` & `room-compiler`: Quản lý SQLite cục bộ.
  - `com.github.bumptech.glide:glide:4.16.0`: Tải và lưu đệm hình ảnh.
  - `com.google.android.material:material:1.12.0`: Thiết kế giao diện Material 3.
  - `com.google.zxing:core:3.5.3`: Tạo và đọc mã QR Code.

---

## 3. Nhật ký Tối ưu & Tinh gọn Mã nguồn (Refactoring & Quality Assurance)

### 3.1. Dọn dẹp tài nguyên dư thừa (Dead Code & Asset Cleanup)
Trong quá trình rà soát dự án, nhóm đã thực hiện kiểm tra đối chiếu toàn bộ thư viện tài nguyên và xóa bỏ 9 tệp tin dư thừa không còn sử dụng để giảm kích thước file APK:
1. `activity_main.xml`: Đã thay thế bằng điều hướng Splash động trong `MainActivity.java`.
2. `bg_fav_btn.xml` & `bg_favorite_btn.xml`: Trùng lặp, thống nhất dùng `bg_favorite_button.xml`.
3. `bg_distance.xml` & `bg_image_overlay.xml`: Các drawable cũ đã được thay thế bằng Material CardView và thuộc tính `strokeColor`.
4. `ic_format_quote.xml`, `ic_thumb_up.xml`, `ic_reply.xml`, `ic_calendar_today.xml`: Các icon vector không còn gắn vào bất kỳ layout nào.

### 3.2. Kiểm thử Tính năng (Testing & Verification)
- **Kiểm thử Xác thực (Auth Testing)**:
  - Đăng ký thành công, chặn đăng ký khi trùng Email.
  - Đăng nhập bằng tài khoản bị khóa (`status: 'locked'`) -> Ứng dụng phát hiện và chặn đăng nhập kèm thông báo.
  - Đăng nhập tài khoản vừa được cấp lại mật khẩu -> Chuyển hướng bắt buộc sang màn hình đổi mật khẩu (`mustChangePassword: true`).
- **Kiểm thử Vận hành Ngoại tuyến (Offline-First Testing)**:
  - Bật chế độ máy bay (Airplane Mode): Màn hình "Sách yêu thích" vẫn tải nhanh và hiển thị đầy đủ thông tin sách nhờ Room Database v9.
  - Khi bật lại mạng: Nhấn "Đồng bộ dữ liệu Room" trong Cài đặt ứng dụng -> Dữ liệu cục bộ được đồng bộ hóa tức thì với Cloud Firestore.
- **Kiểm thử Bộ nhớ đệm (Cache Cleaning)**:
  - Nút "Xóa bộ nhớ đệm (Cache)" trong Cài đặt kích hoạt tiến trình chạy nền xóa toàn bộ Disk Cache của Glide, kiểm tra dung lượng bộ nhớ giải phóng thành công.
