package com.giasuhq.service.impl;

import com.giasuhq.dto.request.CreateTutorRequest;
import com.giasuhq.dto.response.TutorResponse;
import com.giasuhq.entity.Tutor;
import com.giasuhq.exception.ResourceNotFoundException;
import com.giasuhq.mapper.TutorMapper;
import com.giasuhq.repository.TutorRepository;
import com.giasuhq.service.TutorService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TutorServiceImpl implements TutorService {

    private final TutorRepository tutorRepository;
    private final TutorMapper tutorMapper;

    public TutorServiceImpl(TutorRepository tutorRepository, TutorMapper tutorMapper) {
        this.tutorRepository = tutorRepository;
        this.tutorMapper = tutorMapper;
    }

    @Override
    public List<TutorResponse> findAll() {
        return tutorRepository.findAll()
                .stream()
                .map(tutorMapper::toResponse)
                .toList();
    }

    @Override
    public TutorResponse findById(Long id) {
        Tutor tutor = tutorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tutor not found with id: " + id));
        return tutorMapper.toResponse(tutor);
    }

    @Override
    public TutorResponse create(CreateTutorRequest request) {
        Tutor tutor = tutorMapper.toEntity(null, request);
        return tutorMapper.toResponse(tutorRepository.save(tutor));
    }
}
