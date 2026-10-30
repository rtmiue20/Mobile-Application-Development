# BookShare - Nền tảng Chia sẻ & Trao tặng Sách cũ

## 1. Giới thiệu dự án
**BookShare** là ứng dụng di động Android hiện đại được phát triển nhằm kết nối cộng đồng sinh viên và bạn đọc yêu sách, tạo không gian trao tặng, trao đổi và chia sẻ giáo trình, sách cũ miễn phí hoặc chi phí tượng trưng. Ứng dụng thúc đẩy văn hóa đọc văn minh và lan tỏa tri thức trong cộng đồng học thuật.

## 2. Kiến trúc & Công nghệ sử dụng
- **Kiến trúc ứng dụng**: MVVM (Model - View - ViewModel) kết hợp Repository Pattern và Room Database Cache.
- **Ngôn ngữ & Nền tảng**: Java, Android SDK (minSdk 26, targetSdk 35), Gradle Kotlin DSL (`build.gradle.kts`).
- **Giao diện & UI/UX**: Material Design 3, Vector Drawables, ConstraintLayout, Hỗ trợ màn hình xoay ngang (`layout-land`).
- **Backend & Cơ sở dữ liệu Cloud**: Firebase Authentication, Google Credential Manager API, Cloud Firestore (8 collections bảo mật qua Security Rules).
- **Cơ sở dữ liệu Ngoại tuyến**: Room SQLite Database (Version 9) với cơ chế tự động đồng bộ 2 chiều và xóa dữ liệu khi có thay đổi trên Cloud.
- **Thông báo & Kết nối**: Firebase Cloud Messaging (FCM HTTP v1), OkHttp, Glide Image Loading.

## 3. Các tính năng chính của ứng dụng
1. **Xác thực an toàn (Authentication)**:
   - Đăng nhập / Đăng ký bằng Email & Mật khẩu.
   - Đăng nhập một chạm bằng Google Sign-In thông qua Credential Manager API mới nhất.
   - Quên mật khẩu & gửi liên kết đặt lại mật khẩu qua email.
   - Chế độ xem cho Khách (Guest mode) cho phép trải nghiệm tìm kiếm và xem sách trước khi đăng nhập.
2. **Quản lý Hồ sơ & Điểm uy tín (Profile & Reputation)**:
   - Cập nhật thông tin cá nhân, ảnh đại diện, địa chỉ Tỉnh/Thành, Quận/Huyện, Phường/Xã và tọa độ GPS.
   - Thống kê số sách đã cho, số sách đã nhận và Điểm uy tín (Reputation Score) dựa trên đánh giá thực tế.
   - Bắt buộc đổi mật khẩu khi Quản trị viên cấp lại mật khẩu tạm thời.
3. **Đăng bài & Chia sẻ sách (Create Post)**:
   - Đăng bài với hình ảnh chụp thực tế (Camera/Gallery), phân loại danh mục (Giáo trình, Kinh tế, Văn học,...), chọn độ mới sách.
   - 3 Hình thức linh hoạt: Tặng miễn phí (0đ), Trao đổi sách khác, hoặc Phí tượng trưng / Giá rẻ.
   - Định vị điểm hẹn giao nhận mặc định tại khuôn viên các cơ sở UEH (Cơ sở B, Cơ sở A, Cơ sở N,...).
   - Tối ưu hóa giao diện xoay ngang (Landscape Mode) với bố cục 2 cột trực quan.
4. **Trang chủ, Lọc & Tìm kiếm (Home & Search)**:
   - Lọc nhanh theo danh mục dạng cuộn ngang (Horizontal Chips).
   - Tìm kiếm sách thời gian thực theo tiêu đề và tác giả.
   - Hộp thoại bộ lọc nâng cao: Lọc theo tình trạng sách, khoảng cách địa lý và hình thức chia sẻ.
5. **Chi tiết sách & Định vị khoảng cách (Book Detail & GPS)**:
   - Tính toán khoảng cách chính xác từ vị trí người dùng đến cuốn sách bằng công thức Haversine.
   - Đánh dấu sách yêu thích (Favorites) hỗ trợ xem lại ngoại tuyến.
6. **Hỏi đáp & Bình luận cộng đồng (Comments)**:
   - Bảng bình luận độc lập ở tầng root với khóa ngoại `bookId` trỏ tới sách.
   - Hiển thị huy hiệu "Chủ sách" nổi bật, cho phép chủ sách và tác giả bình luận quản lý/xóa bình luận.
7. **Quản lý yêu cầu & Giao nhận an toàn (Manage Requests)**:
   - Gửi yêu cầu nhận sách kèm lời nhắn cho chủ sách.
   - Quy trình duyệt yêu cầu rõ ràng: Đang chờ -> Đã duyệt -> Hoàn tất.
   - Mã QR xác thực và mã bảo mật bí mật khi gặp mặt trực tiếp để hoàn tất giao dịch và ngăn chặn gian lận.
   - Đánh giá sao (1 - 5 sao) và để lại nhận xét nâng cao điểm uy tín thành viên.
8. **Thông báo đẩy (Notifications & FCM)**:
   - Tự động gửi thông báo đẩy thời gian thực khi có người gửi yêu cầu nhận sách, chủ sách duyệt yêu cầu hoặc có bình luận mới.
9. **Cài đặt & Trung tâm trợ giúp (Settings & Help Center)**:
   - Bật/tắt thông báo đẩy FCM ngay trong app.
   - Nút ép đồng bộ tức thì toàn bộ sách ngoại tuyến về Room SQLite.
   - Tính toán dung lượng và giải phóng bộ nhớ đệm hình ảnh (Glide Cache) trong một chạm.
   - Trung tâm trợ giúp với câu hỏi thường gặp FAQ (Accordion đóng/mở mượt mà), kênh gửi email hỗ trợ trực tiếp và hộp thoại góp ý/báo lỗi trong ứng dụng.
10. **Bảng điều khiển Quản trị viên (Admin Dashboard)**:
    - Thống kê toàn diện số lượng sách, người dùng, giao dịch hoàn tất.
    - Quản lý danh sách người dùng, phân quyền Admin, khóa tài khoản vi phạm có lý do rõ ràng.
    - Kiểm duyệt bài đăng sách mới của thành viên.
    - Giám sát sức khỏe cơ sở dữ liệu Firestore theo thời gian thực.

## 4. Các thành viên trong nhóm thực hiện
| STT | Họ và tên | Mã sinh viên | Vai trò / Công việc đảm nhận | % Hoàn thành |
| :---: | :--- | :---: | :--- | :---: |
| 1 | Nguyễn Văn Minh Triều | 31241027455 | Nhóm trưởng, Thiết kế UI/UX & Module Auth (Email, Google Sign-In) | **100%** |
| 2 | Triệu Nguyễn Huỳnh Khang | 31241022785 | Phát triển Trang chủ, Chi tiết sách & Đăng bài (Responsive landscape) | **100%** |
| 3 | Nguyễn Phúc Vượng | 31241025017 | Phát triển Quản lý yêu cầu, Bình luận & Tích hợp Firebase Firestore | **100%** |
| 4 | Ngô Trọng Phúc | 31241020074 | Phát triển Quản trị hệ thống (Admin), Cài đặt, Help Center & Báo cáo | **100%** |

## 5. Tài liệu & Báo cáo kỹ thuật
Tất cả tài liệu kỹ thuật, sơ đồ thiết kế và báo cáo tiến độ chi tiết được lưu trữ trong thư mục [docs/](docs/):
- **SRS (Đặc tả yêu cầu phần mềm)**: [docs/srs.md](docs/srs.md)
- **Sơ đồ kiến trúc & ERD (8 Collections)**: [docs/erd/erd-and-architecture.md](docs/erd/erd-and-architecture.md)
- **Tổng quan luồng giao diện Wireframes**: [docs/wireframes/wireframes-overview.md](docs/wireframes/wireframes-overview.md)
- **Báo cáo tiến độ các tuần**: [docs/weekly-reports/](docs/weekly-reports/) (Tuần 1, Tuần 2, Tuần 3, Tuần 4 và Báo cáo tổng kết)
- **Minh chứng làm việc nhóm**: [docs/report-evidence/teamwork-evidence.md](docs/report-evidence/teamwork-evidence.md)
- **Phân công công việc chi tiết**: [phân công công việc.md](phân%20công%20công%20việc.md)
