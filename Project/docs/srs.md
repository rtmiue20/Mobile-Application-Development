# Software Requirements Specification (SRS) - BookShare
**Ứng dụng Di động Chia sẻ và Trao đổi Sách - UEH BookShare**

---

## 1. Giới thiệu dự án (Project Overview)
**BookShare** là ứng dụng di động Android hiện đại được phát triển theo tiêu chuẩn Material 3, nhằm xây dựng mạng lưới kết nối sinh viên và cộng đồng yêu sách tại Đại học Kinh tế TP. Hồ Chí Minh (UEH) và các khu vực lân cận. Ứng dụng cho phép sinh viên trao tặng sách miễn phí, trao đổi giáo trình hoặc chia sẻ sách cũ với chi phí tiết kiệm; từ đó thúc đẩy văn hóa đọc, bảo vệ môi trường thông qua việc tái sử dụng tri thức và gắn kết sinh viên giữa các cơ sở đào tạo.

---

## 2. Các đối tượng người dùng (User Roles)

| Vai trò | Mô tả | Quyền hạn chính |
| :--- | :--- | :--- |
| **Khách (Guest)** | Người dùng chưa đăng ký hoặc chưa đăng nhập vào hệ thống. | - Trải nghiệm chế độ "Xem như Khách" (Guest Mode).<br>- Khám phá danh sách sách ở Trang chủ, tìm kiếm theo tiêu đề/tác giả, lọc theo thể loại.<br>- Xem chi tiết sách, đọc các bình luận cộng đồng.<br>*(Bị giới hạn không thể yêu cầu sách, đăng bài, bình luận, yêu thích)*. |
| **Thành viên (Member)** | Sinh viên hoặc người dùng đã xác thực danh tính qua Email hoặc Google Sign-In. | - Toàn quyền quản lý hồ sơ cá nhân, cập nhật Cơ sở UEH / Tọa độ GPS.<br>- Đăng tải bài viết chia sẻ sách (kèm ảnh chụp từ Camera hoặc Thư viện).<br>- Gửi và nhận yêu cầu mượn/đổi sách, tạo mã QR Code xác thực khi giao nhận sách.<br>- Thảo luận bình luận dưới bài đăng sách, xóa bình luận của chính mình.<br>- Lưu sách yêu thích và đọc ngoại tuyến (Offline Mode qua Room SQLite).<br>- Đánh giá và chấm điểm sao thành viên sau khi hoàn thành giao dịch.<br>- Góp ý, báo lỗi qua Hộp thoại Trợ giúp & Phản hồi. |
| **Quản trị viên (Admin)** | Người dùng được phân quyền hệ thống (`role: 'admin'`). | - Xem Dashboard thống kê số lượng sách, người dùng, giao dịch thời gian thực.<br>- Quản lý tài khoản: Khóa (Lock), Mở khóa (Unlock), Cấp lại mật khẩu tạm thời.<br>- Kiểm duyệt và xóa bài đăng sách vi phạm tiêu chuẩn cộng đồng.<br>- Giám sát độ trễ và tình trạng kết nối cơ sở dữ liệu (Database Health Check).<br>- Xem danh sách phản hồi (Feedbacks) gửi từ người dùng. |

---

## 3. Các yêu cầu chức năng (Functional Requirements)

### FR01 - Xác thực & Quản lý phiên (Authentication & Session)
- **FR01.1**: Đăng ký tài khoản mới bằng Email và Mật khẩu, tự động kiểm tra định dạng và chống trùng lặp.
- **FR01.2**: Đăng nhập nhanh bằng Google sử dụng **Google Credential Manager API** thế hệ mới (thay thế hoàn toàn API cũ bị deprecated).
- **FR01.3**: Đăng nhập bằng Email/Password hoặc Tra cứu theo Nickname/Username.
- **FR01.4**: Cơ chế Quên mật khẩu: gửi liên kết đặt lại mật khẩu an toàn qua Email Firebase Auth.
- **FR01.5**: Kiểm tra trạng thái tài khoản thời gian thực; nếu tài khoản bị Admin khóa (`status: 'locked'`), ứng dụng lập tức từ chối quyền truy cập và hiển thị thông báo.
- **FR01.6**: Xử lý mật khẩu tạm thời: Bắt buộc đổi mật khẩu (`mustChangePassword: true`) ngay sau khi đăng nhập thành công nếu vừa được Admin cấp lại mật khẩu.
- **FR01.7**: Hỗ trợ nút "Xem như Khách" (Guest Mode) ngay tại màn hình đăng nhập.

### FR02 - Quản lý Hồ sơ cá nhân & Vị trí Cơ sở (User Profile & UEH Campus)
- **FR02.1**: Hiển thị thông tin cá nhân: Ảnh đại diện, Tên hiển thị, Nickname, Email, Số điện thoại, Cơ sở UEH, Điểm uy tín (Reputation Score).
- **FR02.2**: Chỉnh sửa hồ sơ: Tải ảnh đại diện mới lên Cloud Storage / xử lý URI an toàn, cập nhật số điện thoại và tiểu sử.
- **FR02.3**: Thiết lập địa điểm mặc định: Chọn Cơ sở UEH (mặc định Cơ sở B - 279 Nguyễn Tri Phương, Quận 10) hoặc lấy tọa độ GPS thực tế của thiết bị qua FusedLocationProvider.
- **FR02.4**: Quản lý danh sách "Sách của tôi" (My Books) và "Sách yêu thích" (Favorites).

### FR03 - Đăng bài chia sẻ sách (Book Creation & Publishing)
- **FR03.1**: Nhập đầy đủ thông tin sách: Tiêu đề sách, Tên tác giả, Danh mục (Văn học, Kinh tế, Công nghệ, Giáo trình, Ngoại ngữ, Kỹ năng, Truyện tranh, Khác).
- **FR03.2**: Thiết lập Tình trạng sách (Mới 100%, 95%, 80%, Sách cũ chấp nhận được) và Hình thức chia sẻ (Tặng miễn phí, Trao đổi giáo trình, Bán lại giá rẻ).
- **FR03.3**: Chụp ảnh trực tiếp từ Camera thiết bị hoặc chọn từ Thư viện hình ảnh; nén ảnh tối ưu trước khi tải lên.
- **FR03.4**: Chọn vị trí chia sẻ: Tự động điền theo vị trí Cơ sở UEH của người đăng hoặc định vị GPS hiện tại.
- **FR03.5**: Hỗ trợ giao diện xoay ngang chuyên dụng (`layout-land/activity_create_post.xml`) phân tách 2 cột tiện dụng cho máy tính bảng và màn hình ngang.

### FR04 - Khám phá, Tìm kiếm & Lọc sách (Home, Search & Filter)
- **FR04.1**: Thanh tìm kiếm thời gian thực theo Tên sách hoặc Tác giả.
- **FR04.2**: Thanh lọc danh mục trượt ngang dạng Chip/Tab (Tất cả, Kinh tế, Văn học, Công nghệ, Giáo trình, Ngoại ngữ...).
- **FR04.3**: Tính toán và hiển thị khoảng cách địa lý (Haversine Formula) từ vị trí người dùng đến cuốn sách tính theo km.
- **FR04.4**: Cơ chế phân trang (Pagination) và làm mới dữ liệu kéo vuốt (Swipe-to-Refresh).
- **FR04.5**: Hiệu ứng chạm tương tác Material (ViewPressEffect) cho thẻ bài đăng.

### FR05 - Chi tiết sách & Thảo luận Cộng đồng (Book Detail & Comments)
- **FR05.1**: Xem ảnh bìa kích thước lớn, thông tin chi tiết, mô tả nội dung, hình thức chia sẻ, khoảng cách km và thông tin người đăng.
- **FR05.2**: Nút bấm gọi điện thoại (`Intent.ACTION_DIAL`) hoặc mở bản đồ dẫn đường trực tiếp.
- **FR05.3**: Thả tim yêu thích (Favorite Toggle) với hiệu ứng đồng bộ ngay lập tức vào Room SQLite và Firestore.
- **FR05.4**: Bình luận thảo luận công khai: Người dùng có thể để lại thắc mắc, hỏi tình trạng sách.
- **FR05.5**: Quyền xóa bình luận: Tác giả của bình luận hoặc Chủ sở hữu cuốn sách có quyền xóa bình luận; Firestore Security Rules kiểm soát chặt chẽ ở cấp độ tài liệu.

### FR06 - Quy trình Yêu cầu & Giao nhận Mã QR (Book Exchange & QR Verification)
- **FR06.1**: Người nhận gửi yêu cầu mượn/nhận/đổi sách kèm lời nhắn cho chủ sách.
- **FR06.2**: Chủ sách nhận thông báo, kiểm tra danh sách yêu cầu (tab "Yêu cầu nhận được") và lựa chọn Chấp nhận (Accept) hoặc Từ chối (Reject).
- **FR06.3**: Tạo mã QR Code giao nhận độc nhất cho mỗi giao dịch đã chấp nhận.
- **FR06.4**: Xác thực giao nhận: Một bên quét mã QR hoặc nhập mã bí mật để xác nhận hoàn thành giao dịch; trạng thái sách tự động chuyển sang "Đã trao đổi" (Exchanged).

### FR07 - Đánh giá & Chấm điểm uy tín (Reviews & Reputation Scoring)
- **FR07.1**: Sau khi giao dịch hoàn tất, hai bên có thể đánh giá lẫn nhau (chấm điểm từ 1 đến 5 sao kèm nhận xét).
- **FR07.2**: Hệ thống tự động tính toán điểm trung bình rating (`ratingAverage`, `ratingCount`, `reputationScore`) và cập nhật vào hồ sơ người dùng.

### FR08 - Yêu thích & Bộ nhớ đệm ngoại tuyến (Offline Room SQLite v9)
- **FR08.1**: Lưu trữ cục bộ toàn bộ danh sách sách yêu thích vào Room Database (`BookRoomDatabase` phiên bản 9).
- **FR08.2**: Khi mất kết nối Internet, người dùng vẫn có thể mở màn hình "Sách yêu thích" và duyệt các bài đăng đã lưu.
- **FR08.3**: Cơ chế đồng bộ hóa 2 chiều tự động và thủ công (Force Sync) giữa Room SQLite cục bộ và Cloud Firestore từ xa.

### FR09 - Thông báo thời gian thực & FCM (Push Notifications)
- **FR09.1**: Tích hợp Firebase Cloud Messaging (`MyFirebaseMessagingService`) gửi thông báo đẩy đến thiết bị khi có yêu cầu nhận sách mới, yêu cầu được chấp nhận, hoặc có bình luận mới.
- **FR09.2**: Lưu lịch sử thông báo vào collection `notifications`, hiển thị huy hiệu (Badge) số lượng chưa đọc.
- **FR09.3**: Chạm vào thông báo để chuyển hướng thông minh đến đúng màn hình chi tiết tương ứng.

### FR10 - Cài đặt ứng dụng & Quản lý bộ nhớ (App Settings & Cache Cleaning)
- **FR10.1**: Bật/Tắt tính năng nhận thông báo đẩy FCM với trạng thái lưu vào `SharedPreferences`.
- **FR10.2**: Nút "Đồng bộ dữ liệu Room": Cưỡng bức làm mới toàn bộ bộ nhớ đệm ngoại tuyến từ Firestore.
- **FR10.3**: Nút "Xóa bộ nhớ đệm (Cache)": Dọn dẹp an toàn các tệp hình ảnh tạm thời của Glide, giải phóng dung lượng bộ nhớ máy thực tế.

### FR11 - Trung tâm trợ giúp & Góp ý người dùng (Help Center & Feedbacks)
- **FR11.1**: Hộp thoại Trợ giúp tương tác trực quan (`HelpCenterDialog`) chứa danh mục FAQ dạng Collapsible Accordion (Hướng dẫn tìm sách, Đăng bài, Quy trình mượn sách, Bảo mật tài khoản).
- **FR11.2**: Liên hệ hỗ trợ trực tiếp qua Email Intent định dạng sẵn tiêu đề và thông tin hỗ trợ UEH BookShare.
- **FR11.3**: Hộp thoại Góp ý In-app: Cho phép sinh viên gửi phản hồi, báo cáo lỗi kèm thông tin thiết bị vào collection `feedbacks` trên Firestore để Admin xử lý.

### FR12 - Quản trị hệ thống & Kiểm duyệt (Admin Dashboard)
- **FR12.1**: Thống kê số lượng tổng quan: Tổng số sách, Sách có sẵn, Sách đã trao đổi, Tổng số người dùng, Tài khoản bị khóa.
- **FR12.2**: Quản lý người dùng: Tìm kiếm người dùng theo tên/email/nickname, Khóa tài khoản vi phạm, Mở khóa tài khoản, Cấp lại mật khẩu tạm thời.
- **FR12.3**: Kiểm duyệt sách: Xem danh sách bài đăng sách, kiểm tra nội dung và thực hiện xóa các bài đăng vi phạm chính sách.
- **FR12.4**: Giám sát sức khỏe kết nối cơ sở dữ liệu (Database Health Check): Đo lường độ trễ mạng (Ping Firestore latency) hiển thị trạng thái xanh/vàng/đỏ.

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
