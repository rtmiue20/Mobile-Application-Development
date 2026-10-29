# Báo cáo Tuần 2 - Hệ thống Xác thực & Phân quyền

## 1. Mục tiêu tuần 2
- Hoàn thành module xác thực người dùng (Auth).
- Tích hợp Google Sign-In với Credential Manager API mới nhất.
- Xây dựng tính năng phân quyền User / Admin và đổi mật khẩu bắt buộc.

## 2. Công việc đã thực hiện
- Phát triển `LogInActivity` và `RegisterActivity` với giao diện Material 3.
- Cập nhật Google Sign-In sử dụng `CredentialManager` và `GetSignInWithGoogleOption` để khắc phục các API bị deprecated.
- Xây dựng `AuthRepository` xử lý đăng nhập Email/Password và Google Auth, đồng thời hỗ trợ tra cứu tự động khi người dùng nhập Username/Nickname.
- Phát triển tính năng Admin quản lý tài khoản và buộc đổi mật khẩu khi admin cấp lại mật khẩu.

## 3. Đánh giá kết quả
- Hoàn thành toàn bộ hệ thống xác thực an toàn và hiện đại.
