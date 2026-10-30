# Minh chứng Làm việc Nhóm & Tiến độ Dự án (Teamwork & Technical Evidence)
**Dự án: Ứng dụng Di động Chia sẻ và Trao đổi Sách - UEH BookShare**

---

## 1. Phân công Vai trò & Trách nhiệm Thành viên (Team Roles)

Dự án được triển khai bởi nhóm 4 thành viên với sự phân định rõ ràng về trách nhiệm kỹ thuật và quản trị:

| Thành viên | Vai trò chính | Trách nhiệm & Module đảm nhiệm | Tỷ lệ đóng góp |
| :--- | :--- | :--- | :---: |
| **Trọng Phúc** | **Team Leader / Lead Architect** | - Khởi tạo dự án, thiết kế kiến trúc tổng thể MVVM & Repository Pattern.<br>- Xây dựng hệ thống Xác thực (Firebase Auth & Google Credential Manager API).<br>- Thiết lập & chuẩn hóa hệ thống phân quyền Cloud Firestore Security Rules v2 (8 collections).<br>- Phát triển Module Quản trị viên (Admin Dashboard, Quản lý & Khóa tài khoản, Database Health Check).<br>- Phát triển Module Cài đặt ứng dụng (`AppSettingsDialog`) & Trung tâm trợ giúp (`HelpCenterDialog`). | **100% (Hoàn thành)** |
| **Minh Anh** | **UI/UX Designer & Frontend Dev** | - Thiết kế giao diện người dùng theo chuẩn Google Material 3.<br>- Phát triển Trang chủ (`HomeActivity`) với tìm kiếm thời gian thực và bộ lọc phân loại theo Chip.<br>- Phát triển Màn hình Chi tiết sách (`BookDetailActivity`) và tính khoảng cách địa lý GPS (Haversine).<br>- Xây dựng tính năng Bình luận cộng đồng (`CommentAdapter`, `CommentViewModel`) hỗ trợ phân quyền xóa. | **100% (Hoàn thành)** |
| **Hoàng Long** | **Feature Developer** | - Xây dựng Module Đăng bài chia sẻ sách (`CreatePostActivity`).<br>- Tích hợp xử lý đa nguồn hình ảnh (Chụp ảnh từ Camera & Chọn ảnh từ Gallery thiết bị).<br>- Tối ưu hóa giao diện thích ứng đa màn hình, phát triển layout xoay ngang chuyên dụng (`layout-land/activity_create_post.xml`).<br>- Xử lý vị trí địa điểm Cơ sở UEH và tọa độ GPS khi đăng sách. | **100% (Hoàn thành)** |
| **Hải Đăng** | **Data & Backend Developer** | - Xây dựng Module Quản lý yêu cầu nhận/đổi sách (`ManageRequestsActivity`, `RequestAdapter`).<br>- Tích hợp quy trình tạo và quét mã QR Code để xác thực giao nhận sách an toàn.<br>- Xây dựng hệ thống Đánh giá & Chấm điểm uy tín thành viên (`reviews`).<br>- Triển khai cơ sở dữ liệu ngoại tuyến Android Room SQLite phiên bản 9 (`BookRoomDatabase`) hỗ trợ duyệt sách ngoại tuyến và đồng bộ hai chiều. | **100% (Hoàn thành)** |

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
