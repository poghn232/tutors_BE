-- =========================================================
-- GiaSuHQ MVP Initial Seed Data (Data.sql - 9 Tables)
-- =========================================================

-- 1. Insert Base Users
INSERT INTO users (id, email, password, full_name, phone, role) VALUES
(1, 'tutor.nguyen@giasuhq.com', '$2a$10$e.g123456hash', 'Nguyễn Văn A', '0901234567', 'TUTOR'),
(2, 'parent.tran@giasuhq.com', '$2a$10$e.g123456hash', 'Trần Thị B', '0907654321', 'PARENT'),
(3, 'student.tran@giasuhq.com', '$2a$10$e.g123456hash', 'Trần Văn C', '0909999999', 'STUDENT');

SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));

-- 2. Insert Inherited Role Data
INSERT INTO tutors (user_id, bio, qualification, experience_years, hourly_rate) VALUES
(1, 'Gia sư Toán & Lý hơn 5 năm kinh nghiệm dạy kèm cấp 2 và cấp 3.', 'Cử nhân Sư Phàm Toán', 5, 200000);

INSERT INTO parents (user_id, address, emergency_contact) VALUES
(2, '123 Đường Nguyễn Huệ, Quận 1, TP.HCM', '0907654321');

INSERT INTO students (user_id, parent_id, grade_level, school_name) VALUES
(3, 2, 'Lớp 11', 'THPT Lê Hồng Phong');

-- 3. Insert Subjects
INSERT INTO subjects (id, code, name, description) VALUES
(1, 'MATH', 'Toán Học', 'Toán Đại Số & Hình Học phổ thông'),
(2, 'PHYS', 'Vật Lý', 'Vật Lý đại cương & Phổ thông'),
(3, 'CHEM', 'Hóa Học', 'Hóa Học Phổ thông');

SELECT setval('subjects_id_seq', (SELECT MAX(id) FROM subjects));

-- 4. Insert Tutor Subjects
INSERT INTO tutor_subjects (tutor_id, subject_id) VALUES
(1, 1),
(1, 2);

-- 5. Insert Tutoring Class / Contract
INSERT INTO tutoring_classes (id, class_name, tutor_id, student_id, parent_id, subject_id, schedule_description, status) VALUES
(1, 'Lớp Toán 11 - Em Trần Văn C', 1, 3, 2, 1, 'Thứ 2 - Thứ 6 (18:00 - 20:00)', 'ACTIVE');

SELECT setval('tutoring_classes_id_seq', (SELECT MAX(id) FROM tutoring_classes));

-- 6. Insert Completed Lesson
INSERT INTO lessons (id, class_id, title, start_time, end_time, status) VALUES
(1, 1, 'Buổi 1: Phương trình bậc 2 & Công thức Delta', '2026-08-07 18:00:00', '2026-08-07 20:00:00', 'COMPLETED');

SELECT setval('lessons_id_seq', (SELECT MAX(id) FROM lessons));

-- 7. Insert Lesson Note & AI Note
INSERT INTO lesson_notes (lesson_id, raw_tutor_note, ai_summary, key_learnings, areas_for_improvement) VALUES
(1, 
 'Em C nắm khá tốt công thức Delta. Tuy nhiên vẫn hay tính nhầm dấu khi Delta âm. Đã cho làm 5 bài tập tại lớp, đúng 4/5.',
 'Học sinh C đã hiểu cơ bản phương trình bậc 2 và vận dụng tốt công thức Delta.',
 'Nắm vững công thức Delta, giải đúng 80% bài tập trên lớp.',
 'Cần chú ý cẩn thận khi tính toán với số âm để tránh sai sót đáng tiếc.');
