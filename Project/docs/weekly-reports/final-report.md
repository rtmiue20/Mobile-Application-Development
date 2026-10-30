# Báo cáo Tổng kết Đồ án - BookShare
**Ứng dụng Di động Chia sẻ và Trao đổi Sách - UEH BookShare**
**Môn học: Phát triển Ứng dụng Di động (Mobile Application Development)**

---

## 1. Tổng quan Dự án (Executive Summary)

**BookShare** là sản phẩm ứng dụng di động Android hoàn chỉnh được xây dựng nhằm tạo ra môi trường kết nối, chia sẻ giáo trình và sách cũ văn minh, tiết kiệm cho sinh viên Đại học Kinh tế TP. Hồ Chí Minh (UEH). Ứng dụng giải quyết triệt để bài toán lãng phí tài nguyên sách sau mỗi kỳ học, đồng thời giúp các bạn sinh viên dễ dàng tiếp cận nguồn tri thức phong phú với chi phí tối thiểu.

Dự án được xây dựng và hoàn thiện 100% theo các tiêu chuẩn kỹ thuật phát triển ứng dụng di động tiên tiến nhất trên nền tảng Android SDK 37, tuân thủ nguyên tắc thiết kế **Google Material 3**, mô hình kiến trúc **MVVM kết hợp Repository Pattern**, và giải pháp dữ liệu kép **Offline-First (Room SQLite v9 & Cloud Firestore)**.

---

## 2. Bảng Tổng hợp Kết quả Kỹ thuật Đạt được

| Phân hệ Kỹ thuật | Công nghệ & Giải pháp Triển khai | Kết quả Đạt được |
| :--- | :--- | :--- |
| **Kiến trúc Ứng dụng** | MVVM + Repository Pattern | Phân tách rành mạch giữa UI, ViewModel và tầng dữ liệu; mã nguồn module hóa, dễ dàng bảo trì và mở rộng. |
| **Xác thực Người dùng (Auth)** | Firebase Authentication & Google Credential Manager API | Đăng nhập Email/Password, Nickname tự động, Đăng nhập Google một chạm mượt mà, hỗ trợ Chế độ xem cho Khách (Guest Mode) và cơ chế Đổi mật khẩu bắt buộc. |
| **Cơ sở Dữ liệu Đám mây** | Cloud Firestore NoSQL Database (8 Collections) | Đồng bộ dữ liệu thời gian thực cho: `users`, `books`, `requests`, `comments`, `favorites`, `notifications`, `reviews`, `feedbacks`. |
| **Bảo mật & Phân quyền** | Firestore Security Rules v2 | Kiểm soát phân quyền đa tầng, chặn tài khoản bị khóa (`status: 'locked'`), ngăn chặn leo quyền Admin, bảo vệ quyền xóa bài đăng và bình luận. |
| **Bộ nhớ Cục bộ (Offline-First)** | Android Room SQLite Database (Version 9) | Lưu trữ đệm ngoại tuyến cho `Book`, `Favorite`, `User`; tự động tính khoảng cách GPS và cho phép xem sách yêu thích ngay cả khi không có mạng. |
| **Giao tiếp & Thông báo** | Firebase Cloud Messaging (FCM) | Gửi thông báo đẩy thời gian thực khi có yêu cầu mượn sách, chấp nhận yêu cầu hoặc có bình luận mới. |
| **Quy trình Trao đổi Sách** | ZXing QR Code Engine | Tự động sinh mã QR Code giao nhận độc nhất, xác thực giao dịch trực tiếp giữa sinh viên tại khuôn viên trường. |
| **Đánh giá Uy tín (Reviews)** | Hệ thống Rating & Reputation Scoring | Đánh giá 1-5 sao và nhận xét sau giao dịch, tính toán cập nhật điểm uy tín người dùng. |
| **Giao diện & Trải nghiệm (UI/UX)** | Material 3 & Responsive Landscape | Giao diện hiện đại, bảng màu HSL hài hòa, hiệu ứng chạm (ViewPressEffect), hỗ trợ xoay ngang chuyên dụng 2 cột (`layout-land`). |
| **Cài đặt & Tiện ích Hệ thống** | Glide Cache Cleaning & Room Sync Engine | Hộp thoại cài đặt cho phép quản lý FCM, ép đồng bộ dữ liệu Room và dọn dẹp cache thực tế. |
| **Hỗ trợ & Góp ý Sinh viên** | Collapsible FAQ Accordion & In-app Feedback | Trung tâm trợ giúp tương tác trực quan, liên hệ email hỗ trợ và gửi báo cáo lỗi lưu vào Firestore. |
| **Quản trị Hệ thống (Admin)** | Real-time Admin Dashboard & Health Check | Thống kê số lượng theo thời gian thực, quản lý và khóa tài khoản vi phạm, cấp mật khẩu tạm, kiểm tra độ trễ mạng cơ sở dữ liệu. |

---

## 3. Bảng Đánh giá Tiến độ 15 Module Chức năng

| STT | Tên Hạng mục / Module Chức năng | Thành viên Đảm nhiệm | Trạng thái |
| :---: | :--- | :--- | :---: |
| 1 | Khởi tạo Dự án, Kiến trúc MVVM & Quản trị repo | Nguyễn Văn Minh Triều | **100% Hoàn thành** |
| 2 | Module Xác thực (Auth: Đăng nhập, Đăng ký, Google Sign-In) | Triệu Nguyễn Huỳnh Khang | **100% Hoàn thành** |
| 3 | Màn hình Quên mật khẩu, Hồ sơ cá nhân (Edit Profile) & Remember Me | Triệu Nguyễn Huỳnh Khang | **100% Hoàn thành** |
| 4 | Trang chủ, Tìm kiếm & Lọc danh mục trượt ngang | Ngô Trọng Phúc | **100% Hoàn thành** |
| 5 | Màn hình Đăng bài (`CreatePostActivity`) & Chi tiết sách (`BookDetailActivity`) | Ngô Trọng Phúc | **100% Hoàn thành** |
| 6 | Chế độ xem cho Khách (Guest mode) & Hệ thống Chuông thông báo | Nguyễn Văn Minh Triều | **100% Hoàn thành** |
| 7 | Tính năng Bình luận cộng đồng (Comments) & Sách yêu thích (Favorites) | Nguyễn Phúc Vượng | **100% Hoàn thành** |
| 8 | Quản lý yêu cầu nhận sách, Hộp thoại xác nhận & Đánh giá Rating | Nguyễn Phúc Vượng | **100% Hoàn thành** |
| 9 | Thông báo FCM Push & Tính khoảng cách động GPS Haversine | Nguyễn Phúc Vượng | **100% Hoàn thành** |
| 10 | Giao diện Responsive Xoay ngang (`layout-land`) & Dialog Admin Reset Pass | Nguyễn Phúc Vượng | **100% Hoàn thành** |
| 11 | Bảng điều khiển Quản trị viên (`AdminDashboardActivity`) & Quản lý User | Ngô Trọng Phúc | **100% Hoàn thành** |
| 12 | Thiết lập & Bảo mật Cloud Firestore Security Rules v2 (8 Collections) | Ngô Trọng Phúc | **100% Hoàn thành** |
| 13 | Cài đặt ứng dụng (FCM Switch, Ép đồng bộ, Dọn Cache) & Trung tâm FAQ | Ngô Trọng Phúc | **100% Hoàn thành** |
| 14 | Tối ưu cơ sở dữ liệu ngoại tuyến Room SQLite v9 & Đồng bộ Firestore | Nguyễn Phúc Vượng, Ngô Trọng Phúc | **100% Hoàn thành** |
| 15 | Kiểm duyệt bài đăng sách (Admin Posts), Review PR & Báo cáo đồ án | Nguyễn Văn Minh Triều, Ngô Trọng Phúc | **100% Hoàn thành** |

---

## 4. Hệ thống Tài liệu Kỹ thuật Đầy đủ của Dự án

Toàn bộ tài liệu phân tích, thiết kế, kiến trúc và báo cáo đã được cập nhật đồng bộ hoàn toàn với mã nguồn thực tế:
- [README.md](file:///d:/AndroidStudioProject/Project/README.md): Bản giới thiệu tổng thể dự án, hướng dẫn cài đặt và cấu trúc mã nguồn.
- [phân công công việc.md](file:///d:/AndroidStudioProject/Project/ph%C3%A2n%20c%C3%B4ng%20c%C3%B4ng%20vi%E1%BB%87c.md): Bản phân công chi tiết 15 đầu việc kỹ thuật cho 4 thành viên.
- [srs.md](file:///d:/AndroidStudioProject/Project/docs/srs.md): Đặc tả yêu cầu phần mềm đầy đủ 13 yêu cầu chức năng (FR01 - FR13) và 5 yêu cầu phi chức năng.
- [erd-and-architecture.md](file:///d:/AndroidStudioProject/Project/docs/erd/erd-and-architecture.md): Sơ đồ kiến trúc MVVM, sơ đồ ERD Mermaid và đặc tả 8 Collections Firestore cùng cấu trúc Room SQLite v9.
- [wireframes-overview.md](file:///d:/AndroidStudioProject/Project/docs/wireframes/wireframes-overview.md): Bản đồ luồng màn hình chi tiết của toàn bộ 13 Activities và các Dialog tương tác.
- [teamwork-evidence.md](file:///d:/AndroidStudioProject/Project/docs/report-evidence/teamwork-evidence.md): Minh chứng làm việc nhóm, quy trình Git/Firebase và nhật ký tối ưu tài nguyên.
- [Báo cáo các tuần](file:///d:/AndroidStudioProject/Project/docs/weekly-reports/): Gồm đầy đủ báo cáo Tuần 1, Tuần 2, Tuần 3, Tuần 4 và Báo cáo Tổng kết.

---

## 5. Kết luận & Hướng Phát triển Tương lai

Đồ án **UEH BookShare** đã xuất sắc đạt được toàn bộ các mục tiêu đặt ra từ đầu kỳ học. Ứng dụng không chỉ có giao diện bắt mắt, logic xử lý mượt mà mà còn đáp ứng trọn vẹn các yêu cầu khắt khe về mặt kiến trúc phần mềm, bảo mật dữ liệu và vận hành thực tế. 

Trong tương lai, nhóm dự kiến mở rộng thêm các tính năng:
1. Tích hợp AI gợi ý sách theo ngành học và sở thích cá nhân.
2. Tích hợp bản đồ trực quan tương tác (Interactive Map View) vị trí các cuốn sách tại các cơ sở UEH.
3. Mở rộng cộng đồng liên trường cho toàn bộ các trường đại học thuộc khối Đại học Quốc gia và TP.HCM.
