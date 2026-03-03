package com.example.PruebaPDFBox.utilities.Seeders;

import com.example.PruebaPDFBox.model.DataType;
import com.example.PruebaPDFBox.repository.DataTypeRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataTypeSeeder {

    private final DataTypeRepository dataTypeRepository;

    @PostConstruct
    @Transactional
    public void init() {
        // Verifica si la tabla está vacía
        if (dataTypeRepository.count() == 0) {
            seedDataTypes();
            System.out.println("\n");
        } else {
            System.out.println("\n");
        }
    }

    private void seedDataTypes() {
        List<String> typeNames = Arrays.asList(
                "Tiempo de Exposición", "Actividades Manuales",
                "Ejercicios o Deportes", "Consumidor de Alcohol",
                "Fumador Actual", "Peso", "Frecuencia Respiratoria", "Tensión Arterial",
                "Frecuencia Cardíaca", "Lateralidad Dominante", "Perímetro Abdominal",
                "Talla", "IMC", "Concepto Ocupacional",
                "Código de Examen", "Requisitos de Salud", "Oído derecho",
                "Oído izquierdo", "Nombre del Diagnóstico", "Ponderado Decibelios",
                "Recomendaciones", "Observaciones",
                "Conducta Y/O Recomendaciones", "HEMATOCRITO", "HEMOGLOBINA",
                "RTO.BLANCOS", "RTO.PLAQUETAS", "GLICEMIA", "COLESTEROL TOTAL",
                "COLESTEROL HDL", "COLESTEROL LDL", "TRIGLICERIDOS", "Interpretación"
        );

        typeNames.forEach(this::createTypeIfNotExists);
    }

    private void createTypeIfNotExists(String name) {
        if (!dataTypeRepository.existsByNameIgnoreCase(name)) {
            DataType type = new DataType();
            type.setName(name);
            dataTypeRepository.save(type);
        }
    }
}