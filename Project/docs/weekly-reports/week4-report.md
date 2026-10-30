# Báo cáo Tuần 4 - Quản lý Yêu cầu QR, Room Offline v9, Quản trị Admin, Trợ giúp & Tinh gọn Hệ thống
**Dự án: Ứng dụng Di động Chia sẻ và Trao đổi Sách - UEH BookShare**

---

## 1. Mục tiêu Tuần 4
- Hoàn thiện toàn diện quy trình mượn, nhận và trao đổi sách kèm cơ chế xác thực giao nhận bằng mã QR Code.
- Triển khai giải pháp vận hành ngoại tuyến (Offline-First) với Android Room SQLite Database phiên bản 9.
- Xây dựng Bảng điều khiển Quản trị viên (Admin Dashboard) với chức năng khóa tài khoản, cấp lại mật khẩu và kiểm tra độ trễ mạng cơ sở dữ liệu.
- Phát triển Hộp thoại Cài đặt ứng dụng (quản lý thông báo FCM, đồng bộ dữ liệu Room, dọn dẹp cache) và Trung tâm trợ giúp người dùng (FAQ Accordion, gửi phản hồi in-app).
- Thiết lập và kiểm thử bộ quy tắc bảo mật Cloud Firestore Security Rules v2 cho toàn bộ 8 collections.
- Rà soát toàn bộ dự án, dọn dẹp mã nguồn và tài nguyên dư thừa, kiểm thử biên dịch và đóng gói đồ án.

---

## 2. Các công việc đã thực hiện chi tiết

### 2.1. Phân hệ Quản lý Yêu cầu & Xác thực Mã QR
- Xây dựng [ManageRequestsActivity.java](file:///d:/AndroidStudioProject/Project/app/src/main/java/vn/edu/ueh/rtmiue20/project/ui/request/ManageRequestsActivity.java) với 2 tab giao diện: "Yêu cầu nhận được" và "Yêu cầu đã gửi".
- Triển khai chức năng Chấp nhận (Accept) và Từ chối (Reject) yêu cầu mượn/đổi sách.
- Tích hợp thư viện ZXing (`com.google.zxing:core:3.5.3`) để sinh mã QR Code giao nhận độc nhất cho mỗi giao dịch; hỗ trợ quét mã để chuyển trạng thái giao dịch sang Hoàn thành.
- Xây dựng hộp thoại Chấm điểm sao uy tín (1-5 sao) và nhận xét sau khi hoàn tất giao dịch, cập nhật tự động lên hồ sơ thành viên.

### 2.2. Cơ sở Dữ liệu Ngoại tuyến Android Room SQLite v9
- Nâng cấp [BookRoomDatabase.java](file:///d:/AndroidStudioProject/Project/app/src/main/java/vn/edu/ueh/rtmiue20/project/data/local/BookRoomDatabase.java) lên phiên bản `version = 9`, lưu trữ 3 thực thể: `Book`, `Favorite`, `User`.
- Bổ sung các trường vị trí địa lý thực tế (`latitude`, `longitude`, `city`, `district`, `ward`) vào cơ sở dữ liệu cục bộ.
- Xây dựng cơ chế đồng bộ hai chiều (Two-way Synchronization) và lưu đệm danh sách sách yêu thích, hỗ trợ trải nghiệm người dùng ngay cả khi không có kết nối mạng Internet.

### 2.3. Hộp thoại Cài đặt Ứng dụng & Trung tâm Trợ giúp
- Xây dựng Hộp thoại Cài đặt ứng dụng ([dialog_app_settings.xml](file:///d:/AndroidStudioProject/Project/app/src/main/res/layout/dialog_app_settings.xml)):
  - Công tắc bật/tắt nhận thông báo đẩy FCM lưu trạng thái vào `SharedPreferences`.
  - Nút "Đồng bộ dữ liệu Room" cho phép cưỡng bức đồng bộ dữ liệu từ Firestore về máy.
  - Nút "Xóa bộ nhớ đệm (Cache)" kích hoạt luồng dọn dẹp an toàn bộ nhớ đệm hình ảnh Glide.
- Xây dựng Hộp thoại Trung tâm trợ giúp ([dialog_help_center.xml](file:///d:/AndroidStudioProject/Project/app/src/main/res/layout/dialog_help_center.xml)):
  - Danh mục câu hỏi thường gặp FAQ Accordion dạng Collapsible Card có hiệu ứng đóng/mở mượt mà.
  - Tích hợp Intent mở ứng dụng Email gửi trực tiếp tới hòm thư hỗ trợ sinh viên UEH.
  - Mở form Góp ý in-app gửi dữ liệu trực tiếp vào collection `feedbacks` trên Firestore.

### 2.4. Bảng điều khiển Quản trị viên & An ninh Hệ thống
- Phát triển [AdminDashboardActivity.java](file:///d:/AndroidStudioProject/Project/app/src/main/java/vn/edu/ueh/rtmiue20/project/ui/admin/AdminDashboardActivity.java) hiển thị thống kê tổng quan sách, người dùng và giao dịch thời gian thực.
- Phát triển [UserManagementActivity.java](file:///d:/AndroidStudioProject/Project/app/src/main/java/vn/edu/ueh/rtmiue20/project/ui/admin/UserManagementActivity.java) hỗ trợ tìm kiếm tài khoản, Khóa (Lock), Mở khóa (Unlock) và cấp lại mật khẩu tạm thời.
- Tích hợp công cụ giám sát sức khỏe kết nối cơ sở dữ liệu (Database Health Check Ping).
- Hoàn thiện tệp [firestore.rules](file:///d:/AndroidStudioProject/Project/firestore.rules) phân quyền chặt chẽ trên 8 collections: `users`, `books`, `requests`, `comments`, `favorites`, `notifications`, `reviews`, `feedbacks`.

### 2.5. Tinh gọn Tài nguyên & Kiểm thử Đồ án
- Rà soát toàn bộ dự án và xóa bỏ 9 tệp tin tài nguyên dư thừa không sử dụng (`activity_main.xml`, `bg_fav_btn.xml`, `bg_distance.xml`, `ic_reply.xml`, ...).
- Kiểm tra tính tương thích và khả năng biên dịch thành công 100% qua lệnh Gradle `assembleDebug`.

---

## 3. Đánh giá Kết quả Tuần 4
- **Tiến độ**: Hoàn thành 100% tất cả các mục tiêu đề ra cho tuần 4.
- **Chất lượng**: Ứng dụng vận hành trơn tru, bảo mật cao, giao diện Material 3 đồng nhất và tài liệu đồ án được cập nhật hoàn chỉnh.
