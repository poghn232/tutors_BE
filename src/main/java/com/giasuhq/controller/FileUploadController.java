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

    private final Path uploadDir = Paths.get("uploads").toAbsolutePath().normalize();

    public FileUploadController() {
        try {
            Files.createDirectories(this.uploadDir);
        } catch (Exception ex) {
            System.err.println("Could not create upload directory: " + ex.getMessage());
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadFile(@RequestParam(value = "file", required = false) MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Vui lòng chọn tệp tin hợp lệ."));
        }

        try {
            if (!Files.exists(this.uploadDir)) {
                Files.createDirectories(this.uploadDir);
            }
            String rawName = file.getOriginalFilename();
            String originalFilename = (rawName != null && !rawName.trim().isEmpty()) ? StringUtils.cleanPath(rawName) : "file_" + System.currentTimeMillis();
            // Sanitize against path traversal
            if (originalFilename.contains("..")) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Tên tệp không hợp lệ."));
            }

            String extension = "";
            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex > 0) {
                extension = originalFilename.substring(dotIndex);
            }

            String storedFileName = UUID.randomUUID().toString().replace("-", "") + "_" + originalFilename;
            Path targetLocation = this.uploadDir.resolve(storedFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            Map<String, Object> fileData = new HashMap<>();
            fileData.put("fileId", UUID.randomUUID().toString());
            fileData.put("originalName", originalFilename);
            fileData.put("fileName", storedFileName);
            fileData.put("size", file.getSize());
            fileData.put("contentType", file.getContentType());
            fileData.put("fileUrl", "/api/files/download/" + storedFileName);

            return ResponseEntity.ok(ApiResponse.success("Tải tệp lên thành công.", fileData));
        } catch (IOException ex) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi khi lưu trữ tệp tin: " + ex.getMessage()));
        }
    }

    @PostMapping("/upload-multiple")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> uploadMultipleFiles(@RequestParam(value = "files", required = false) MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Vui lòng chọn ít nhất một tệp tin."));
        }
        try {
            if (!Files.exists(this.uploadDir)) {
                Files.createDirectories(this.uploadDir);
            }
        } catch (Exception ignored) {}

        List<Map<String, Object>> results = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                try {
                    String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
                    if (originalFilename.contains("..")) continue;

                    String storedFileName = UUID.randomUUID().toString().replace("-", "") + "_" + originalFilename;
                    Path targetLocation = this.uploadDir.resolve(storedFileName);
                    Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

                    Map<String, Object> fileData = new HashMap<>();
                    fileData.put("fileId", UUID.randomUUID().toString());
                    fileData.put("originalName", originalFilename);
                    fileData.put("fileName", storedFileName);
                    fileData.put("size", file.getSize());
                    fileData.put("contentType", file.getContentType());
                    fileData.put("fileUrl", "/api/files/download/" + storedFileName);
                    results.add(fileData);
                } catch (IOException ignored) {
                }
            }
        }
        return ResponseEntity.ok(ApiResponse.success("Tải các tệp lên thành công.", results));
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
