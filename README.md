# GiaSuHQ Backend (`tutors_BE`)

Hệ thống Backend API cho dự án GiaSuHQ (Quản lý dạy kèm & AI Note).

## 🚀 Công nghệ sử dụng
- **Ngôn ngữ:** Java 17
- **Framework:** Spring Boot 3.3.5 (Web, JPA, Validation)
- **Cơ sở dữ liệu:** MySQL
- **Build Tool:** Maven

---

## 🛠️ Hướng dẫn cài đặt & Chạy trên máy Local (Dành cho thành viên nhóm)

### 1. Yêu cầu môi trường
- JDK 17 trở lên
- Maven 3.8+ (hoặc dùng IntelliJ IDEA)
- Cơ sở dữ liệu MySQL (local hoặc remote server)

### 2. Cấu hình biến môi trường
Tạo các biến môi trường hoặc chạy với cấu hình local. Bạn có thể xem mẫu tại `src/main/resources/application.properties.example`.

Các biến môi trường cần thiết:
```env
DB_URL=jdbc:mysql://localhost:3306/giasuhq?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=root
DB_PASSWORD=<MẬT_KHẨU_MYSQL>
CORS_ORIGINS=http://localhost:5173
GEMINI_API_KEY=<GEMINI_API_KEY>
GEMINI_MODEL=gemini-3.6-flash
```

### 3. Chạy ứng dụng
Mở terminal tại thư mục `tutors_BE`:
```bash
# Compile dự án
mvn clean compile

# Khởi chạy ứng dụng
mvn spring-boot:run
```
Ứng dụng backend sẽ khởi chạy tại: `http://localhost:8080`

## AI Note, báo cáo tiến độ và voucher

- `POST /api/lessons/{id}/ai-note-suggest`: tạo AI Note từ ghi chú thô của gia sư.
- `POST /api/progress/report` hoặc `GET /api/progress/report?classId=...`: tổng hợp buổi học, bài tập và tạo báo cáo tiến độ bằng Gemini.
- `GET /api/vouchers`: xem voucher của tài khoản.
- `POST /api/vouchers/claim`: nhận voucher theo mã hoặc mã chia sẻ.
- `POST /api/vouchers/{userVoucherId}/share`: tạo link chia sẻ voucher dùng một lần.
- `POST /api/vouchers/quote`: kiểm tra mức giảm trước khi thanh toán.
- `POST /api/classes/{id}/pay-connection-fee`: thanh toán phí kết nối, áp dụng voucher và tự phát voucher cảm ơn sau giao dịch thành công.

API key chỉ được đọc ở backend qua `GEMINI_API_KEY` hoặc file `application-local.properties` bị git-ignore; không đưa key vào frontend.

---

## 🌿 Quy định Git & Làm việc nhóm 2 người
- **Branch chính:** `main` (chỉ chứa code đã kiểm thử hoạt động ổn định).
- **Tạo branch mới cho mỗi tính năng:** `feature/ten-tinh-nang` (ví dụ: `feature/auth`, `feature/ai-note`).
- **Tạo Pull Request (PR):** Khi hoàn thành tính năng, tạo PR vào `main` và báo người còn lại review trước khi merge.
- 🛑 **LƯU Ý QUAN TRỌNG:** Không commit mật khẩu Database hoặc file cấu hình chứa key cá nhân lên GitHub!
