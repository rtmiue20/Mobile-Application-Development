# Software Requirements Specification (SRS) - BookShare
**Ứng dụng Di động Chia sẻ và Trao đổi Sách - UEH BookShare**

---

## 1. Giới thiệu dự án (Project Overview)
**BookShare** là ứng dụng di động Android hiện đại được phát triển theo tiêu chuẩn Material 3, nhằm xây dựng mạng lưới kết nối sinh viên và cộng đồng yêu thích đọc sách, chia sẻ tài liệu và trao đổi sách theo nhu cầu cá nhân.

---

## 2. Các đối tượng người dùng (User Roles)

| Vai trò | Mô tả | Quyền hạn chính |
| :--- | :--- | :--- |
| **Khách (Guest)** | Người dùng chưa đăng ký hoặc chưa đăng nhập vào hệ thống. | - Trải nghiệm chế độ "Xem như Khách" (Guest Mode).<br>- Khám phá danh sách sách và xem nội dung công khai.<br>- Quyết định có muốn đăng ký hay không. |
| **Thành viên (Member)** | Sinh viên hoặc người dùng đã xác thực danh tính qua Email hoặc Google Sign-In. | - Quản lý hồ sơ cá nhân, cơ sở UEH, sách yêu thích và sách của mình.<br>- Đăng bài, tìm kiếm, trao đổi và đánh giá sách.<br>- Thực hiện giao dịch, bình luận và tương tác cộng đồng. |
| **Quản trị viên (Admin)** | Người dùng được phân quyền hệ thống (`role: 'admin'`). | - Theo dõi thống kê hệ thống.<br>- Quản lý người dùng và tài khoản.<br>- Kiểm duyệt bài đăng và giám sát hoạt động của ứng dụng. |

---

## 3. Các yêu cầu chức năng (Functional Requirements)

### FR01 - Đăng ký, đăng nhập và quản lý phiên
- Người dùng có thể đăng ký tài khoản mới bằng Email và Mật khẩu.
- Hệ thống kiểm tra định dạng email, mật khẩu và tránh trùng lặp tài khoản.
- Người dùng có thể đăng nhập bằng Email, Google Sign-In hoặc Nickname/Username.
- Hệ thống hỗ trợ chức năng quên mật khẩu qua Email.
- Nếu tài khoản bị khóa bởi Admin, hệ thống từ chối truy cập ngay.
- Người dùng có thể sử dụng chế độ “Xem như Khách” khi chưa đăng nhập.

### FR02 - Quản lý hồ sơ cá nhân và vị trí cơ sở UEH
- Người dùng có thể xem và chỉnh sửa thông tin cá nhân như tên, ảnh đại diện, email, số điện thoại, tiểu sử.
- Hệ thống cho phép chọn cơ sở UEH mặc định hoặc sử dụng vị trí GPS thực tế.
- Người dùng quản lý danh sách “Sách của tôi” và “Sách yêu thích”.

### FR03 - Đăng bài chia sẻ sách
- Người dùng có thể tạo bài đăng sách với đầy đủ thông tin: tiêu đề, tác giả, danh mục, tình trạng sách, hình thức chia sẻ.
- Hệ thống hỗ trợ upload ảnh từ Camera hoặc Thư viện, đồng thời nén ảnh tối ưu trước khi tải lên.
- Người dùng có thể chọn vị trí chia sẻ dựa trên cơ sở UEH hoặc vị trí GPS hiện tại.
- Giao diện hỗ trợ layout ngang cho tablet và màn hình rộng.

### FR04 - Khám phá, tìm kiếm và lọc sách
- Người dùng có thể tìm kiếm sách theo tên hoặc tác giả theo thời gian thực.
- Hệ thống cho phép lọc sách theo danh mục và hiển thị kết quả dưới dạng chip/tab.
- Ứng dụng tính toán khoảng cách từ vị trí người dùng đến cuốn sách và hiển thị theo km.
- Danh sách sách hỗ trợ phân trang và làm mới dữ liệu bằng swipe-to-refresh.

### FR05 - Xem chi tiết sách và tương tác cộng đồng
- Người dùng có thể xem thông tin chi tiết sách, ảnh bìa lớn, mô tả, trạng thái, khoảng cách và thông tin chủ sách.
- Hệ thống hỗ trợ gọi điện hoặc mở bản đồ chỉ đường trực tiếp từ chi tiết sách.
- Người dùng có thể thả tim yêu thích và đồng bộ trạng thái yêu thích lên local/remote data.
- Người dùng có thể bình luận và xóa bình luận của chính mình hoặc do chủ sở hữu sách quản lý.

### FR06 - Yêu cầu nhận sách và xử lý giao dịch
- Người nhận có thể gửi yêu cầu mượn/nhận/đổi sách kèm lời nhắn cho chủ sách.
- Chủ sách nhận được thông báo và xem danh sách yêu cầu nhận sách.
- Chủ sách có quyền chấp nhận hoặc từ chối yêu cầu.

### FR07 - Tạo mã QR và xác thực giao nhận
- Hệ thống tạo mã QR Code duy nhất cho mỗi giao dịch đã được chấp nhận.
- Một bên có thể quét mã QR hoặc nhập mã bí mật để xác thực hoàn tất giao dịch.
- Sau khi xác nhận, trạng thái sách tự động chuyển sang “Đã trao đổi”.

### FR08 - Đánh giá và cập nhật uy tín
- Sau khi giao dịch hoàn tất, hai bên có thể đánh giá nhau từ 1 đến 5 sao kèm nhận xét.
- Hệ thống tự động tính điểm trung bình đánh giá và cập nhật vào hồ sơ người dùng.
- Điểm uy tín (reputation score) được sử dụng để đánh giá độ tin cậy của người dùng.

### FR09 - Sách yêu thích và lưu trữ offline
- Hệ thống lưu trữ sách yêu thích trong Room Database để người dùng vẫn xem được khi offline.
- Khi mất kết nối mạng, người dùng vẫn có thể duyệt các bài đăng đã lưu trong danh sách yêu thích.
- Dữ liệu local và Firestore được đồng bộ hai chiều thủ công hoặc tự động.

### FR10 - Thông báo thời gian thực và FCM
- Ứng dụng tích hợp Firebase Cloud Messaging để gửi thông báo khi có yêu cầu mới, trạng thái cập nhật hoặc giao dịch.
- Hệ thống lưu lịch sử thông báo và hiển thị badge số lượng chưa đọc.
- Khi nhấn vào thông báo, ứng dụng chuyển hướng tới màn hình tương ứng.

### FR11 - Cài đặt ứng dụng và quản lý bộ nhớ
- Người dùng có thể bật/tắt thông báo đẩy trong thiết lập ứng dụng.
- Hệ thống cho phép đồng bộ dữ liệu Room một cách cưỡng bức từ Firestore.
- Người dùng có thể xóa bộ nhớ cache hình ảnh tạm thời để giải phóng dung lượng lưu trữ.

### FR12 - Trung tâm trợ giúp và phản hồi
- Ứng dụng cung cấp hộp thoại trợ giúp có dạng FAQ, hỗ trợ người dùng tìm hiểu cách đăng bài, tìm sách và quy trình giao dịch.
- Người dùng có thể liên hệ hỗ trợ qua Email với tiêu đề và thông tin đã chuẩn hóa sẵn.
- Người dùng có thể gửi góp ý hoặc báo lỗi trong ứng dụng qua form phản hồi.

### FR13 - Quản trị hệ thống và kiểm duyệt người dùng
- Admin có dashboard thống kê tổng số sách, sách có sẵn, sách đã trao đổi, người dùng và tài khoản bị khóa.
- Admin có quyền tìm kiếm người dùng theo tên, email hoặc nickname.
- Admin có quyền khóa/mở khóa tài khoản và cấp lại mật khẩu tạm thời cho người dùng.

### FR14 - Kiểm duyệt nội dung và giám sát hệ thống
- Admin xem danh sách bài đăng sách và có quyền xóa nội dung vi phạm chính sách.
- Hệ thống giám sát trạng thái kết nối và độ trễ của Firestore theo mức xanh/vàng/đỏ.
- Admin có thể theo dõi tình trạng hệ thống để đảm bảo dịch vụ hoạt động ổn định.

### FR15 - Bảo mật, phân quyền và kiểm soát truy cập
- Hệ thống áp dụng Firestore Security Rules để kiểm soát quyền truy cập dữ liệu theo vai trò người dùng.
- Mật khẩu không được lưu ở dạng plain text; phiên đăng nhập được bảo mật bằng Firebase Auth Token.
- Chỉ người có quyền sở hữu mới được thực hiện thao tác sửa/xóa bài đăng, bình luận và dữ liệu liên quan.
- Ứng dụng kiểm tra trạng thái tài khoản và các điều kiện quyền trước khi cho phép thực hiện chức năng.

---

## 4. Yêu cầu phi chức năng (Non-Functional Requirements)

- **NFR01 - Hiệu năng (Performance)**:
  - Thời gian khởi động ứng dụng lạnh (Cold Start) < 1.5 giây.
  - Tải ảnh mượt mà với thư viện Glide, hỗ trợ bộ nhớ đệm Disk & Memory cache.
  - Truy vấn dữ liệu có phân trang (limit 20 items/trang), tránh tràn bộ nhớ heap.
- **NFR02 - Bảo mật (Security)**:
  - Phân quyền chặt chẽ thông qua Cloud Firestore Security Rules v2 (8 collections).
  - Không lưu mật khẩu dạng plain text; bảo mật phiên đăng nhập qua Firebase Auth Token.
  - Kiểm tra quyền sở hữu bài đăng và bình luận trước khi thực hiện thao tác xóa/sửa.
- **NFR03 - Tính khả dụng & Thích ứng (Usability & Responsiveness)**:
  - Thiết kế tuân thủ triệt để ngôn ngữ thiết kế Google Material 3.
  - Hỗ trợ màn hình dọc tiêu chuẩn và màn hình xoay ngang (`layout-land`).
  - Hỗ trợ Chế độ xem cho Khách giúp gia tăng trải nghiệm người dùng trước khi quyết định đăng ký.
- **NFR04 - Độ tin cậy & Ngoại tuyến (Reliability & Offline-first)**:
  - Duy trì khả năng xem sách yêu thích cả khi ngắt kết nối mạng thông qua Room Database v9.
  - Tự động bắt lỗi ngoại lệ mạng, hiển thị thông báo Snackbar/Toast thân thiện thay vì làm sập ứng dụng (Crash).
- **NFR05 - Tương thích thiết bị (Compatibility)**:
  - Hỗ trợ Android 8.0 (API level 26 - Oreo) trở lên, tương thích tối đa với Android 14/15/16 (API 34 - 37).
