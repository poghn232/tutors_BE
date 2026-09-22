-- =========================================================
-- GiaSuHQ MVP Initial Seed Data (Data.sql - 8 Tables)
-- =========================================================

-- 1. Insert Base Users (Mật khẩu chuẩn BCrypt của '123456')
INSERT IGNORE INTO users (id, email, password, full_name, phone, role, is_vip, balance, email_verified) VALUES
(1, 'tutor.nguyen@giasuhq.com', '$2a$10$10Q2J.X5iX/KOM4nHtFMfeXi4JoW3O6sv4ZtaJ6Ab2P0FNC71XcpO', 'Hoàng Thiên Ứng', '0901234567', 'TUTOR', true, 0, true),
(2, 'parent.tran@giasuhq.com', '$2a$10$10Q2J.X5iX/KOM4nHtFMfeXi4JoW3O6sv4ZtaJ6Ab2P0FNC71XcpO', 'Trần Thị B', '0907654321', 'PARENT', true, 500000, true),
(3, 'admin@giasuhq.com', '$2a$10$I9klrgy1h/1nBfZcrsooIO1dMDIRrefD/CJL9kRJz194/zb60fW7C', 'Quản Trị Viên Hệ Thống', '0909999999', 'ADMIN', true, 0, true),
(4, 'tutor.lan@giasuhq.com', '$2a$10$10Q2J.X5iX/KOM4nHtFMfeXi4JoW3O6sv4ZtaJ6Ab2P0FNC71XcpO', 'TS. Phạm Thị Lan', '0902222333', 'TUTOR', true, 0, true),
(5, 'tutor.thu@giasuhq.com', '$2a$10$10Q2J.X5iX/KOM4nHtFMfeXi4JoW3O6sv4ZtaJ6Ab2P0FNC71XcpO', 'TS. Lê Thị Thu', '0903333444', 'TUTOR', true, 0, true),
(6, 'tutor.duc@giasuhq.com', '$2a$10$10Q2J.X5iX/KOM4nHtFMfeXi4JoW3O6sv4ZtaJ6Ab2P0FNC71XcpO', 'Trần Minh Đức', '0904444555', 'TUTOR', false, 0, true),
(7, 'tutor.mai@giasuhq.com', '$2a$10$10Q2J.X5iX/KOM4nHtFMfeXi4JoW3O6sv4ZtaJ6Ab2P0FNC71XcpO', 'Vũ Thị Mai', '0905555666', 'TUTOR', false, 0, true),
(8, 'tutor.nam@giasuhq.com', '$2a$10$10Q2J.X5iX/KOM4nHtFMfeXi4JoW3O6sv4ZtaJ6Ab2P0FNC71XcpO', 'Đặng Hoàng Nam', '0906666777', 'TUTOR', true, 0, true),
(11, 'testtest123@gmail.com', '$2a$10$I9klrgy1h/1nBfZcrsooIO1dMDIRrefD/CJL9kRJz194/zb60fW7C', 'giasutest1', '0901111111', 'TUTOR', true, 0, true),
(12, 'giasutest2@gmail.com', '$2a$10$I9klrgy1h/1nBfZcrsooIO1dMDIRrefD/CJL9kRJz194/zb60fW7C', 'giasutest2', '0902222222', 'TUTOR', true, 0, true),
(13, 'giasutest3@gmail.com', '$2a$10$I9klrgy1h/1nBfZcrsooIO1dMDIRrefD/CJL9kRJz194/zb60fW7C', 'giasutest3', '0903333333', 'TUTOR', true, 0, true);

-- 2. Insert Inherited Role Data for Tutors
INSERT IGNORE INTO tutors (user_id, bio, qualification, experience_years, facebook_url, verification_status, certificates_json) VALUES
(1, 'Tiến sĩ Toán học ứng dụng tại ĐH Quốc gia Hà Nội. Tôi giúp học sinh hiểu toán học qua các ứng dụng thực tế. 8+ năm kinh nghiệm từ THCS đến đại học.', 'Tiến sĩ Toán học', 8, 'https://facebook.com/giasu.hoangthienung', 'APPROVED', '[{"id":101,"title":"Bằng Tiến Sĩ Toán Học - ĐHQGHN","imageUrl":"https://images.unsplash.com/photo-1523240795612-9a054b0db644?w=600&auto=format&fit=crop","date":"2023"}]'),
(4, 'Tiến sĩ Y khoa tại ĐH Y Hà Nội. Chuyên luyện thi y dược và khoa học tự nhiên. 96% học sinh đậu kỳ thi quốc gia.', 'Tiến sĩ Y khoa · ĐH Y Hà Nội', 10, 'https://facebook.com/giasu.phamthilan', 'APPROVED', '[{"id":102,"title":"Bằng Bác Sĩ Chuyên Khoa - ĐH Y Hà Nội","imageUrl":"https://images.unsplash.com/photo-1576091160399-112ba8d25d1d?w=600&auto=format&fit=crop","date":"2022"}]'),
(5, 'Cựu giảng viên đại học với niềm đam mê làm cho khoa học trở nên thú vị. Sử dụng thí nghiệm thực hành và ví dụ thực tế.', 'Tiến sĩ · ĐH Stanford (Hoa Kỳ)', 12, 'https://facebook.com/giasu.lethithu', 'APPROVED', '[{"id":103,"title":"Chứng nhận Giảng viên Xuất sắc Stanford","imageUrl":"https://images.unsplash.com/photo-1517486808906-6ca8b3f04846?w=600&auto=format&fit=crop","date":"2021"}]'),
(6, 'Thạc sĩ Giáo dục tại ĐH Ngoại Thương. Cựu giáo viên THPT, chuyên gia luyện thi đại học với tỉ lệ học sinh đậu 95%.', 'Thạc sĩ · ĐH Ngoại Thương', 6, 'https://facebook.com/giasu.tranminhduc', 'PENDING', '[{"id":104,"title":"Bằng Thạc Sĩ Quản Lý Giáo Dục - ĐH Ngoại Thương","imageUrl":"https://images.unsplash.com/photo-1589829545856-d10d557cf95f?w=600&auto=format&fit=crop","date":"2024"}]'),
(7, 'Người Pháp gốc Việt, Thạc sĩ Lịch sử Nghệ thuật tại Sorbonne. Dạy ngôn ngữ qua văn hóa - nghệ thuật, điện ảnh và văn học.', 'Thạc sĩ · ĐH Sorbonne', 7, 'https://facebook.com/giasu.vuthimai', 'APPROVED', '[{"id":105,"title":"Thạc Sĩ Văn Hóa Nghệ Thuật - Sorbonne University","imageUrl":"https://images.unsplash.com/photo-1497633762265-9d179a990aa6?w=600&auto=format&fit=crop","date":"2022"}]'),
(8, 'Thạc sĩ KHMT ĐH Bách Khoa TP.HCM. Chuyên gia luyện thi Olympic Tin học và Toán ứng dụng.', 'Thạc sĩ Khoa học Máy tính', 5, 'https://facebook.com/giasu.danghoangnam', 'PENDING', '[{"id":106,"title":"Chứng nhận Giải Ba Olympic Tin học Toàn quốc","imageUrl":"https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=600&auto=format&fit=crop","date":"2023"}]'),
(11, 'Gia sư chuyên môn Toán học & Khoa học Tự nhiên. Tận tâm, nhiệt tình giúp học sinh nắm vững kiến thức.', 'Cử nhân Sư phạm Toán', 3, 'https://facebook.com/giasutest1', 'APPROVED', '[{"id":111,"title":"Chứng chỉ Nghiệp vụ Sư phạm","imageUrl":"https://images.unsplash.com/photo-1523240795612-9a054b0db644?w=600&auto=format&fit=crop","date":"2023"}]'),
(12, 'Gia sư chuyên môn Vật Lý & Hóa học. Phương pháp dạy trực quan, dễ hiểu.', 'Thạc sĩ Khoa học Tự nhiên', 4, 'https://facebook.com/giasutest2', 'APPROVED', '[{"id":112,"title":"Chứng nhận Giảng dạy Xuất sắc","imageUrl":"https://images.unsplash.com/photo-1576091160399-112ba8d25d1d?w=600&auto=format&fit=crop","date":"2023"}]'),
(13, 'Gia sư chuyên môn Tiếng Anh & Ngữ Văn. Luyện thi chứng chỉ quốc tế và kỳ thi THPT.', 'Cử nhân Sư phạm Ngoại ngữ', 5, 'https://facebook.com/giasutest3', 'APPROVED', '[{"id":113,"title":"Chứng chỉ IELTS 8.0 & Sư phạm","imageUrl":"https://images.unsplash.com/photo-1517486808906-6ca8b3f04846?w=600&auto=format&fit=crop","date":"2023"}]');


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
(8, 1), (8, 6),
(11, 1), (11, 2),
(12, 2), (12, 3),
(13, 4), (13, 7);


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

-- 8. Insert Initial Learning Materials
INSERT IGNORE INTO learning_materials (id, title, description, subject_name, material_type, type_badge, badge_extra, author_name, file_url, file_name, file_size, is_vip, downloads_count, btn_text, btn_color, created_by) VALUES
(1, 'Công thức Toán THPT tổng hợp', 'Tổng hợp toàn bộ công thức Toán từ lớp 10 đến 12, bao gồm Đại số, Hình học và Giải tích.', 'Toán học', 'pdf', 'PDF', NULL, 'TS. Nguyễn Thị Hoa', '/uploads/cong_thuc_toan_thpt.pdf', 'cong_thuc_toan_thpt.pdf', '1.2 MB', FALSE, 2341, 'Tải xuống (1.2MB)', '#f97316', 1),
(2, 'Video giải bài Vật lý sóng âm', 'Hướng dẫn chi tiết giải các dạng bài tập sóng âm, giao thoa sóng và hiệu ứng Doppler.', 'Vật lý', 'video', 'Video', 'MỚI', 'TS. Nguyễn Thị Hoa', '/uploads/video_song_am.mp4', 'video_song_am.mp4', '24 MB', FALSE, 1892, 'Tải xuống (24 phút)', '#7c3aed', 1),
(3, 'Từ vựng Tiếng Anh chủ đề môi trường', 'Bộ từ vựng 200+ từ về môi trường, biến đổi khí hậu và phát triển bền vững kèm ví dụ.', 'Tiếng Anh', 'pdf', 'PDF', NULL, 'Trần Minh Đức', '/uploads/tu_vung_tieng_anh_moi_truong.pdf', 'tu_vung_tieng_anh_moi_truong.pdf', '0.8 MB', FALSE, 3104, 'Tải xuống (0.8MB)', '#f43f5e', 6),
(4, 'Trắc nghiệm Sinh học tế bào', 'Bộ 80 câu trắc nghiệm về cấu trúc và chức năng tế bào, có đáp án và giải thích chi tiết.', 'Sinh học', 'quiz', 'Trắc nghiệm', NULL, 'TS. Lê Thị Thu', '/uploads/trac_nghiem_sinh_hoc_te_bao.pdf', 'trac_nghiem_sinh_hoc_te_bao.pdf', '1.5 MB', FALSE, 1567, 'Tải xuống', '#00c288', 5),
(5, 'Đề thi thử THPT quốc gia Toán 2026', 'Đề thi chuẩn cấu trúc Bộ GD&ĐT kèm video chữa bài độc quyền từ thủ khoa và giáo viên chuyên.', 'Toán học', 'pdf', 'PDF', NULL, 'TS. Nguyễn Thị Hoa', '/uploads/de_thi_thu_toan_thpt_2026.pdf', 'de_thi_thu_toan_thpt_2026.pdf', '3.5 MB', TRUE, 841, 'Tải xuống (3.5MB)', '#2563eb', 1),
(6, 'Bài tập Hóa hữu cơ cơ chế phản ứng', 'Tuyển tập 150 câu bài tập cơ chế chuyên sâu dành cho học sinh giỏi và thi chuyên.', 'Hóa học', 'exercise', 'Bài tập', NULL, 'TS. Phạm Thị Lan', '/uploads/bai_tap_hoa_huu_co.pdf', 'bai_tap_hoa_huu_co.pdf', '2.1 MB', TRUE, 712, 'Tải xuống (2.1MB)', '#2563eb', 4),
(7, 'Video luyện nghe IELTS 7.5+ chuyên đề Science', 'Chiến thuật bắt key words và bẫy phát âm trong Section 4 bài thi IELTS Listening.', 'Tiếng Anh', 'video', 'Video', NULL, 'Trần Minh Đức', '/uploads/video_ielts_listening_science.mp4', 'video_ielts_listening_science.mp4', '45 MB', TRUE, 954, 'Tải xuống (45 phút)', '#2563eb', 6),
(8, 'Trắc nghiệm Vật lý hạt nhân 12 nâng cao', 'Dạng bài toán phóng xạ, năng lượng liên kết và phản ứng nhiệt hạch có độ phân hóa cao.', 'Vật lý', 'quiz', 'Trắc nghiệm', NULL, 'TS. Nguyễn Thị Hoa', '/uploads/trac_nghiem_vat_ly_hat_nhan.pdf', 'trac_nghiem_vat_ly_hat_nhan.pdf', '1.4 MB', TRUE, 623, 'Tải xuống (1.4MB)', '#2563eb', 1),
(9, 'Sổ tay công thức Hóa học 10-11-12', 'Bản in tóm tắt bỏ túi toàn bộ lý thuyết, bảng tính tan, chuỗi thế điện cực và mẹo giải nhanh.', 'Luyện thi THPT', 'pdf', 'PDF', NULL, 'TS. Phạm Thị Lan', '/uploads/so_tay_hoa_hoc_10_11_12.pdf', 'so_tay_hoa_hoc_10_11_12.pdf', '1.8 MB', FALSE, 4102, 'Tải xuống (1.8MB)', '#f97316', 4);
