package com.example.PruebaPDFBox.service.ServiceEntity;


import com.example.PruebaPDFBox.model.Worker;
import com.example.PruebaPDFBox.repository.WorkerRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WorkerService {

    private final WorkerRepository workerRepository;

    public Worker save(Worker worker) {
        return workerRepository.save(worker);
    }

    public Worker findOrCreate(Worker worker) {
        return workerRepository.findByCC(worker.getCC())
                .orElseGet(() -> workerRepository.save(worker));
    }

    @Transactional
    public Worker buildFromExtractedData(Map<String, String> extractedData, String cc) {
        Worker worker = new Worker();
        worker.setCC(cc);
        worker.setName(extractedData.get("Nombre del Trabajador a Aspirante:"));
        worker.setGender(extractedData.get("Genero:"));
        worker.setAge(extractedData.get("Edad:"));
        worker.setMaritalStatus(extractedData.get("Estado Civil:"));
        worker.setEducationalLevel(extractedData.get("Nivel Educativo:"));

        return worker;
    }

    public boolean existsByCC(String cc) {
        return workerRepository.findByCC(cc).isPresent();
    }
}