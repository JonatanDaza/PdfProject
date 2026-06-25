package com.example.PruebaPDFBox.business.Auth;

import com.example.PruebaPDFBox.utilities.config.Security.JwtUtil;
import com.example.PruebaPDFBox.dto.*;
import com.example.PruebaPDFBox.dto.DTOAuth.AuthResponseDto;
import com.example.PruebaPDFBox.dto.DTOAuth.LoginRequestDto;
import com.example.PruebaPDFBox.dto.DTOAuth.RegisterRequestDto;
import com.example.PruebaPDFBox.dto.DTOAuth.RoleAssignmentDto;
import com.example.PruebaPDFBox.model.Role;
import com.example.PruebaPDFBox.model.Users;
import com.example.PruebaPDFBox.service.ServiceEntity.AuthService;
import com.example.PruebaPDFBox.utilities.Infrastructure.exception.CustomException;
import com.example.PruebaPDFBox.utilities.mapper.UsersMap;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AuthBusiness {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    public AuthResponseDto register(RegisterRequestDto request) {
        try {

            Users user = authService.register(request);
            String token = jwtUtil.generateToken(user);

            return buildAuthResponse(user, token);

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("Error al procesar el registro: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public AuthResponseDto login(LoginRequestDto request) {
        try {

            Users user = authService.login(request);
            String token = jwtUtil.generateToken(user);

            return buildAuthResponse(user, token);

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("Error al procesar el login: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public void changePassword(Long userId, String newPassword) {
        try {
            authService.changePassword(userId, newPassword);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("Error al procesar el cambio de contraseña: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public UsersDto activateUser(Long userId) {
        try {

            Users user = authService.activateUser(userId);
            return UsersMap.INSTANCE.EntityToDTO(user);

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("Error al activar usuario: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public UsersDto deactivateUser(Long userId) {
        try {
            Users user = authService.deactivateUser(userId);
            return UsersMap.INSTANCE.EntityToDTO(user);

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("Error al desactivar usuario: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public UsersDto addRole(RoleAssignmentDto request) {
        try {

            Users user = authService.addRole(request);
            return UsersMap.INSTANCE.EntityToDTO(user);

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("Error al agregar rol: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public UsersDto removeRole(RoleAssignmentDto request) {
        try {

            Users user = authService.removeRole(request);
            return UsersMap.INSTANCE.EntityToDTO(user);

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("Error al eliminar rol: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public UsersDto getUserById(Long userId) {
        try {

            Users user = authService.getUserById(userId);
            return UsersMap.INSTANCE.EntityToDTO(user);

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("Error al obtener usuario: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public List<UsersDto> getAllUsers() {
        try {

            List<Users> users = authService.getAllUsers();
            return UsersMap.INSTANCE.EntityToDTOs(users);

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("Error al obtener todos los usuarios: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public List<UsersDto> getActiveUsers() {
        try {

            List<Users> users = authService.getActiveUsers();
            return UsersMap.INSTANCE.EntityToDTOs(users);

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("Error al obtener usuarios activos: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public UsersDto changeRole(Long userId, Long newRoleId) {
        try {
            Users user = authService.changeRole(userId, newRoleId);
            return UsersMap.INSTANCE.EntityToDTO(user);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("Error al cambiar rol: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public List<RoleDto> getAllRoles() {
        try {
            return authService.getAllRoles().stream()
                    .map(role -> new RoleDto(role.getId(), role.getName()))
                    .collect(Collectors.toList());
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("Error al obtener roles: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private AuthResponseDto buildAuthResponse(Users user, String token) {
        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());

        return AuthResponseDto.builder()
                .token(token)
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .roles(roles)
                .active(user.isActive())
                .build();
    }
}