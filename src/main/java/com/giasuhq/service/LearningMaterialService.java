package com.giasuhq.service;

import com.giasuhq.dto.request.CreateMaterialRequest;
import com.giasuhq.dto.request.UpdateMaterialRequest;
import com.giasuhq.dto.response.MaterialResponse;
import com.giasuhq.entity.User;

import java.util.List;

public interface LearningMaterialService {

    List<MaterialResponse> getAllMaterials(String subject, String type, String search);

    MaterialResponse getMaterialById(Long id);

    MaterialResponse createMaterial(User user, CreateMaterialRequest request);

    MaterialResponse updateMaterial(User user, Long id, UpdateMaterialRequest request);

    void deleteMaterial(User user, Long id);

    MaterialResponse incrementDownloadCount(Long id);
}
