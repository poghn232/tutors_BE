package com.giasuhq.controller;

import com.giasuhq.dto.response.ApiResponse;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    private static final long MAX_FILE_SIZE_BYTES = 20L * 1024 * 1024;
    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final long MAX_REQUEST_SIZE_BYTES = 25L * 1024 * 1024;
    private static final int MAX_FILES_PER_REQUEST = 8;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".webp",
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".txt", ".csv",
            ".mp4", ".webm", ".mov", ".zip"
    );

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp",
            "application/pdf", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain", "text/csv", "application/zip", "application/x-zip-compressed",
            "video/mp4", "video/webm", "video/quicktime"
    );

    private final Path uploadDir = Paths.get("uploads").toAbsolutePath().normalize();

    public FileUploadController() {
        try {
            Files.createDirectories(this.uploadDir);
        } catch (Exception ex) {
            System.err.println("Could not create upload directory: " + ex.getMessage());
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            Map<String, Object> fileData = storeFile(file);
            return ResponseEntity.ok(ApiResponse.success("Tải tệp lên thành công.", fileData));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        } catch (IOException ex) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi khi lưu trữ tệp tin: " + ex.getMessage()));
        }
    }

    @PostMapping("/upload-multiple")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> uploadMultipleFiles(@RequestParam("files") MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Vui lòng chọn ít nhất một tệp cần tải lên."));
        }
        if (files.length > MAX_FILES_PER_REQUEST) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Mỗi lần chỉ được tải lên tối đa " + MAX_FILES_PER_REQUEST + " tệp."));
        }

        long totalSize = 0;
        try {
            for (MultipartFile file : files) {
                validateFile(file);
                totalSize += file.getSize();
            }
            if (totalSize > MAX_REQUEST_SIZE_BYTES) {
                return ResponseEntity.status(413).body(ApiResponse.error("Tổng dung lượng các tệp không được vượt quá 25MB."));
            }

            List<Map<String, Object>> results = new ArrayList<>();
            for (MultipartFile file : files) {
                results.add(storeFile(file));
            }
            return ResponseEntity.ok(ApiResponse.success("Tải các tệp lên thành công.", results));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        } catch (IOException ex) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi khi lưu trữ tệp tin: " + ex.getMessage()));
        }
    }

    private Map<String, Object> storeFile(MultipartFile file) throws IOException {
        validateFile(file);

        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : ""
        );
        if (originalFilename.isBlank() || originalFilename.contains("..")) {
            throw new IllegalArgumentException("Tên tệp không hợp lệ.");
        }

        String storedFileName = UUID.randomUUID().toString().replace("-", "") + "_" + originalFilename;
        Path targetLocation = this.uploadDir.resolve(storedFileName).normalize();
        if (!targetLocation.startsWith(this.uploadDir)) {
            throw new IllegalArgumentException("Tên tệp không hợp lệ.");
        }
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        Map<String, Object> fileData = new HashMap<>();
        fileData.put("fileId", UUID.randomUUID().toString());
        fileData.put("originalName", originalFilename);
        fileData.put("fileName", storedFileName);
        fileData.put("size", file.getSize());
        fileData.put("contentType", file.getContentType());
        fileData.put("fileUrl", "/api/files/view/" + storedFileName);
        fileData.put("downloadUrl", "/api/files/download/" + storedFileName);
        return fileData;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn tệp tin hợp lệ.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Tệp vượt quá giới hạn 20MB mỗi tệp.");
        }

        String filename = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : ""
        );
        String extension = getExtension(filename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Kiểu tệp không được hỗ trợ. Chỉ chấp nhận JPG, PNG, WEBP, PDF, DOC/DOCX, XLS/XLSX, PPT/PPTX, TXT, CSV, MP4, WEBM, MOV và ZIP.");
        }
        if (isImageExtension(extension) && file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new IllegalArgumentException("Tệp hình ảnh không được vượt quá 5MB.");
        }

        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            String normalizedContentType = contentType.toLowerCase(Locale.ROOT);
            if (!ALLOWED_CONTENT_TYPES.contains(normalizedContentType)
                    && !"application/octet-stream".equals(normalizedContentType)) {
                throw new IllegalArgumentException("Kiểu nội dung của tệp không được hỗ trợ.");
            }
        }
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 && dotIndex < filename.length() - 1
                ? filename.substring(dotIndex).toLowerCase(Locale.ROOT)
                : "";
    }

    private boolean isImageExtension(String extension) {
        return Set.of(".jpg", ".jpeg", ".png", ".webp").contains(extension);
    }

    @GetMapping("/view/{fileName:.+}")
    public ResponseEntity<Resource> viewFile(@PathVariable String fileName) {
        try {
            Path filePath = this.uploadDir.resolve(fileName).normalize();
            if (!filePath.startsWith(this.uploadDir)) {
                return ResponseEntity.badRequest().build();
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                String contentType = "application/octet-stream";
                try {
                    String probe = Files.probeContentType(filePath);
                    if (probe != null) contentType = probe;
                } catch (Exception ignored) {}

                String displayFilename = fileName;
                int underscoreIdx = fileName.indexOf('_');
                if (underscoreIdx > 0 && underscoreIdx < fileName.length() - 1) {
                    displayFilename = fileName.substring(underscoreIdx + 1);
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + displayFilename + "\"")
                        .header(HttpHeaders.CACHE_CONTROL, "max-age=86400, public")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        try {
            Path filePath = this.uploadDir.resolve(fileName).normalize();
            // Prevent path traversal
            if (!filePath.startsWith(this.uploadDir)) {
                return ResponseEntity.badRequest().build();
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                String contentType = "application/octet-stream";
                try {
                    String probe = Files.probeContentType(filePath);
                    if (probe != null) contentType = probe;
                } catch (Exception ignored) {}

                // Extract original filename if formatted as uuid_filename
                String displayFilename = fileName;
                int underscoreIdx = fileName.indexOf('_');
                if (underscoreIdx > 0 && underscoreIdx < fileName.length() - 1) {
                    displayFilename = fileName.substring(underscoreIdx + 1);
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + displayFilename + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException ex) {
            return ResponseEntity.badRequest().build();
        }
    }
}
