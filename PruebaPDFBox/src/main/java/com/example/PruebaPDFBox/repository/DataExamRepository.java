package com.example.PruebaPDFBox.repository;

import com.example.PruebaPDFBox.model.DataExam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DataExamRepository extends JpaRepository<DataExam, Long> {
}
