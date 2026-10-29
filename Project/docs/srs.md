# Software Requirements Specification (SRS) - BookShare

## 1. Giới thiệu dự án (Introduction)
**BookShare** là ứng dụng di động Android được phát triển nhằm xây dựng cộng đồng trao tặng, trao đổi và chia sẻ sách cũ miễn phí hoặc giá rẻ. Ứng dụng kết nối những người có sách không dùng đến với những người có nhu cầu đọc sách, thúc đẩy văn hóa đọc và tái sử dụng tri thức trong cộng đồng.

## 2. Các đối tượng người dùng (User Roles)
1. **Khách (Guest)**: Chưa đăng nhập, có thể xem danh sách sách công khai, xem chi tiết sách, tìm kiếm sách theo tên/tác giả/danh mục.
2. **Thành viên (Member)**: Đã đăng nhập bằng Email/Password hoặc Google. Có thể đăng bài chia sẻ sách, chỉnh sửa hồ sơ, yêu cầu nhận sách, bình luận, quản lý sách cá nhân, đổi mật khẩu bắt buộc khi được admin cấp lại.
3. **Quản trị viên (Admin)**: Quản lý người dùng (khóa/mở khóa tài khoản, cấp lại mật khẩu), kiểm duyệt bài đăng, quản lý hệ thống.

## 3. Các yêu cầu chức năng (Functional Requirements)
- **FR01 - Đăng ký & Đăng nhập (Auth)**: Hỗ trợ đăng ký tài khoản mới, đăng nhập bằng Email/Password, đăng nhập nhanh bằng Google (Credential Manager), quên mật khẩu và chế độ xem cho Khách.
- **FR02 - Quản lý Hồ sơ cá nhân (Profile)**: Cập nhật thông tin cá nhân, avatar, địa chỉ (Tỉnh/Thành, Quận/Huyện, Phường/Xã), định vị tọa độ GPS, đổi mật khẩu và quản lý sách yêu thích.
- **FR03 - Đăng bài chia sẻ sách (Create Post)**: Cho phép thành viên đăng sách cần tặng/đổi/bán với hình ảnh thực tế (Camera/Gallery), chọn danh mục, tình trạng sách, hình thức chia sẻ và vị trí giao lưu. Có hỗ trợ giao diện xoay ngang (Landscape).
- **FR04 - Trang chủ & Tìm kiếm (Home & Search)**: Hiển thị danh sách sách theo danh mục (Tất cả, Văn học, Kinh tế, Giáo khoa, Khác), tìm kiếm theo từ khóa, lọc theo điều kiện và phân trang.
- **FR05 - Chi tiết sách (Book Detail)**: Xem thông tin chi tiết cuốn sách, thông tin người đăng, khoảng cách ước tính từ người dùng đến sách, và trạng thái sách.
- **FR06 - Yêu cầu nhận sách & Trao đổi (Requests)**: Gửi yêu cầu nhận sách hoặc trao đổi sách đến người đăng, quản lý trạng thái yêu cầu (Đang chờ, Đã chấp nhận, Đã từ chối).
- **FR07 - Thông báo & FCM (Notifications)**: Nhận thông báo thời gian thực khi có yêu cầu mới hoặc trạng thái yêu cầu thay đổi.
- **FR08 - Bình luận (Comments)**: Thảo luận, hỏi đáp dưới bài đăng sách.
- **FR09 - Quản trị hệ thống (Admin Dashboard)**: Thống kê số lượng người dùng, bài đăng, yêu cầu; quản lý danh sách người dùng, khóa tài khoản vi phạm.

## 4. Yêu cầu phi chức năng (Non-Functional Requirements)
- **Hiệu suất**: Giao diện mượt mà với Material 3, phản hồi tương tác nhanh.
- **Bảo mật**: Xác thực an toàn qua Firebase Auth & Credential Manager, phân quyền Firestore Security Rules.
- **Tính thích ứng (Adaptive)**: Hỗ trợ linh hoạt trên các kích thước màn hình điện thoại, máy tính bảng và chế độ xoay ngang (Landscape).
