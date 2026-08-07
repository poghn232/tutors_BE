-- =========================================================
-- GiaSuHQ MVP PostgreSQL Database Schema (Inheritance & Complete Domain)
-- Compatible with Supabase PostgreSQL & Local PostgreSQL
-- =========================================================

-- Drop existing tables if re-initializing (Order respects FK constraints)
DROP TABLE IF EXISTS lesson_notes CASCADE;
DROP TABLE IF EXISTS lessons CASCADE;
DROP TABLE IF EXISTS tutoring_classes CASCADE;
DROP TABLE IF EXISTS tutor_subjects CASCADE;
DROP TABLE IF EXISTS subjects CASCADE;
DROP TABLE IF EXISTS parents CASCADE;
DROP TABLE IF EXISTS students CASCADE;
DROP TABLE IF EXISTS tutors CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- 1. Base Users Table (Chứa thông tin đăng nhập & định danh chung)
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    avatar_url VARCHAR(500),
    role VARCHAR(20) NOT NULL CHECK (role IN ('PARENT', 'STUDENT', 'TUTOR', 'ADMIN')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Tutors Table (Kế thừa từ Users qua FK user_id)
CREATE TABLE tutors (
    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    bio TEXT,
    qualification VARCHAR(255),           -- Trình độ (Đại học, Thạc sĩ...)
    experience_years INT DEFAULT 0,       -- Số năm kinh nghiệm
    hourly_rate DOUBLE PRECISION DEFAULT 0.0
);

-- 3. Parents Table (Kế thừa từ Users qua FK user_id)
CREATE TABLE parents (
    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    address VARCHAR(255),
    emergency_contact VARCHAR(50)
);

-- 4. Students Table (Kế thừa từ Users qua FK user_id, liên kết Phụ huynh)
CREATE TABLE students (
    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    parent_id BIGINT REFERENCES parents(user_id) ON DELETE SET NULL,
    grade_level VARCHAR(50),               -- Khối lớp (Lớp 10, Lớp 11...)
    school_name VARCHAR(255)
);

-- 5. Subjects Table (Danh mục môn học)
CREATE TABLE subjects (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,      -- MATH, PHYS, CHEM, ENG...
    name VARCHAR(100) NOT NULL,            -- Toán Học, Vật Lý, Hóa Học...
    description TEXT
);

-- 6. Tutor Subjects (Bảng trung gian Môn học Gia sư nhận dạy)
CREATE TABLE tutor_subjects (
    tutor_id BIGINT NOT NULL REFERENCES tutors(user_id) ON DELETE CASCADE,
    subject_id BIGINT NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    PRIMARY KEY (tutor_id, subject_id)
);

-- 7. Tutoring Classes Table (Lớp học / Hợp đồng Dạy kèm nối Tutor - Student - Parent)
CREATE TABLE tutoring_classes (
    id BIGSERIAL PRIMARY KEY,
    class_name VARCHAR(255) NOT NULL,
    tutor_id BIGINT NOT NULL REFERENCES tutors(user_id),
    student_id BIGINT NOT NULL REFERENCES students(user_id),
    parent_id BIGINT REFERENCES parents(user_id),
    subject_id BIGINT NOT NULL REFERENCES subjects(id),
    schedule_description VARCHAR(255),    -- Ví dụ: "Thứ 2 - Thứ 4 (18:00 - 20:00)"
    status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'COMPLETED', 'PAUSED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 8. Lessons Table (Các Buổi học chi tiết của Lớp học)
CREATE TABLE lessons (
    id BIGSERIAL PRIMARY KEY,
    class_id BIGINT NOT NULL REFERENCES tutoring_classes(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status VARCHAR(20) DEFAULT 'SCHEDULED' CHECK (status IN ('SCHEDULED', 'COMPLETED', 'CANCELLED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 9. Lesson Notes & AI Note Table (Ghi chú thô & AI Note của buổi học)
CREATE TABLE lesson_notes (
    id BIGSERIAL PRIMARY KEY,
    lesson_id BIGINT UNIQUE NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    raw_tutor_note TEXT NOT NULL,          -- Ghi chú thô do gia sư nhập
    ai_summary TEXT,                      -- Tóm tắt bài học do AI Note tạo
    key_learnings TEXT,                   -- Nội dung đã hoàn thành & điểm mạnh
    areas_for_improvement TEXT,           -- Cần cải thiện & bài tập về nhà
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for Query Performance
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_students_parent ON students(parent_id);
CREATE INDEX idx_classes_tutor ON tutoring_classes(tutor_id);
CREATE INDEX idx_classes_student ON tutoring_classes(student_id);
CREATE INDEX idx_lessons_class ON lessons(class_id);
