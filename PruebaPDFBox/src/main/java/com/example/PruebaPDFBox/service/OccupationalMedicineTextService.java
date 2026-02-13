package com.example.PruebaPDFBox.service;

import com.example.PruebaPDFBox.model.*;
import com.example.PruebaPDFBox.repository.ClinicalHistoryRepository;
import com.example.PruebaPDFBox.repository.DataTypeRepository;
import com.example.PruebaPDFBox.repository.DataValueRepository;
import com.example.PruebaPDFBox.repository.ExamRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class OccupationalMedicineTextService {

    private final DataTypeRepository dataTypeRepository;
    private final DataValueRepository dataValueRepository;
    private final ClinicalHistoryRepository clinicalHistoryRepository;
    private final ExamRepository examRepository;

    // ── Validaciones de existencia ──────────────────────────

    public boolean examAlreadyExists(String examDate, String examName) {
        return examRepository.findByExamDateAndExamName(examDate, examName).isPresent();
    }

    // ── Persistencia de entidades ───────────────────────────

    @Transactional
    public Exam saveExam(String examName, String examDate, Worker worker) {
        ClinicalHistory clinicalHistory = clinicalHistoryRepository
                .findByWorkerAndExamDate(worker, examDate)
                .orElseGet(() -> clinicalHistoryRepository.save(new ClinicalHistory()));

        Exam exam = new Exam();
        exam.setExamName(examName);
        exam.setExamDate(examDate);
        exam.setClinicalHistory(clinicalHistory);
        exam.setWorker(worker);

        return examRepository.save(exam);
    }

    @Transactional
    public void saveDataValue(String typeName, String value, Exam exam) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }

        DataType dataType = dataTypeRepository.findByName(typeName)
                .orElseGet(() -> {
                    DataType newType = new DataType();
                    newType.setName(typeName);
                    return dataTypeRepository.save(newType);
                });

        DataValue dataValue = new DataValue();
        dataValue.setValue(value);
        dataValue.setDataType(dataType);
        dataValue.setExam(exam);

        dataValueRepository.save(dataValue);
    }

    @Transactional
    public void saveAllDataValues(Map<String, String> values, Exam exam) {
        values.forEach((typeName, value) -> saveDataValue(typeName, value, exam));
    }
}
