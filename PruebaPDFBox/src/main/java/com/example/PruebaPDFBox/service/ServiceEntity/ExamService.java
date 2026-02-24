package com.example.PruebaPDFBox.service.ServiceEntity;

import com.example.PruebaPDFBox.model.ClinicalHistory;
import com.example.PruebaPDFBox.model.Exam;
import com.example.PruebaPDFBox.model.Worker;
import com.example.PruebaPDFBox.repository.ExamRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamRepository examRepository;

    public boolean examAlreadyExists(String examDate, String examName) {
        return examRepository.findByExamDateAndExamName(examDate, examName).isPresent();
    }

    @Transactional
    public Exam save(String examName, String examDate, Worker worker, ClinicalHistory clinicalHistory) {
        Exam exam = new Exam();
        exam.setExamName(examName);
        exam.setExamDate(examDate);
        exam.setClinicalHistory(clinicalHistory);
        exam.setWorker(worker);
        return examRepository.save(exam);
    }

    @Transactional
    public Exam save(Exam exam) {
        return examRepository.save(exam);
    }
}
