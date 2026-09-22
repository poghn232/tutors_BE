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

