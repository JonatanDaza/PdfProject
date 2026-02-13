package com.example.PruebaPDFBox.dto;

import lombok.*;

import java.util.List;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UsersDto {

    private Long id;
    private String name;
    private String email;
    private String password;
    private boolean active;
    private List<RoleDto> roleDtoList;
}
