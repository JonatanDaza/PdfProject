package com.example.PruebaPDFBox.repository;

import com.example.PruebaPDFBox.model.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {
    Optional<Exam> findByExamDateAndExamName(String examDate, String examName);
}
