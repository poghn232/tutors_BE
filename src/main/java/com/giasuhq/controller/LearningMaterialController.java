package com.giasuhq.controller;

import com.giasuhq.dto.request.CreateMaterialRequest;
import com.giasuhq.dto.request.UpdateMaterialRequest;
import com.giasuhq.dto.response.ApiResponse;
import com.giasuhq.dto.response.MaterialResponse;
import com.giasuhq.entity.User;
import com.giasuhq.repository.UserRepository;
import com.giasuhq.service.LearningMaterialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
public class LearningMaterialController {

    private final LearningMaterialService materialService;
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<List<MaterialResponse>> getMaterials(
            @RequestParam(value = "subject", required = false) String subject,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "search", required = false) String search) {
        List<MaterialResponse> responses = materialService.getAllMaterials(subject, type, search);
        return ApiResponse.success("Lấy danh sách tài liệu thành công", responses);
    }

    @GetMapping("/{id}")
    public ApiResponse<MaterialResponse> getMaterialById(@PathVariable Long id) {
        MaterialResponse response = materialService.getMaterialById(id);
        return ApiResponse.success("Lấy chi tiết tài liệu thành công", response);
    }

    @PostMapping
    public ApiResponse<MaterialResponse> createMaterial(
            @Valid @RequestBody CreateMaterialRequest request,
            Principal principal) {
        User user = getUserByPrincipal(principal);
        MaterialResponse response = materialService.createMaterial(user, request);
        return ApiResponse.success("Thêm tài liệu học tập mới thành công!", response);
    }

    @PutMapping("/{id}")
    public ApiResponse<MaterialResponse> updateMaterial(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMaterialRequest request,
            Principal principal) {
        User user = getUserByPrincipal(principal);
        MaterialResponse response = materialService.updateMaterial(user, id, request);
        return ApiResponse.success("Cập nhật tài liệu học tập thành công!", response);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteMaterial(
            @PathVariable Long id,
            Principal principal) {
        User user = getUserByPrincipal(principal);
        materialService.deleteMaterial(user, id);
        return ApiResponse.success("Xóa tài liệu học tập thành công!", null);
    }

    @PostMapping("/{id}/download")
    public ApiResponse<MaterialResponse> incrementDownload(@PathVariable Long id) {
        MaterialResponse response = materialService.incrementDownloadCount(id);
        return ApiResponse.success("Tăng lượt tải thành công", response);
    }

    private User getUserByPrincipal(Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Chưa đăng nhập hoặc phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.");
        }
        String email = principal.getName();
        return userRepository.findByEmailIgnoreCase(email)
                .or(() -> userRepository.findByEmail(email))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với tài khoản: " + email));
    }
}
