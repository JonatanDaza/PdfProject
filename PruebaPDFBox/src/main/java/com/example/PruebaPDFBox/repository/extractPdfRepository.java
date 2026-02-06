package com.example.PruebaPDFBox.repository;

import com.example.PruebaPDFBox.model.ExtractPdf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface extractPdfRepository extends JpaRepository<ExtractPdf, Long> {
}
