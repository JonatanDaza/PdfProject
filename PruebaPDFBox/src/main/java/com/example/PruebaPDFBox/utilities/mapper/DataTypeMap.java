package com.example.PruebaPDFBox.utilities.mapper;

import com.example.PruebaPDFBox.dto.DataTypeDto;
import com.example.PruebaPDFBox.model.DataType;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;

@Mapper(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface DataTypeMap {
    DataTypeMap INSTANCE = Mappers.getMapper(DataTypeMap.class);

    DataTypeDto EntityToDTOs(DataType dataType);

    DataType DTOToEntity(DataTypeDto dataType);

    List<DataTypeDto> EntityToDTOs(List<DataType> dataTypes);

    void updateDataType(DataTypeDto dataTypeDto, @MappingTarget DataType dataType);

    default Page<DataTypeDto> EntityToDTOs(Page<DataType> dataTypes) {
        List<DataTypeDto> dtos = EntityToDTOs(dataTypes.getContent());
        return new PageImpl<>(dtos, dataTypes.getPageable(), dataTypes.getTotalElements());
    }
}
