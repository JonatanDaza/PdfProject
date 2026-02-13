package com.example.PruebaPDFBox.utilities.mapper;

import com.example.PruebaPDFBox.dto.UsersDto;
import com.example.PruebaPDFBox.model.Users;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;

@Mapper(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, uses = {RoleMap.class})
public interface UsersMap {
    UsersMap INSTANCE = Mappers.getMapper(UsersMap.class);

    @Mapping(source = "roles", target = "roleDtoList")
    UsersDto EntityToDTO(Users users);

    @Mapping(source = "roleDtoList", target = "roles")
    Users DTOToEntity(UsersDto user);

    List<UsersDto> EntityToDTOs(List<Users> users);

    @Mapping(source = "roleDtoList", target = "roles")
    void updateUser(UsersDto usersDto, @MappingTarget Users users);

    default Page<UsersDto> EntityToDTOs(Page<Users> users) {
        List<UsersDto> dtos = EntityToDTOs(users.getContent());
        return new PageImpl<>(dtos, users.getPageable(), users.getTotalElements());
    }
}
