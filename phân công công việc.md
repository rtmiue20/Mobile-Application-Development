# Phân công chi tiết theo tuần: BookShare — Chia sẻ sách báo cũ

> Nhóm 4 người: **Leader**, **Thành viên 2**, **Thành viên 3**, **Thành viên 4** (thay bằng tên thật khi dùng).
> 4 tuần, 03/10/2026 → 31/10/2026. Mã FR tham chiếu file `plan.md`.

## Bảng tóm tắt vai trò

| Người | Mảng phụ trách | Chức năng (mã FR) |
|---|---|---|
| **Leader** | BA, kiến trúc, dữ liệu, deploy, review | FR14 + khung `data/`, Security Rules, admin layout |
| **Khang** | Người dùng, hồ sơ, bảo mật tài khoản | FR01, FR02, FR03, FR15 |
| **Phúc** | Sách và tìm kiếm (lõi app) | FR04, FR05, FR06, FR07 |
| **Vương** | Tương tác, offline, thông báo | FR08, FR09, FR10, FR11, FR12, FR13 |

## Tổng quan theo tuần (ai làm gì)

| Tuần | Leader | Khang | Phúc | Vương |
|---|---|---|---|---|
| 1 | BA, ERD, repo, board, khung MVVM, kết nối Firebase | Cài môi trường, FR01 đăng ký/đăng nhập Email, FR02 Google Sign-In | Cài môi trường, view tĩnh Trang chủ/Chi tiết/Đăng bài | Cài môi trường, view tĩnh Yêu cầu/Thông báo, nghiên cứu Room + FCM |
| 2 | Repository chung, Security Rules bản đầu, hỗ trợ tích hợp | FR03 hồ sơ cá nhân | FR04 Trang chủ, FR05 Tìm kiếm, FR06 Đăng/sửa/xóa bài, FR07 Chi tiết | FR08 Bình luận, FR09 Yêu thích |
| 3 | FR14 Admin duyệt/ẩn/xóa bài, Security Rules chính thức | FR15 Admin quản lý người dùng | Responsive (`layout-land`) cho các màn chính | FR10 Yêu cầu nhận sách, FR12 Room/offline, FR11 FCM |
| 4 | Tổng hợp báo cáo, chuẩn bị build, chụp minh chứng GitHub | Test chéo + viết báo cáo phần mình | Test chéo + viết báo cáo phần mình | Test chéo + viết báo cáo phần mình |

---

## Tuần 1 (03/10 – 09/10): BA và nền tảng

**Leader**
- [ ] Viết bảng yêu cầu (FR), use case; chốt quy tắc nghiệp vụ (trạng thái bài đăng, có duyệt trước hay không).
- [ ] Hoàn thiện wireframe từ Google Stitch → Figma; vẽ sơ đồ cấu trúc Firestore + sơ đồ bảng Room.
- [ ] Tạo cấu trúc thư mục, `.gitignore`, `README`, `google-services.json.example`.
- [ ] GitHub: bảo vệ `main`, tạo `develop`, Issue template, nhãn, milestone theo tuần 1 đến 4; tạo toàn bộ Issue theo mã FR trong GitHub Project, gán người.
- [ ] Khởi tạo Firebase project (Auth, Firestore, Storage, FCM); dựng khung MVVM/Repository dùng chung.

**Thành viên 2** (nhánh `feature/FR01-auth`, `feature/FR02-google-signin`)
- [ ] Cài Android Studio, clone repo, thử tạo nhánh và PR nhỏ.
- [ ] FR01: Đăng ký, đăng nhập, đăng xuất bằng Email (Firebase Auth).
- [ ] FR02: Google Sign-In — cấu hình SHA-1 trên Firebase Console, test trên thiết bị thật.

**Thành viên 3** (nhánh `feature/FR04-views`)
- [ ] Cài môi trường, clone repo, thử PR nhỏ.
- [ ] Dựng view tĩnh: Trang chủ, Chi tiết sách, Đăng bài (theo Figma).

**Thành viên 4** (nhánh `feature/FR10-views`)
- [ ] Cài môi trường, clone repo, thử PR nhỏ.
- [ ] Dựng view tĩnh: Yêu cầu nhận sách, Thông báo.
- [ ] Nghiên cứu trước: Room (entity/DAO cơ bản), Firebase Cloud Messaging.

**Bàn giao:** SRS, ERD, wireframe, backlog đầy đủ trên GitHub Project, mọi người chạy được project trống, đăng nhập Email + Google hoạt động.
**Minh chứng:** ảnh repo, board, Firebase Console, mỗi người có ít nhất một PR đã merge.

---

## Tuần 2 (10/10 – 16/10): Chức năng lõi

**Leader** (nhánh `feature/repository-core`, `feature/security-rules-draft`)
- [ ] Hoàn thiện lớp `Repository` dùng chung (nguồn dữ liệu duy nhất cho UI).
- [ ] Firestore Security Rules bản đầu (chỉ chủ dữ liệu được sửa/xóa).
- [ ] Hỗ trợ các thành viên tích hợp Firestore vào ViewModel.

**Thành viên 2** (nhánh `feature/FR03-profile`)
- [ ] `ProfileController`/ViewModel: xem, sửa hồ sơ, đổi avatar (upload Storage).
- [ ] Validate input (tên, số điện thoại nếu có).

**Thành viên 3** (nhánh `feature/FR04-FR07-core`)
- [ ] FR04: Trang chủ — danh sách sách theo danh mục, dạng lưới.
- [ ] FR05: Tìm kiếm + bộ lọc (danh mục, tình trạng, sắp xếp).
- [ ] FR06: Đăng bài mới (chọn ảnh → nhập thông tin), sửa/xóa bài của mình.
- [ ] FR07: Chi tiết sách.

**Thành viên 4** (nhánh `feature/FR08-FR09-interact`)
- [ ] FR08: Bình luận trên bài đăng.
- [ ] FR09: Yêu thích (lưu/bỏ lưu), đảm bảo mỗi người chỉ lưu một lần.

**Bàn giao:** luồng chính hoạt động online — đăng ký → đăng bài → người khác tìm thấy, yêu thích, bình luận.

---

## Tuần 3 (17/10 – 23/10): Offline, Responsive, Admin

**Leader** (nhánh `feature/FR14-admin-posts`)
- [ ] Khung màn Admin (kiểm tra quyền `role == admin`).
- [ ] FR14: danh sách bài cần xử lý, nút Duyệt/Ẩn/Xóa.
- [ ] Hoàn thiện Security Rules chính thức (review lại toàn bộ collection).

**Thành viên 2** (nhánh `feature/FR15-admin-users`)
- [ ] FR15: danh sách người dùng, khóa/mở khóa (người bị khóa không đăng nhập được).

**Thành viên 3** (nhánh `feature/responsive-core-screens`)
- [ ] Responsive cho Trang chủ, Chi tiết sách, Đăng bài: giữ state qua ViewModel khi xoay, tạo `layout-land/`.

**Thành viên 4** (nhánh `feature/FR10-requests`, `feature/FR12-offline`, `feature/FR11-fcm`)
- [ ] FR10: Yêu cầu nhận sách (gửi, chủ bài đồng ý/từ chối).
- [ ] FR12: Room — entity + DAO cho Book/User/Favorite; đồng bộ Firestore → Room → UI; banner "Đang xem ngoại tuyến".
- [ ] FR11: Thông báo FCM khi có yêu cầu/bình luận mới.
- [ ] Hỗ trợ Thành viên 3 làm responsive cho các màn còn lại (Yêu thích, Yêu cầu, Hồ sơ).

**Bàn giao:** đáp ứng đủ yêu cầu kỹ thuật bắt buộc (Firebase + offline SQLite + responsive + Google Sign-In), có admin.

---

## Tuần 4 (24/10 – 31/10): Kiểm thử, Báo cáo, Trình bày

**Leader**
- [ ] Review toàn bộ PR còn lại, gỡ xung đột, kiểm thử tích hợp toàn hệ thống.
- [ ] Build APK bản ổn định, chạy checklist trước demo (mục 11, file `plan.md`).
- [ ] Chụp minh chứng GitHub (Network graph, danh sách PR đã merge), tổng hợp báo cáo (các mục chung: giới thiệu, phân tích, thiết kế, quản lý nhóm).

**Thành viên 2**: test chéo chức năng của Thành viên 3 (FR04–FR07), ghi bug vào Issue; viết phần báo cáo "FR01, FR02, FR03, FR15".

**Thành viên 3**: test chéo chức năng của Thành viên 4 (FR08–FR12), ghi bug vào Issue; viết phần báo cáo "FR04–FR07, responsive".

**Thành viên 4**: test chéo chức năng của Thành viên 2 (FR01–FR03, FR15), ghi bug vào Issue; viết phần báo cáo "FR08–FR12, FCM".

**Cả nhóm**
- [ ] Test trên ít nhất 2 thiết bị/emulator, cả xoay ngang và chế độ máy bay.
- [ ] Luyện kịch bản demo 5 phút, quay video demo dự phòng.
- [ ] Nộp bài trước hạn ít nhất 1 ngày.

**Bàn giao (31/10):** code trên GitHub, báo cáo PDF hoàn chỉnh, app chạy ổn định, sẵn sàng trình bày.

---

## Kịch bản demo 5 phút (để cả nhóm luyện theo)

1. **0:00–0:30** — Giới thiệu bài toán và ý tưởng BookShare.
2. **0:30–1:30** — Kiến trúc và kỹ thuật trọng tâm: MVVM/Repository, Firebase, Google Sign-In, Room offline, responsive.
3. **1:30–4:00** — Demo liền mạch: đăng nhập Google → xem sách → đăng bài → bật chế độ máy bay, vẫn xem lại được trang đã mở → xoay ngang → admin xóa bài.
4. **4:00–5:00** — Phân công, mức hoàn thành, kết luận.

---

## Quy ước chung cho cả 4 tuần

- Họp đầu tuần (phân task), họp giữa tuần 15 phút (gỡ vướng), cuối tuần demo ngắn và ghi biên bản.
- Mỗi task là một Issue gắn mã FR, một nhánh `feature/...`, một Pull Request có ít nhất một review; Leader là người merge vào `develop`.
- Mỗi người chỉ sửa file thuộc module mình; cần đổi file chung (`data/model`, Room database class, layout chung) thì tạo Issue cho Leader.
- Cuối mỗi tuần mỗi người chụp ảnh minh chứng công việc của mình vào `docs/report-evidence/tuần-N/`.

## Bảng theo dõi mức độ hoàn thành (cập nhật hàng tuần, đưa vào báo cáo)

| Thành viên | Module | Tuần 1 | Tuần 2 | Tuần 3 | Tuần 4 | Tổng % hoàn thành |
|---|---|---|---|---|---|---|
| Leader | Kiến trúc, dữ liệu, admin, deploy | | | | | |
| Thành viên 2 | Người dùng & hồ sơ | | | | | |
| Thành viên 3 | Sách & tìm kiếm | | | | | |
| Thành viên 4 | Tương tác, offline, thông báo | | | | | |

## Nếu một người bị trễ

| Tình huống | Xử lý |
|---|---|
| Thành viên 4 trễ FR12 (Room/offline) | Leader hỗ trợ ngay đầu tuần 3 vì đây là yêu cầu bắt buộc, không thể cắt |
| Thành viên 2 trễ FR15 (admin khóa user) | Cắt FR15 trước, giữ FR01/FR02/FR03 |
| Thành viên 3 trễ FR05 (tìm kiếm nâng cao) | Giữ tìm kiếm cơ bản theo tên sách, bỏ bộ lọc nâng cao |
| Leader quá tải | Chuyển việc kiểm thử tích hợp cho thành viên xong sớm nhất trong tuần 4 |
