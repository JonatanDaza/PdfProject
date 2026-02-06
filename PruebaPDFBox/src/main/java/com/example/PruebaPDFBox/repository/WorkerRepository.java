package com.example.PruebaPDFBox.repository;

import com.example.PruebaPDFBox.model.Worker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkerRepository extends JpaRepository<Worker, Long> {
    Optional<Worker> findByCC(String cc);
}
