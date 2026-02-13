package com.example.PruebaPDFBox.utilities.mapper;

import com.example.PruebaPDFBox.dto.RoleDto;
import com.example.PruebaPDFBox.model.Role;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;

@Mapper(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface RoleMap {
    RoleMap INSTANCE = Mappers.getMapper(RoleMap.class);

    RoleDto EntityToDTO(Role role);

    Role DTOToEntity(RoleDto role);

    List<RoleDto> EntityToDTOs(List<Role> roles);

    void updateRole(RoleDto roleDto, @MappingTarget Role role);

    default Page<RoleDto> EntityToDTOs(Page<Role> roles) {
        List<RoleDto> dtos = EntityToDTOs(roles.getContent());
        return new PageImpl<>(dtos, roles.getPageable(), roles.getTotalElements());
    }
}
