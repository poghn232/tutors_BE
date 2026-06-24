package com.giasuhq.repository;

import com.giasuhq.entity.Tutor;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

@Repository
public class TutorRepository {

    private final AtomicLong idSequence = new AtomicLong(0);
    private final List<Tutor> tutors = new ArrayList<>();

    public List<Tutor> findAll() {
        return List.copyOf(tutors);
    }

    public Optional<Tutor> findById(Long id) {
        return tutors.stream()
                .filter(tutor -> tutor.getId().equals(id))
                .findFirst();
    }

    public Tutor save(Tutor tutor) {
        if (tutor.getId() == null) {
            tutor.setId(idSequence.incrementAndGet());
        }
        tutors.add(tutor);
        return tutor;
    }
}
