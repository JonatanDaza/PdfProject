package com.example.PruebaPDFBox.dto;

import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {

    private Long id;
    private String name;
    private String email;
    private String password;
    private boolean active;
    private UserTypeDto userType;
}
