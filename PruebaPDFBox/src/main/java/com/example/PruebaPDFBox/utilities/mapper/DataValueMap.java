package com.example.PruebaPDFBox.utilities.mapper;

import com.example.PruebaPDFBox.dto.DataValueDto;
import com.example.PruebaPDFBox.model.DataValue;
import com.example.PruebaPDFBox.utilities.config.MapperConfig.DataValueConverter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;

@Mapper(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, uses = {ExamMap.class, DataValueConverter.class})
public interface DataValueMap {
    DataValueMap INSTANCE = Mappers.getMapper(DataValueMap.class);

    @Mapping(target = "value", source = "value", qualifiedByName = "stringToObject")
    DataValueDto EntityToDTO(DataValue dataValue);

    @Mapping(target = "value", source = "value", qualifiedByName = "objectToString")
    DataValue DTOToEntity(DataValueDto dataValue);

    List<DataValueDto> EntityToDTOs(List<DataValue> dataValues);

    @Mapping(target = "value", source = "value", qualifiedByName = "objectToString")
    void updateDataValue(DataValueDto dataValueDto, @MappingTarget DataValue dataValue);

    default Page<DataValueDto> EntityToDTOs(Page<DataValue> dataValues) {
        List<DataValueDto> dtos = EntityToDTOs(dataValues.getContent());
        return new PageImpl<>(dtos, dataValues.getPageable(), dataValues.getTotalElements());
    }
}
