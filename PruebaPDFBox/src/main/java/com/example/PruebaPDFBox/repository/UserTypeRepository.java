package com.example.PruebaPDFBox.repository;

import com.example.PruebaPDFBox.model.UserType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserTypeRepository extends JpaRepository<UserType, Long> {
    boolean existsByNameIgnoreCase(String name);
}
