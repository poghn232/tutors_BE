-- =========================================================
-- GiaSuHQ MVP MySQL Database Schema (PARENT, TUTOR, ADMIN Domain)
-- Compatible with Local & Remote MySQL Databases
-- =========================================================

-- Legacy migration: old student table is removed because child info now lives on parent profile.
-- Run this block on existing databases to keep schema aligned with the new model.
ALTER TABLE tutoring_classes DROP FOREIGN KEY IF EXISTS fk_classes_student;
ALTER TABLE tutoring_classes DROP INDEX IF EXISTS idx_classes_student;
ALTER TABLE tutoring_classes DROP COLUMN IF EXISTS student_id;
ALTER TABLE parents ADD COLUMN IF NOT EXISTS student_name VARCHAR(255) AFTER emergency_contact;
ALTER TABLE parents ADD COLUMN IF NOT EXISTS student_grade_level VARCHAR(50) AFTER student_name;
ALTER TABLE parents ADD COLUMN IF NOT EXISTS student_school_name VARCHAR(255) AFTER student_grade_level;
ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'PARENT';
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_vip BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS balance DECIMAL(15,2) NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified BOOLEAN DEFAULT FALSE;
ALTER TABLE tutors ADD COLUMN IF NOT EXISTS facebook_url VARCHAR(500);
ALTER TABLE tutors ADD COLUMN IF NOT EXISTS verification_status VARCHAR(30) DEFAULT 'APPROVED';
ALTER TABLE tutors ADD COLUMN IF NOT EXISTS rejection_reason TEXT;
ALTER TABLE tutors ADD COLUMN IF NOT EXISTS verified_at TIMESTAMP NULL;
ALTER TABLE tutors ADD COLUMN IF NOT EXISTS certificates_json LONGTEXT;
DROP TABLE IF EXISTS students;

-- Auto-migrate any legacy STUDENT users to PARENT role
UPDATE users SET role = 'PARENT' WHERE role = 'STUDENT';
INSERT IGNORE INTO parents (user_id)
SELECT id FROM users WHERE role = 'PARENT' AND id NOT IN (SELECT user_id FROM parents);

-- 1. Base Users Table (Chứa thông tin đăng nhập & định danh chung)
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    avatar_url VARCHAR(500),
    role VARCHAR(20) NOT NULL CHECK (role IN ('PARENT', 'TUTOR', 'ADMIN', 'STUDENT')),
    is_vip BOOLEAN DEFAULT FALSE,
    balance DECIMAL(15,2) NOT NULL DEFAULT 0,
    email_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_users_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Tutors Table (Kế thừa từ Users qua FK user_id)
CREATE TABLE IF NOT EXISTS tutors (
    user_id BIGINT PRIMARY KEY,
    bio TEXT,
    qualification VARCHAR(255),           -- Trình độ (Đại học, Thạc sĩ...)
    experience_years INT DEFAULT 0,       -- Số năm kinh nghiệm
    facebook_url VARCHAR(500),            -- Link Facebook cá nhân gia sư
    verification_status VARCHAR(30) DEFAULT 'PENDING',
    rejection_reason TEXT,
    verified_at TIMESTAMP NULL,
    certificates_json LONGTEXT,
    CONSTRAINT fk_tutors_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Parents Table (Kế thừa từ Users qua FK user_id, chứa thông tin con em / học sinh)
CREATE TABLE IF NOT EXISTS parents (
    user_id BIGINT PRIMARY KEY,
    address VARCHAR(255),
    emergency_contact VARCHAR(50),
    student_name VARCHAR(255),
    student_grade_level VARCHAR(50),
    student_school_name VARCHAR(255),
    CONSTRAINT fk_parents_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Subjects Table (Danh mục môn học)
CREATE TABLE IF NOT EXISTS subjects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,      -- MATH, PHYS, CHEM, ENG...
    name VARCHAR(100) NOT NULL,            -- Toán Học, Vật Lý, Hóa Học...
    description TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Tutor Subjects (Bảng trung gian Môn học Gia sư nhận dạy)
CREATE TABLE IF NOT EXISTS tutor_subjects (
    tutor_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    PRIMARY KEY (tutor_id, subject_id),
    CONSTRAINT fk_tutor_subjects_tutor FOREIGN KEY (tutor_id) REFERENCES tutors(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_tutor_subjects_subject FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Tutoring Classes Table (Lớp học / Hợp đồng Dạy kèm kết nối Tutor và Parent)
CREATE TABLE IF NOT EXISTS tutoring_classes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_name VARCHAR(255) NOT NULL,
    tutor_id BIGINT NOT NULL,
    student_name VARCHAR(255),
    student_grade_level VARCHAR(50),
    student_school_name VARCHAR(255),
    parent_id BIGINT,
    subject_id BIGINT NOT NULL,
    schedule_description VARCHAR(255),    -- Ví dụ: "Thứ 2 - Thứ 4 (18:00 - 20:00)"
    connection_fee DECIMAL(15,2) NOT NULL DEFAULT 0,
    approved_at TIMESTAMP NULL,
    paid_at TIMESTAMP NULL,
    status VARCHAR(30) DEFAULT 'PENDING_TUTOR_APPROVAL' CHECK (status IN ('PENDING_TUTOR_APPROVAL', 'PENDING_PAYMENT', 'ACTIVE', 'DECLINED', 'COMPLETED', 'PAUSED', 'CANCELLED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_classes_tutor FOREIGN KEY (tutor_id) REFERENCES tutors(user_id),
    CONSTRAINT fk_classes_parent FOREIGN KEY (parent_id) REFERENCES parents(user_id),
    CONSTRAINT fk_classes_subject FOREIGN KEY (subject_id) REFERENCES subjects(id),
    INDEX idx_classes_tutor (tutor_id),
    INDEX idx_classes_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Lessons Table (Các Buổi học chi tiết của Lớp học)
CREATE TABLE IF NOT EXISTS lessons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status VARCHAR(20) DEFAULT 'SCHEDULED' CHECK (status IN ('SCHEDULED', 'COMPLETED', 'CANCELLED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_lessons_class FOREIGN KEY (class_id) REFERENCES tutoring_classes(id) ON DELETE CASCADE,
    INDEX idx_lessons_class (class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Lesson Notes & AI Note Table (Ghi chú thô & AI Note của buổi học)
CREATE TABLE IF NOT EXISTS lesson_notes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lesson_id BIGINT UNIQUE NOT NULL,
    raw_tutor_note TEXT NOT NULL,          -- Ghi chú thô do gia sư nhập
    ai_summary TEXT,                      -- Tóm tắt bài học do AI Note tạo
    key_learnings TEXT,                   -- Nội dung đã hoàn thành & điểm mạnh
    areas_for_improvement TEXT,           -- Cần cải thiện & bài tập về nhà
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_notes_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. OTP Verification Challenges (persisted so restart/multiple instances do not lose OTPs)
CREATE TABLE IF NOT EXISTS otp_verifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    purpose VARCHAR(30) NOT NULL,
    otp_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_otp_email_purpose (email, purpose)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- 9. Assignments Table (Quản lý Bài tập: Gia sư giao bài, Phụ huynh nộp, Đánh giá 0-10 & Nhận xét)
CREATE TABLE IF NOT EXISTS assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    subject_name VARCHAR(100),
    tutor_id BIGINT NOT NULL,
    parent_id BIGINT NOT NULL,
    class_id BIGINT NULL,
    attachment_url VARCHAR(500),
    attachment_name VARCHAR(255),
    attachment_size VARCHAR(50),
    due_date TIMESTAMP NOT NULL,
    status VARCHAR(30) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SUBMITTED', 'GRADED', 'NOT_SUBMITTED')),
    submitted_file_url VARCHAR(500),
    submitted_file_name VARCHAR(255),
    submitted_file_size VARCHAR(50),
    submitted_at TIMESTAMP NULL,
    submission_note TEXT,
    rating DECIMAL(3,1) NULL,
    tutor_comment TEXT,
    graded_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_assignments_tutor FOREIGN KEY (tutor_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_assignments_parent FOREIGN KEY (parent_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_assignments_class FOREIGN KEY (class_id) REFERENCES tutoring_classes(id) ON DELETE SET NULL,
    INDEX idx_assignments_tutor (tutor_id),
    INDEX idx_assignments_parent (parent_id),
    INDEX idx_assignments_due_date (due_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. Learning Materials Table (Quản lý Tài liệu Học tập: Admin / Gia sư đăng, phân quyền VIP, đính kèm file <= 20MB)
CREATE TABLE IF NOT EXISTS learning_materials (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    subject_name VARCHAR(100) NOT NULL,
    material_type VARCHAR(50) NOT NULL DEFAULT 'pdf',   -- pdf, video, exercise, quiz
    type_badge VARCHAR(50) DEFAULT 'PDF',               -- PDF, Video, Bài tập, Trắc nghiệm
    badge_extra VARCHAR(50) NULL,                       -- MỚI, HOT...
    author_name VARCHAR(255),
    file_url VARCHAR(500),
    file_name VARCHAR(255),
    file_size VARCHAR(50),
    is_vip BOOLEAN NOT NULL DEFAULT FALSE,
    downloads_count INT NOT NULL DEFAULT 0,
    btn_text VARCHAR(100),
    btn_color VARCHAR(50),
    created_by BIGINT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_materials_creator FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_materials_subject (subject_name),
    INDEX idx_materials_is_vip (is_vip)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================================
-- Seed Data for Admin User (admin@giasuhq.com)
-- Password BCrypt: $2a$10$I9klrgy1h/1nBfZcrsooIO1dMDIRrefD/CJL9kRJz194/zb60fW7C
-- =========================================================
INSERT IGNORE INTO users (id, email, password, full_name, phone, role, is_vip, balance, email_verified) VALUES
(3, 'admin@giasuhq.com', '$2a$10$I9klrgy1h/1nBfZcrsooIO1dMDIRrefD/CJL9kRJz194/zb60fW7C', 'Quản Trị Viên Hệ Thống', '0909999999', 'ADMIN', true, 0, true)
ON DUPLICATE KEY UPDATE password = '$2a$10$I9klrgy1h/1nBfZcrsooIO1dMDIRrefD/CJL9kRJz194/zb60fW7C', full_name = 'Quản Trị Viên Hệ Thống', role = 'ADMIN';

-- =========================================================
-- Seed Data for Test Tutors (giasutest1, giasutest2, giasutest3)
-- Password BCrypt: $2a$10$I9klrgy1h/1nBfZcrsooIO1dMDIRrefD/CJL9kRJz194/zb60fW7C
-- =========================================================
INSERT IGNORE INTO users (id, email, password, full_name, phone, role, is_vip, balance, email_verified) VALUES
(11, 'testtest123@gmail.com', '$2a$10$I9klrgy1h/1nBfZcrsooIO1dMDIRrefD/CJL9kRJz194/zb60fW7C', 'giasutest1', '0901111111', 'TUTOR', true, 0, true),
(12, 'giasutest2@gmail.com', '$2a$10$I9klrgy1h/1nBfZcrsooIO1dMDIRrefD/CJL9kRJz194/zb60fW7C', 'giasutest2', '0902222222', 'TUTOR', true, 0, true),
(13, 'giasutest3@gmail.com', '$2a$10$I9klrgy1h/1nBfZcrsooIO1dMDIRrefD/CJL9kRJz194/zb60fW7C', 'giasutest3', '0903333333', 'TUTOR', true, 0, true)
ON DUPLICATE KEY UPDATE password = VALUES(password), full_name = VALUES(full_name), role = 'TUTOR';

INSERT IGNORE INTO tutors (user_id, bio, qualification, experience_years, facebook_url, verification_status, certificates_json) VALUES
(11, 'Gia sư chuyên môn Toán học & Khoa học Tự nhiên (Account Test 1). Tận tâm, nhiệt tình giúp học sinh nắm vững kiến thức.', 'Cử nhân Sư phạm Toán', 3, 'https://facebook.com/giasutest1', 'APPROVED', '[{"id":111,"title":"Chứng chỉ Nghiệp vụ Sư phạm","imageUrl":"https://images.unsplash.com/photo-1523240795612-9a054b0db644?w=600&auto=format&fit=crop","date":"2023"}]'),
(12, 'Gia sư chuyên môn Vật Lý & Hóa học (Account Test 2). Phương pháp dạy trực quan, dễ hiểu.', 'Thạc sĩ Khoa học Tự nhiên', 4, 'https://facebook.com/giasutest2', 'APPROVED', '[{"id":112,"title":"Chứng nhận Giảng dạy Xuất sắc","imageUrl":"https://images.unsplash.com/photo-1576091160399-112ba8d25d1d?w=600&auto=format&fit=crop","date":"2023"}]'),
(13, 'Gia sư chuyên môn Tiếng Anh & Ngữ Văn (Account Test 3). Luyện thi chứng chỉ quốc tế và kỳ thi THPT.', 'Cử nhân Sư phạm Ngoại ngữ', 5, 'https://facebook.com/giasutest3', 'APPROVED', '[{"id":113,"title":"Chứng chỉ IELTS 8.0 & Sư phạm","imageUrl":"https://images.unsplash.com/photo-1517486808906-6ca8b3f04846?w=600&auto=format&fit=crop","date":"2023"}]')
ON DUPLICATE KEY UPDATE verification_status = 'APPROVED';

INSERT IGNORE INTO tutor_subjects (tutor_id, subject_id) VALUES
(11, 1), (11, 2),
(12, 2), (12, 3),
(13, 4), (13, 7);

-- Seed Initial Learning Materials
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
