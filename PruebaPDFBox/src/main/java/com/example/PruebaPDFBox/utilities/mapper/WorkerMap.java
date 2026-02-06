package com.example.PruebaPDFBox.utilities.mapper;

import com.example.PruebaPDFBox.dto.WorkerDto;
import com.example.PruebaPDFBox.model.Worker;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;

@Mapper(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface WorkerMap {
    WorkerMap INSTANCE = Mappers.getMapper(WorkerMap.class);

    WorkerDto EntityToDTO(Worker worker);

    Worker DTOToEntity(WorkerDto worker);

    List<WorkerDto> EntityToDTOs(List<Worker> workers);

    void updateWorker(WorkerDto workerDto, @MappingTarget Worker worker);

    default Page<WorkerDto> EntityToDTOs(Page<Worker> workers) {
        List<WorkerDto> dtos = EntityToDTOs(workers.getContent());
        return new PageImpl<>(dtos, workers.getPageable(), workers.getTotalElements());
    }
}
