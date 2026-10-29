# Wireframes & UI Flow - BookShare

## 1. Luồng màn hình (User UI Flow)
- **Khởi động**: `MainActivity` -> Kiểm tra đăng nhập/Khách -> `LogInActivity` hoặc `HomeActivity`.
- **Đăng nhập / Đăng ký**:
  - `LogInActivity`: Đăng nhập Email, Đăng nhập Google (Credential Manager), Đăng ký, Quên mật khẩu, **Xem như Khách**.
  - `RegisterActivity`: Đăng ký tài khoản mới với thông tin cá nhân.
- **Trang chủ (`HomeActivity`)**:
  - Thanh tìm kiếm, danh mục sách (Tất cả, Văn học, Kinh tế, Giáo khoa, Khác).
  - Danh sách bài đăng sách theo dạng lưới/thẻ (CardView).
  - Nút thêm bài đăng (`+`), chuyển trang, điều hướng cá nhân/quản lý yêu cầu.
- **Chi tiết sách (`BookDetailActivity`)**:
  - Hình ảnh sách lớn, thông tin tác giả, danh mục, tình trạng, khoảng cách địa lý, thông tin người đăng, nút gửi yêu cầu nhận sách, bình luận.
- **Đăng bài (`CreatePostActivity`)**:
  - Form nhập tên sách, tác giả, chọn ảnh từ Camera/Gallery, chọn hình thức chia sẻ, định vị GPS / Tỉnh thành, mô tả. Hỗ trợ xoay ngang (`layout-land`).
- **Quản lý yêu cầu (`ManageRequestsActivity`)**:
  - Xem các yêu cầu nhận sách gửi đi và nhận về, chấp nhận hoặc từ chối yêu cầu.
- **Quản trị (`AdminDashboardActivity`)**:
  - Thống kê hệ thống, quản lý người dùng (khóa/mở khóa, cấp lại mật khẩu).
