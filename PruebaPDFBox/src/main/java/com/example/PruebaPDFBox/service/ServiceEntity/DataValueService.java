package com.example.PruebaPDFBox.service.ServiceEntity;

import com.example.PruebaPDFBox.model.DataType;
import com.example.PruebaPDFBox.model.DataValue;
import com.example.PruebaPDFBox.model.Exam;
import com.example.PruebaPDFBox.repository.DataTypeRepository;
import com.example.PruebaPDFBox.repository.DataValueRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class DataValueService {

    private final DataValueRepository dataValueRepository;
    private final DataTypeService dataTypeService;

    @Transactional
    public void save(String typeName, String value, Exam exam) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        DataType dataType = dataTypeService.findOrCreate(typeName);

        DataValue dataValue = new DataValue();
        dataValue.setValue(value);
        dataValue.setDataType(dataType);
        dataValue.setExam(exam);

        dataValueRepository.save(dataValue);
    }

    @Transactional
    public void saveAll(Map<String, String> values, Exam exam) {
        values.forEach((typeName, value) -> save(typeName, value, exam));
    }
}
