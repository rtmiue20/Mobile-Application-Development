# Sơ đồ kiến trúc & ERD - BookShare

## 1. Kiến trúc hệ thống (System Architecture)
BookShare áp dụng mô hình kiến trúc **MVVM (Model - View - ViewModel)** kết hợp với **Repository Pattern**:
- **UI Layer (`ui/`)**: Activities, Fragments, Adapters, ViewModels hiển thị dữ liệu và nhận tương tác người dùng.
- **Data Layer (`data/`)**: 
  - **Repositories (`repository/`)**: `AuthRepository`, `BookRepository`, `UserRepository`, `RequestRepository` quản lý nguồn dữ liệu từ Firebase Auth và Cloud Firestore.
  - **Models (`model/`)**: `User`, `Book`, `Request`, `Comment` định nghĩa cấu trúc thực thể.
- **Backend Service**: Sử dụng **Firebase Authentication** (Xác thực người dùng) và **Cloud Firestore** (NoSQL Database).

## 2. Sơ đồ thực thể liên kết (ERD - Entity Relationship Diagram)

### Các Thực thể chính trong Firestore:
1. **Users (`users/{uid}`)**
   - `uid` (String, Primary Key)
   - `fullName` (String)
   - `nickname` (String)
   - `email` (String)
   - `avatarUrl` (String)
   - `phoneNumber` (String)
   - `role` (String: "user" | "admin")
   - `status` (String: "active" | "locked")
   - `city`, `district`, `ward` (String)
   - `latitude`, `longitude` (Double)
   - `mustChangePassword` (Boolean)
   - `createdAt` (Long)

2. **Books (`books/{bookId}`)**
   - `bookId` (String, Primary Key)
   - `title` (String)
   - `author` (String)
   - `category` (String)
   - `condition` (String)
   - `sharingType` (String: "free" | "exchange" | "fee")
   - `price` (Double)
   - `imageUrl` (String)
   - `userId` (String, Foreign Key -> Users)
   - `userName`, `userAvatar` (String)
   - `city`, `district`, `ward` (String)
   - `latitude`, `longitude` (Double)
   - `status` (String: "available" | "requested" | "exchanged")
   - `createdAt` (Long)

3. **Requests (`requests/{requestId}`)**
   - `requestId` (String, Primary Key)
   - `bookId` (String, Foreign Key -> Books)
   - `bookTitle`, `bookImageUrl` (String)
   - `senderId` (String, Foreign Key -> Users)
   - `senderName`, `senderAvatar` (String)
   - `receiverId` (String, Foreign Key -> Users)
   - `status` (String: "pending" | "accepted" | "rejected")
   - `message` (String)
   - `createdAt` (Long)

4. **Comments (`comments/{commentId}`)**
   - `commentId` (String, Primary Key)
   - `bookId` (String, Foreign Key -> Books)
   - `userId` (String, Foreign Key -> Users)
   - `userName`, `userAvatar` (String)
   - `content` (String)
   - `createdAt` (Long)
