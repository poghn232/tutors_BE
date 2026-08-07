-- =========================================================
-- GiaSuHQ MVP MySQL Database Schema (Inheritance & Complete Domain)
-- Compatible with Local & Remote MySQL Databases
-- =========================================================

SET FOREIGN_KEY_CHECKS = 0;

-- Drop existing tables if re-initializing
DROP TABLE IF EXISTS lesson_notes;
DROP TABLE IF EXISTS lessons;
DROP TABLE IF EXISTS tutoring_classes;
DROP TABLE IF EXISTS tutor_subjects;
DROP TABLE IF EXISTS subjects;
DROP TABLE IF EXISTS parents;
DROP TABLE IF EXISTS students;
DROP TABLE IF EXISTS tutors;
DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1;

-- 1. Base Users Table (Chứa thông tin đăng nhập & định danh chung)
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    avatar_url VARCHAR(500),
    role VARCHAR(20) NOT NULL CHECK (role IN ('PARENT', 'STUDENT', 'TUTOR', 'ADMIN')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Tutors Table (Kế thừa từ Users qua FK user_id)
CREATE TABLE tutors (
    user_id BIGINT PRIMARY KEY,
    bio TEXT,
    qualification VARCHAR(255),           -- Trình độ (Đại học, Thạc sĩ...)
    experience_years INT DEFAULT 0,       -- Số năm kinh nghiệm
    hourly_rate DOUBLE DEFAULT 0.0,
    CONSTRAINT fk_tutors_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Parents Table (Kế thừa từ Users qua FK user_id)
CREATE TABLE parents (
    user_id BIGINT PRIMARY KEY,
    address VARCHAR(255),
    emergency_contact VARCHAR(50),
    CONSTRAINT fk_parents_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Students Table (Kế thừa từ Users qua FK user_id, liên kết Phụ huynh)
CREATE TABLE students (
    user_id BIGINT PRIMARY KEY,
    parent_id BIGINT,
    grade_level VARCHAR(50),               -- Khối lớp (Lớp 10, Lớp 11...)
    school_name VARCHAR(255),
    CONSTRAINT fk_students_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_students_parent FOREIGN KEY (parent_id) REFERENCES parents(user_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Subjects Table (Danh mục môn học)
CREATE TABLE subjects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,      -- MATH, PHYS, CHEM, ENG...
    name VARCHAR(100) NOT NULL,            -- Toán Học, Vật Lý, Hóa Học...
    description TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Tutor Subjects (Bảng trung gian Môn học Gia sư nhận dạy)
CREATE TABLE tutor_subjects (
    tutor_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    PRIMARY KEY (tutor_id, subject_id),
    CONSTRAINT fk_tutor_subjects_tutor FOREIGN KEY (tutor_id) REFERENCES tutors(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_tutor_subjects_subject FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Tutoring Classes Table (Lớp học / Hợp đồng Dạy kèm nối Tutor - Student - Parent)
CREATE TABLE tutoring_classes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_name VARCHAR(255) NOT NULL,
    tutor_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    parent_id BIGINT,
    subject_id BIGINT NOT NULL,
    schedule_description VARCHAR(255),    -- Ví dụ: "Thứ 2 - Thứ 4 (18:00 - 20:00)"
    status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'COMPLETED', 'PAUSED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_classes_tutor FOREIGN KEY (tutor_id) REFERENCES tutors(user_id),
    CONSTRAINT fk_classes_student FOREIGN KEY (student_id) REFERENCES students(user_id),
    CONSTRAINT fk_classes_parent FOREIGN KEY (parent_id) REFERENCES parents(user_id),
    CONSTRAINT fk_classes_subject FOREIGN KEY (subject_id) REFERENCES subjects(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Lessons Table (Các Buổi học chi tiết của Lớp học)
CREATE TABLE lessons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status VARCHAR(20) DEFAULT 'SCHEDULED' CHECK (status IN ('SCHEDULED', 'COMPLETED', 'CANCELLED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_lessons_class FOREIGN KEY (class_id) REFERENCES tutoring_classes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. Lesson Notes & AI Note Table (Ghi chú thô & AI Note của buổi học)
CREATE TABLE lesson_notes (
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

-- Indexes for Query Performance
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_students_parent ON students(parent_id);
CREATE INDEX idx_classes_tutor ON tutoring_classes(tutor_id);
CREATE INDEX idx_classes_student ON tutoring_classes(student_id);
CREATE INDEX idx_lessons_class ON lessons(class_id);
