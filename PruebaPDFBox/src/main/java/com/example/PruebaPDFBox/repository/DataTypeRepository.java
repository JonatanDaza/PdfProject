package com.example.PruebaPDFBox.repository;

import com.example.PruebaPDFBox.model.DataType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DataTypeRepository extends JpaRepository<DataType, Long> {
    Optional<DataType> findByName(String name);

    boolean existsByNameIgnoreCase(String name);
}
