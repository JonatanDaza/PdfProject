package com.example.PruebaPDFBox.utilities.Seeders;

import com.example.PruebaPDFBox.model.DataType;
import com.example.PruebaPDFBox.repository.DataTypeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataTypeSeeder implements CommandLineRunner {

    private final DataTypeRepository dataTypeRepository;

    public DataTypeSeeder(DataTypeRepository dataTypeRepository) {
        this.dataTypeRepository = dataTypeRepository;
    }

    @Override
    public void run(String... args) {
        createTypesNotExists("Tiempo de Exposición");
        createTypesNotExists("Actividades Manuales");
        createTypesNotExists("Ejercicios o Deportes");
        createTypesNotExists("Consumidor de Alcohol");
        createTypesNotExists("Fumador Actual");
        createTypesNotExists("Tensión Arterial");
        createTypesNotExists("Frecuencia Cardíaca");
        createTypesNotExists("Frecuencia Respiratoria");
        createTypesNotExists("Lateralidad Dominante");
        createTypesNotExists("Perímetro Abdominal");
        createTypesNotExists("Peso");
        createTypesNotExists("Talla");
        createTypesNotExists("IMC");
        createTypesNotExists("Diagnósticos");
        createTypesNotExists("Concepto Ocupacional");
        createTypesNotExists("Código de Examen");
        createTypesNotExists("Requisitos de Salud");
        createTypesNotExists("Oído derecho");
        createTypesNotExists("Oído izquierdo");
        createTypesNotExists("Nombre del Diagnóstico");
        createTypesNotExists("Ponderado Decibelios");
        createTypesNotExists("Recomendaciones");
        createTypesNotExists("Observaciones");
        createTypesNotExists("DIAGNÓSTICO");
        createTypesNotExists("Conducta Y/O Recomendaciones");
        createTypesNotExists("HEMATOCRITO");
        createTypesNotExists("HEMOGLOBINA");
        createTypesNotExists("RTO.BLANCOS");
        createTypesNotExists("RTO.PLAQUETAS");
        createTypesNotExists("GLICEMIA");
        createTypesNotExists("COLESTEROL TOTAL");
        createTypesNotExists("COLESTEROL HDL");
        createTypesNotExists("COLESTEROL LDL");
        createTypesNotExists("TRIGLICERIDOS");
        createTypesNotExists("Interpretación");
    }

    public void createTypesNotExists(String name) {
        if (!dataTypeRepository.existsByNameIgnoreCase(name)) {
            DataType type = new DataType();
            type.setName(name);
            dataTypeRepository.save(type);
        }
    }
}
