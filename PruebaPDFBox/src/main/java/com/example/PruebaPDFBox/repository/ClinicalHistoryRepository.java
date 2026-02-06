package com.example.PruebaPDFBox.repository;

import com.example.PruebaPDFBox.model.ClinicalHistory;
import com.example.PruebaPDFBox.model.Worker;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClinicalHistoryRepository  extends JpaRepository<ClinicalHistory,Long> {

    @Query("SELECT DISTINCT e.clinicalHistory FROM Exam e WHERE e.worker = :worker AND e.examDate = :examDate")
    Optional<ClinicalHistory> findByWorkerAndExamDate(@Param("worker") Worker worker, @Param("examDate") String examDate);

    @EntityGraph(attributePaths = {"exams", "exams.dataExam", "exams.worker"})
    @Query("SELECT ch FROM ClinicalHistory ch")
    Page<ClinicalHistory> findAllPaged(Pageable pageable);
}
