package com.example.PruebaPDFBox.utilities.mapper;

import com.example.PruebaPDFBox.dto.DataValueDto;
import com.example.PruebaPDFBox.model.DataValue;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;

@Mapper(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface DataValueMap {
        DataValueMap INSTANCE = Mappers.getMapper(DataValueMap.class);

        DataValueDto EntityToDTO(DataValue dataValue);

        DataValue DTOToEntity(DataValueDto dataValue);

        List<DataValueDto> EntityToDTOs(List<DataValue> dataValues);

        void updateDataValue(DataValueDto dataValueDto, @MappingTarget DataValue dataValue);

        default Page<DataValueDto> EntityToDTOs(Page<DataValue> dataValues) {
            List<DataValueDto> dtos = EntityToDTOs(dataValues.getContent());
            return new PageImpl<>(dtos, dataValues.getPageable(), dataValues.getTotalElements());
        }
}
