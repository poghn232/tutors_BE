-- =========================================================
-- GiaSuHQ MVP Initial Seed Data (Data.sql - 8 Tables)
-- =========================================================

-- 1. Insert Base Users (Mật khẩu chuẩn BCrypt của '123456')
INSERT IGNORE INTO users (id, email, password, full_name, phone, role, is_vip, balance, email_verified) VALUES
(1, 'tutor.nguyen@giasuhq.com', '$2a$10$10Q2J.X5iX/KOM4nHtFMfeXi4JoW3O6sv4ZtaJ6Ab2P0FNC71XcpO', 'Hoàng Thiên Ứng', '0901234567', 'TUTOR', true, 0, true),
(2, 'parent.tran@giasuhq.com', '$2a$10$10Q2J.X5iX/KOM4nHtFMfeXi4JoW3O6sv4ZtaJ6Ab2P0FNC71XcpO', 'Trần Thị B', '0907654321', 'PARENT', true, 500000, true),
(3, 'admin@giasuhq.com', '$2a$10$10Q2J.X5iX/KOM4nHtFMfeXi4JoW3O6sv4ZtaJ6Ab2P0FNC71XcpO', 'Quản Trị Viên Hệ Thống', '0909999999', 'ADMIN', true, 0, true),
(4, 'tutor.lan@giasuhq.com', '$2a$10$10Q2J.X5iX/KOM4nHtFMfeXi4JoW3O6sv4ZtaJ6Ab2P0FNC71XcpO', 'TS. Phạm Thị Lan', '0902222333', 'TUTOR', true, 0, true),
(5, 'tutor.thu@giasuhq.com', '$2a$10$10Q2J.X5iX/KOM4nHtFMfeXi4JoW3O6sv4ZtaJ6Ab2P0FNC71XcpO', 'TS. Lê Thị Thu', '0903333444', 'TUTOR', true, 0, true),
(6, 'tutor.duc@giasuhq.com', '$2a$10$10Q2J.X5iX/KOM4nHtFMfeXi4JoW3O6sv4ZtaJ6Ab2P0FNC71XcpO', 'Trần Minh Đức', '0904444555', 'TUTOR', false, 0, true),
(7, 'tutor.mai@giasuhq.com', '$2a$10$10Q2J.X5iX/KOM4nHtFMfeXi4JoW3O6sv4ZtaJ6Ab2P0FNC71XcpO', 'Vũ Thị Mai', '0905555666', 'TUTOR', false, 0, true),
(8, 'tutor.nam@giasuhq.com', '$2a$10$10Q2J.X5iX/KOM4nHtFMfeXi4JoW3O6sv4ZtaJ6Ab2P0FNC71XcpO', 'Đặng Hoàng Nam', '0906666777', 'TUTOR', true, 0, true);

-- 2. Insert Inherited Role Data for Tutors
INSERT IGNORE INTO tutors (user_id, bio, qualification, experience_years, facebook_url, verification_status, certificates_json) VALUES
(1, 'Tiến sĩ Toán học ứng dụng tại ĐH Quốc gia Hà Nội. Tôi giúp học sinh hiểu toán học qua các ứng dụng thực tế. 8+ năm kinh nghiệm từ THCS đến đại học.', 'Tiến sĩ Toán học', 8, 'https://facebook.com/giasu.hoangthienung', 'APPROVED', '[{"id":101,"title":"Bằng Tiến Sĩ Toán Học - ĐHQGHN","imageUrl":"https://images.unsplash.com/photo-1523240795612-9a054b0db644?w=600&auto=format&fit=crop","date":"2023"}]'),
(4, 'Tiến sĩ Y khoa tại ĐH Y Hà Nội. Chuyên luyện thi y dược và khoa học tự nhiên. 96% học sinh đậu kỳ thi quốc gia.', 'Tiến sĩ Y khoa · ĐH Y Hà Nội', 10, 'https://facebook.com/giasu.phamthilan', 'APPROVED', '[{"id":102,"title":"Bằng Bác Sĩ Chuyên Khoa - ĐH Y Hà Nội","imageUrl":"https://images.unsplash.com/photo-1576091160399-112ba8d25d1d?w=600&auto=format&fit=crop","date":"2022"}]'),
(5, 'Cựu giảng viên đại học với niềm đam mê làm cho khoa học trở nên thú vị. Sử dụng thí nghiệm thực hành và ví dụ thực tế.', 'Tiến sĩ · ĐH Stanford (Hoa Kỳ)', 12, 'https://facebook.com/giasu.lethithu', 'APPROVED', '[{"id":103,"title":"Chứng nhận Giảng viên Xuất sắc Stanford","imageUrl":"https://images.unsplash.com/photo-1517486808906-6ca8b3f04846?w=600&auto=format&fit=crop","date":"2021"}]'),
(6, 'Thạc sĩ Giáo dục tại ĐH Ngoại Thương. Cựu giáo viên THPT, chuyên gia luyện thi đại học với tỉ lệ học sinh đậu 95%.', 'Thạc sĩ · ĐH Ngoại Thương', 6, 'https://facebook.com/giasu.tranminhduc', 'PENDING', '[{"id":104,"title":"Bằng Thạc Sĩ Quản Lý Giáo Dục - ĐH Ngoại Thương","imageUrl":"https://images.unsplash.com/photo-1589829545856-d10d557cf95f?w=600&auto=format&fit=crop","date":"2024"}]'),
(7, 'Người Pháp gốc Việt, Thạc sĩ Lịch sử Nghệ thuật tại Sorbonne. Dạy ngôn ngữ qua văn hóa - nghệ thuật, điện ảnh và văn học.', 'Thạc sĩ · ĐH Sorbonne', 7, 'https://facebook.com/giasu.vuthimai', 'APPROVED', '[{"id":105,"title":"Thạc Sĩ Văn Hóa Nghệ Thuật - Sorbonne University","imageUrl":"https://images.unsplash.com/photo-1497633762265-9d179a990aa6?w=600&auto=format&fit=crop","date":"2022"}]'),
(8, 'Thạc sĩ KHMT ĐH Bách Khoa TP.HCM. Chuyên gia luyện thi Olympic Tin học và Toán ứng dụng.', 'Thạc sĩ Khoa học Máy tính', 5, 'https://facebook.com/giasu.danghoangnam', 'PENDING', '[{"id":106,"title":"Chứng nhận Giải Ba Olympic Tin học Toàn quốc","imageUrl":"https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=600&auto=format&fit=crop","date":"2023"}]');

-- Insert Parents
INSERT IGNORE INTO parents (user_id, address, emergency_contact, student_name, student_grade_level, student_school_name) VALUES
(2, '123 Đường Nguyễn Huệ, Quận 1, TP.HCM', '0907654321', 'Trần Văn C', 'Lớp 11', 'THPT Lê Hồng Phong');

-- 3. Insert Subjects (Đầy đủ 8 môn học)
INSERT IGNORE INTO subjects (id, code, name, description) VALUES
(1, 'MATH', 'Toán Học', 'Toán Đại Số & Hình Học phổ thông'),
(2, 'PHYS', 'Vật Lý', 'Vật Lý đại cương & Phổ thông'),
(3, 'CHEM', 'Hóa Học', 'Hóa Học Phổ thông & Luyện thi'),
(4, 'ENG', 'Tiếng Anh', 'Tiếng Anh giao tiếp, Ôn thi THPT & IELTS'),
(5, 'BIO', 'Sinh Học', 'Sinh Học Phổ thông & Luyện thi Y Dược'),
(6, 'CS', 'Tin Học', 'Tin Học lập trình, Thuật toán & Tin học trẻ'),
(7, 'LIT', 'Ngữ Văn', 'Ngữ Văn Phổ thông & Luyện thi Đại học'),
(8, 'HIST', 'Lịch Sử', 'Lịch Sử Việt Nam & Thế Giới');

-- 4. Insert Tutor Subjects
INSERT IGNORE INTO tutor_subjects (tutor_id, subject_id) VALUES
(1, 1), (1, 2), (1, 6),
(4, 3), (4, 5),
(5, 5), (5, 3),
(6, 4), (6, 8),
(7, 4),
(8, 1), (8, 6);

-- 5. Insert Sample Tutoring Class / Contract
INSERT IGNORE INTO tutoring_classes (id, class_name, tutor_id, student_name, student_grade_level, student_school_name, parent_id, subject_id, schedule_description, connection_fee, status) VALUES
(1, 'Lớp Toán 11 - Em Trần Văn C', 1, 'Trần Văn C', 'Lớp 11', 'THPT Lê Hồng Phong', 2, 1, 'Thứ 2 - Thứ 6 (18:00 - 20:00)', 5000.00, 'ACTIVE');

-- 6. Insert Completed Lesson
INSERT IGNORE INTO lessons (id, class_id, title, start_time, end_time, status) VALUES
(1, 1, 'Buổi 1: Phương trình bậc 2 & Công thức Delta', '2026-08-07 18:00:00', '2026-08-07 20:00:00', 'COMPLETED');

-- 7. Insert Lesson Note & AI Note
INSERT IGNORE INTO lesson_notes (lesson_id, raw_tutor_note, ai_summary, key_learnings, areas_for_improvement) VALUES
(1, 
 'Em C nắm khá tốt công thức Delta. Tuy nhiên vẫn hay tính nhầm dấu khi Delta âm. Đã cho làm 5 bài tập tại lớp, đúng 4/5.',
 'Học sinh C đã hiểu cơ bản phương trình bậc 2 và vận dụng tốt công thức Delta.',
 'Nắm vững công thức Delta, giải đúng 80% bài tập trên lớp.',
 'Cần chú ý cẩn thận khi tính toán với số âm để tránh sai sót đáng tiếc.');
