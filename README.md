# BookShare - Nền tảng Chia sẻ & Trao tặng Sách cũ

## 1. Giới thiệu dự án

**BookShare** là một ứng dụng di động Android được phát triển nhằm kết nối cộng đồng yêu sách, giúp người dùng chia sẻ, trao đổi và tặng miễn phí các cuốn sách cũ trong môi trường học tập và sinh hoạt.

Dự án tập trung vào việc tạo ra một nền tảng đơn giản, thân thiện và hiệu quả để:
- giúp người có sách dư thừa chia sẻ với cộng đồng;
- tạo điều kiện cho người khác tiếp cận tài liệu học tập với chi phí thấp hoặc miễn phí;
- xây dựng cộng đồng đọc sách và trao đổi kiến thức.

BookShare phù hợp cho môi trường sinh viên và người dùng có nhu cầu tìm kiếm sách học tập, giáo trình, sách tham khảo, hoặc những cuốn sách hay để đọc và chia sẻ.

## 2. Tính năng chính

### 2.1 Xác thực và quản lý người dùng
- Đăng nhập / đăng ký bằng Email và mật khẩu
- Đăng nhập bằng Google Sign-In qua Credential Manager
- Chế độ xem cho Khách (Guest mode)
- Quản lý hồ sơ cá nhân, avatar, địa chỉ và vị trí GPS
- Đổi mật khẩu, đặt lại mật khẩu qua email

### 2.2 Đăng bài và chia sẻ sách
- Đăng bài với hình ảnh thực tế của cuốn sách
- Chọn danh mục sách, tình trạng sách, hình thức chia sẻ
- Hỗ trợ các tùy chọn: Tặng miễn phí, Trao đổi, hoặc Phí tượng trưng
- Thiết lập điểm hẹn giao nhận phù hợp

### 2.3 Trang chủ, lọc và tìm kiếm
- Hiển thị danh sách sách trên trang chủ
- Tìm kiếm theo tên sách, tác giả và danh mục
- Lọc theo điều kiện như tình trạng sách, khoảng cách, hình thức chia sẻ
- Hỗ trợ danh sách phân trang và trải nghiệm dễ sử dụng

### 2.4 Chi tiết sách và khoảng cách
- Xem thông tin chi tiết của từng cuốn sách
- Tính khoảng cách từ vị trí người dùng đến người cho sách
- Hỗ trợ lưu yêu thích và xem lại sách quan tâm

### 2.5 Yêu cầu, bình luận và tương tác cộng đồng
- Gửi yêu cầu nhận sách hoặc trao đổi
- Theo dõi trạng thái yêu cầu: chờ duyệt, đã duyệt, hoàn tất
- Bình luận và phản hồi trên từng cuốn sách
- Thông báo thời gian thực cho các hoạt động mới

### 2.6 Quản trị hệ thống (Admin)
- Thống kê tổng quan về người dùng, sách và giao dịch
- Quản lý danh sách người dùng
- Phân quyền Admin
- Khóa tài khoản vi phạm
- Kiểm duyệt bài đăng mới

## 3. Công nghệ sử dụng

- Ngôn ngữ: Java
- Nền tảng: Android SDK
- UI: Material Design 3, ConstraintLayout, layout-land hỗ trợ màn hình xoay ngang
- Xác thực: Firebase Authentication, Google Credential Manager
- Cơ sở dữ liệu: Cloud Firestore, Room SQLite
- Thông báo: Firebase Cloud Messaging (FCM)
- Hình ảnh: Glide
- Định vị: GPS / tính khoảng cách theo công thức Haversine
- Mô hình kiến trúc: MVVM + Repository Pattern

## 4. Cấu trúc repository

```text
Mobile-Application-Development/
├── Project/                  # Mã nguồn Android chính của dự án
├── docs/                     # Tài liệu kỹ thuật, sơ đồ, báo cáo
├── README.md                 # Tổng quan dự án
├── plan-BookShare.md         # Kế hoạch và tiến độ dự án
├── phân công công việc.md    # Phân công nhiệm vụ nhóm
└── .github/                  # Cấu hình GitHub, CI/CD hoặc tài liệu liên quan
```

## 5. Thành viên nhóm

| STT | Họ và tên | Mã sinh viên | Vai trò chính |
| :---: | :--- | :---: | :--- |
| 1 | Nguyễn Văn Minh Triều | 31241027455 | Nhóm trưởng, thiết kế UI/UX, Auth, thông báo |
| 2 | Triệu Nguyễn Huỳnh Khang | 31241022785 | Xác thực, hồ sơ người dùng, giao diện chính |
| 3 | Nguyễn Phúc Vượng | 31241025017 | Bình luận, yêu cầu, đánh giá, Firestore |
| 4 | Ngô Trọng Phúc | 31241020074 | Trang chủ, đăng bài, admin dashboard |

## 6. Tài liệu và báo cáo

Dự án đi kèm với các tài liệu kỹ thuật và báo cáo đầy đủ trong thư mục [Project](Project/), bao gồm:
- [Project/README.md](Project/README.md)
- [Project/docs](Project/docs/)
- [plan-BookShare.md](plan-BookShare.md)
- [phân công công việc.md](phân%20công%20công%20việc.md)

## 7. Hướng dẫn chạy dự án

1. Mở project bằng Android Studio.
2. Đồng bộ Gradle và tải các dependency cần thiết.
3. Kết nối Firebase cho project theo cấu hình phù hợp.
4. Build và chạy ứng dụng trên thiết bị ảo hoặc thiết bị Android thật.

## 8. Mục tiêu của dự án

BookShare không chỉ là một ứng dụng chia sẻ sách, mà còn là một giải pháp thực tế để thúc đẩy việc tái sử dụng tài liệu học tập, giảm lãng phí, tăng khả năng kết nối cộng đồng và hỗ trợ môi trường học tập bền vững.

---

Nếu bạn muốn, mình có thể tiếp tục giúp bạn:
- chỉnh lại README theo phong cách “dự án học tập chuyên nghiệp” hơn;
- viết thêm phần “Hướng dẫn cài đặt chi tiết”;
- tạo phiên bản README tiếng Anh hoặc tiếng Việt theo chuẩn GitHub.
