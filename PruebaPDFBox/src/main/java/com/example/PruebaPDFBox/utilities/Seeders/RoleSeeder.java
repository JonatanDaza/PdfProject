package com.example.PruebaPDFBox.utilities.Seeders;

import com.example.PruebaPDFBox.model.Role;
import com.example.PruebaPDFBox.repository.RoleRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Component
public class RoleSeeder {

    private final RoleRepository roleRepository;

    public RoleSeeder(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @PostConstruct
    @Transactional
    public void init() {
        if (roleRepository.count() == 0) {
            seedRoles();
        }
    }

    private void seedRoles() {
        List<String> roleNames = Arrays.asList(
                "ADMIN", "USER", "TEST", "DEV"
        );

        roleNames.forEach(this::createRoleIfNotExists);
    }


    private void createRoleIfNotExists(String name) {
        if (!roleRepository.existsByNameIgnoreCase(name)) {
            Role role = new Role();
            role.setName(name);
            roleRepository.save(role);
        }
    }
}
