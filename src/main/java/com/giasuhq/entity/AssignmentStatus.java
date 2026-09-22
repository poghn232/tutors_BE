package com.giasuhq.entity;

public enum AssignmentStatus {
    PENDING,        // Đang mở / Chờ nộp
    SUBMITTED,      // Đã nộp (Chờ gia sư chấm)
    GRADED,         // Đã chấm (Đã có điểm & nhận xét)
    NOT_SUBMITTED   // Không nộp (Đã quá hạn, không được phép nộp bù)
}
