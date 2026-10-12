package com.giasuhq.service;

import com.giasuhq.dto.response.ProgressReportResponse;
import com.giasuhq.entity.User;

public interface ProgressReportService {
    ProgressReportResponse generateReport(User currentUser, Long classId);
}
