package com.example.PruebaPDFBox.repository;

import com.example.PruebaPDFBox.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    boolean existsByNameIgnoreCase(String name);
    Optional<Role> findByNameIgnoreCase(String name);
}
