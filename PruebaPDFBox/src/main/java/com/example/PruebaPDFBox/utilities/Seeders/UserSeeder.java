package com.example.PruebaPDFBox.utilities.Seeders;

import java.util.Collections;

import com.example.PruebaPDFBox.model.Users;
import com.example.PruebaPDFBox.repository.UsersRepository;

import jakarta.annotation.PostConstruct;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.annotation.DependsOn;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.PruebaPDFBox.model.Role;
import com.example.PruebaPDFBox.repository.RoleRepository;

@Component
@DependsOn("roleSeeder")
public class UserSeeder {

    private final UsersRepository usersRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserSeeder(UsersRepository usersRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.usersRepository = usersRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    @Transactional
    public void init() {
        if (usersRepository.count() == 0) {
            createDefaultAdminUser();
        }
    }

    private void createDefaultAdminUser() {
        Role adminRole = roleRepository.findByNameIgnoreCase("ADMIN")
                .orElseThrow(() -> new RuntimeException("Role ADMIN not found. Make sure RoleSeeder runs first."));

        Users admin = new Users();
        admin.setName("Administrator");
        admin.setEmail("admin@zonamedicaips.com");
        admin.setPassword(passwordEncoder.encode("AdminZMedication2026!*#"));
        admin.setActive(true);
        admin.setRoles(Collections.singletonList(adminRole));

        usersRepository.save(admin);
    }
}
