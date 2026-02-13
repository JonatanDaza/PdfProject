package com.example.PruebaPDFBox.dto.DTOAuth;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleAssignmentDto {

    @NotNull(message = "El ID del usuario es obligatorio")
    private Long userId;

    @NotNull(message = "El ID del rol es obligatorio")
    private Long roleId;
}