package com.example.PruebaPDFBox.service.ServiceEntity;

import com.example.PruebaPDFBox.model.DataType;
import com.example.PruebaPDFBox.repository.DataTypeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DataTypeService {

    private final DataTypeRepository dataTypeRepository;

    public Optional<DataType> findByName(String name) {
        return dataTypeRepository.findByName(name);
    }

    @Transactional
    public DataType findOrCreate(String name) {
        return dataTypeRepository.findByName(name)
                .orElseGet(() -> {
                    DataType newType = new DataType();
                    newType.setName(name);
                    return dataTypeRepository.save(newType);
                });
    }
}
