# Mobile Application Development — BookShare (Demo)

Đồ án kết thúc học phần môn Phát triển ứng dụng Mobile (Mobile Application Development).

> Lưu ý: README này được soạn dựa trên cấu trúc hiện có của repository. Hãy cập nhật phần "Các thành viên" và "Tính năng" nếu cần thiết để phản ánh đúng nội dung dự án.

## Mô tả dự án
BookShare (tên tạm) là một ứng dụng Android cho phép người dùng chia sẻ, xem và quản lý sách trong cộng đồng. Ứng dụng được phát triển bằng Java (mã nguồn chính nằm trong `Project/app/src/main/java/vn/edu/ueh/rtmiue20/project`) và sử dụng hệ thống build Gradle (Gradle Kotlin DSL).

## Công nghệ sử dụng
- Ngôn ngữ: Java (đóng góp chính cho code)
- Build: Gradle (sử dụng Gradle wrapper trong `Project/gradlew`)
- Android: Android SDK (AndroidManifest.xml có trong `Project/app/src/main/`)
- Firebase: file `Project/app/google-services.json` có mặt — dự án sử dụng cấu hình dịch vụ Google (ví dụ Authentication / Firestore / Storage). Kiểm tra code để biết chi tiết dịch vụ sử dụng.
- Android Studio: đề xuất dùng Android Studio (IDE chính để mở và chạy dự án).

## Hiện có trong repository
Root:
- `.idea/` — cấu hình IDE (Android Studio / IntelliJ)
- `CODEOWNER/` — (thư mục dành cho file quy ước người phụ trách; kiểm tra nội dung)
- `Project/` — mã nguồn chính và cấu hình dự án Android
- `phân công công việc.md` — phân công nhiệm vụ (có thể chứa danh sách thành viên và nhiệm vụ)
- `plan-BookShare.md` — kế hoạch chi tiết dự án (đọc để biết yêu cầu, tính năng, nghiệp vụ)

Trong `Project/`:
- `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties` — cấu hình dự án
- `gradle/`, `gradlew`, `gradlew.bat` — Gradle wrapper và scripts
- `app/` — module ứng dụng Android:
  - `app/build.gradle.kts` — cấu hình module
  - `app/google-services.json` — cấu hình Firebase
  - `app/src/main/AndroidManifest.xml`
  - `app/src/main/java/vn/edu/ueh/rtmiue20/project/` — mã nguồn Java của app
  - `app/src/main/res/` — tài nguyên giao diện, hình ảnh, layout,...

## Cấu trúc thư mục (tóm tắt)
- Project/
  - app/
    - src/
      - main/
        - java/vn/edu/ueh/rtmiue20/project/  (mã nguồn chính)
        - res/                              (resources: layouts, drawables, ...)
        - AndroidManifest.xml
    - build.gradle.kts
    - google-services.json
  - build.gradle.kts
  - settings.gradle.kts
  - gradle.properties
  - gradle/
  - gradlew / gradlew.bat

## Hướng dẫn cài đặt (Development setup)
1. Yêu cầu môi trường
   - Java JDK 11 (hoặc JDK được Android Studio khuyến nghị)
   - Android Studio (phiên bản hỗ trợ Android Gradle Plugin tương ứng)
   - Android SDK & các SDK platforms / build-tools cần thiết (cài qua SDK Manager trong Android Studio)
   - Kết nối Internet để tải dependencies Gradle

2. Mở project
   - Mở Android Studio → "Open" → chọn thư mục `Project/` (thư mục chứa `settings.gradle.kts`).
   - Android Studio sẽ tự động đồng bộ Gradle. Nếu được yêu cầu, chọn sử dụng Gradle wrapper (recommended).

3. Cấu hình Firebase (nếu cần)
   - File `app/google-services.json` đã có sẵn — kiểm tra xem file này phù hợp với dự án Firebase hiện tại.
   - Nếu bạn muốn dùng tài khoản Firebase khác:
     - Tạo một project trên Firebase Console.
     - Thêm ứng dụng Android vào Firebase và tải `google-services.json` mới về.
     - Thay thế `Project/app/google-services.json` bằng file mới.
   - Đảm bảo plugin Google Services đã được khai báo trong `app/build.gradle.kts` nếu dự án cần.

4. Build & chạy
   - Từ Android Studio: chọn một AVD (emulator) hoặc thiết bị thật rồi nhấn Run.
   - Hoặc từ dòng lệnh (từ thư mục `Project/`):
     - Trên macOS / Linux:
       - ./gradlew assembleDebug
       - ./gradlew installDebug   (cài APK lên thiết bị đã kết nối)
     - Trên Windows:
       - gradlew.bat assembleDebug
       - gradlew.bat installDebug

## Hướng dẫn sử dụng (Người dùng)
- Chức năng cơ bản (dự kiến, kiểm tra chi tiết trong code):
  - Đăng ký / đăng nhập người dùng (có thể dùng Firebase Auth nếu được cấu hình)
  - Xem danh sách sách có sẵn
  - Thêm sách mới (có thể kèm ảnh, mô tả)
  - Mượn / trả / theo dõi trạng thái sách
  - Quản lý hồ sơ người dùng
- Cách dùng:
  - Mở ứng dụng trên thiết bị → đăng ký/đăng nhập → điều hướng qua các màn hình danh sách, thêm sách, hồ sơ.
  - Nếu ứng dụng yêu cầu quyền (camera, storage), cấp quyền khi được yêu cầu.

## Chạy test
- Kiểm tra các tests đơn vị trong `app/src/test/` và instrumentation tests trong `app/src/androidTest/` (nếu có).
- Sử dụng Android Studio hoặc Gradle:
  - ./gradlew test               (chạy unit tests)
  - ./gradlew connectedAndroidTest  (chạy instrumentation tests trên thiết bị)

## Hướng dẫn đóng gói (Release)
- Thiết lập signingConfigs trong `app/build.gradle.kts` (keystore, alias, password).
- Chạy:
  - ./gradlew assembleRelease
- Kiểm tra file APK / AAB sinh ra tại `Project/app/build/outputs/`.

## Gợi ý bảo mật & cấu hình
- Không commit các thông tin nhạy cảm (keystore passwords, API keys) vào Git.
- Hiện có `google-services.json` trong repo — nếu đây là file chứa thông tin test, cân nhắc chuyển sang biến môi trường hoặc secret manager cho môi trường production.
- Kiểm tra `.gitignore` trong `Project/` để loại trừ file build và tệp cá nhân.

## Các thành viên
- Chủ repository / tác giả trên GitHub: rtmiue20
- (Nếu dự án có nhiều thành viên, cập nhật phần này từ `phân công công việc.md`)

## Tài liệu & Kế hoạch
- Xem `plan-BookShare.md` để biết mô tả tính năng, giao diện, và phân công công việc chi tiết.
- Xem `phân công công việc.md` để nắm ai chịu trách nhiệm phần nào.

## Đóng góp
- Fork repo → tạo branch mới → thực hiện thay đổi → tạo pull request.
- Mô tả rõ thay đổi, ticket liên quan, và cách chạy thử.

## Liên hệ
- Chủ repository: https://github.com/rtmiue20
- Email / thông tin liên hệ: (cập nhật nếu muốn công khai)

## Lịch sử thay đổi
- README ban đầu: mô tả dự án, hướng dẫn cài đặt, cấu trúc thư mục và các hướng dẫn cần thiết để chạy & phát triển ứng dụng.

---

Nếu bạn muốn, tôi có thể:
- Tạo trực tiếp file README.md này trong repository (bạn cần xác nhận),
- Hoặc mở rộng phần Tính năng & Thành viên bằng cách đọc nội dung chi tiết của `plan-BookShare.md` và `phân công công việc.md` để tự động điền thông tin chính xác.
