package com.example.PruebaPDFBox.utilities.Seeders;

import com.example.PruebaPDFBox.model.UserType;
import com.example.PruebaPDFBox.repository.DataTypeRepository;
import com.example.PruebaPDFBox.repository.UserTypeRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Component
public class UserTypeSeeder {

    private final UserTypeRepository userTypeRepository;
    private final DataTypeRepository dataTypeRepository;

    public UserTypeSeeder(UserTypeRepository userTypeRepository, DataTypeRepository dataTypeRepository) {
        this.userTypeRepository = userTypeRepository;
        this.dataTypeRepository = dataTypeRepository;
    }

    @PostConstruct
    @Transactional
    public void init() {
        if (dataTypeRepository.count() == 0) {
            seedDataTypes();
        }
    }

    private void seedDataTypes() {
        List<String> typeNames = Arrays.asList(
                "Admin", "User", "Test"
        );

        typeNames.forEach(this::createTypeIfNotExists);
    }


    private void createTypeIfNotExists(String name) {
        if (!userTypeRepository.existsByNameIgnoreCase(name)) {
            UserType type = new UserType();
            type.setName(name);
            userTypeRepository.save(type);
        }
    }
}
