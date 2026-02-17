package com.example.PruebaPDFBox.controller;

import com.example.PruebaPDFBox.business.Auth.AuthBusiness;
import com.example.PruebaPDFBox.dto.DTOAuth.AuthResponseDto;
import com.example.PruebaPDFBox.dto.DTOAuth.LoginRequestDto;
import com.example.PruebaPDFBox.dto.DTOAuth.RegisterRequestDto;
import com.example.PruebaPDFBox.dto.DTOAuth.RoleAssignmentDto;
import com.example.PruebaPDFBox.dto.UsersDto;
import com.example.PruebaPDFBox.utilities.Infrastructure.exception.CustomException;
import com.example.PruebaPDFBox.utilities.Infrastructure.http.ResponseHttpApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthBusiness authBusiness;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequestDto request) {
        try {
            AuthResponseDto response = authBusiness.register(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseHttpApi.responseHttpFindId(
                            response,
                            ResponseHttpApi.CODE_OK,
                            "Usuario registrado exitosamente"
                    ));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(ResponseHttpApi.responseHttpError(e.getMessage(), e.getStatus()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseHttpApi.responseHttpError(
                            "Error interno del servidor",
                            HttpStatus.INTERNAL_SERVER_ERROR
                    ));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDto request) {
        try {
            AuthResponseDto response = authBusiness.login(request);
            return ResponseEntity.ok()
                    .body(ResponseHttpApi.responseHttpFindId(
                            response,
                            ResponseHttpApi.CODE_OK,
                            "Login exitoso"
                    ));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(ResponseHttpApi.responseHttpError(e.getMessage(), e.getStatus()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseHttpApi.responseHttpError(
                            "Error interno del servidor",
                            HttpStatus.INTERNAL_SERVER_ERROR
                    ));
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String token) {
        try {
            // Implementar lógica de refresh token si es necesario
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                    .body(ResponseHttpApi.responseHttpError(
                            "Funcionalidad no implementada",
                            HttpStatus.NOT_IMPLEMENTED
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseHttpApi.responseHttpError(
                            "Error interno del servidor",
                            HttpStatus.INTERNAL_SERVER_ERROR
                    ));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String token) {
        try {
            // Aquí podrías invalidar el token en un blacklist si lo deseas
            return ResponseEntity.ok()
                    .body(ResponseHttpApi.responseHttpAction(
                            null,
                            ResponseHttpApi.CODE_OK,
                            "Sesión cerrada exitosamente"
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseHttpApi.responseHttpError(
                            "Error interno del servidor",
                            HttpStatus.INTERNAL_SERVER_ERROR
                    ));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/users/{userId}/activate")
    public ResponseEntity<?> activateUser(@PathVariable Long userId) {
        try {
            UsersDto response = authBusiness.activateUser(userId);
            return ResponseEntity.ok()
                    .body(ResponseHttpApi.responseHttpAction(
                            response.getId(),
                            ResponseHttpApi.CODE_OK,
                            "Usuario activado exitosamente"
                    ));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(ResponseHttpApi.responseHttpError(e.getMessage(), e.getStatus()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseHttpApi.responseHttpError(
                            "Error interno del servidor",
                            HttpStatus.INTERNAL_SERVER_ERROR
                    ));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/users/{userId}/deactivate")
    public ResponseEntity<?> deactivateUser(@PathVariable Long userId) {
        try {
            UsersDto response = authBusiness.deactivateUser(userId);
            return ResponseEntity.ok()
                    .body(ResponseHttpApi.responseHttpAction(
                            response.getId(),
                            ResponseHttpApi.CODE_OK,
                            "Usuario desactivado exitosamente"
                    ));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(ResponseHttpApi.responseHttpError(e.getMessage(), e.getStatus()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseHttpApi.responseHttpError(
                            "Error interno del servidor",
                            HttpStatus.INTERNAL_SERVER_ERROR
                    ));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users/roles/add")
    public ResponseEntity<?> addRole(@Valid @RequestBody RoleAssignmentDto request) {
        try {
            UsersDto response = authBusiness.addRole(request);
            return ResponseEntity.ok()
                    .body(ResponseHttpApi.responseHttpAction(
                            response.getId(),
                            ResponseHttpApi.CODE_OK,
                            "Rol agregado exitosamente"
                    ));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(ResponseHttpApi.responseHttpError(e.getMessage(), e.getStatus()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseHttpApi.responseHttpError(
                            "Error interno del servidor",
                            HttpStatus.INTERNAL_SERVER_ERROR
                    ));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users/roles/remove")
    public ResponseEntity<?> removeRole(@Valid @RequestBody RoleAssignmentDto request) {
        try {
            UsersDto response = authBusiness.removeRole(request);
            return ResponseEntity.ok()
                    .body(ResponseHttpApi.responseHttpAction(
                            response.getId(),
                            ResponseHttpApi.CODE_OK,
                            "Rol eliminado exitosamente"
                    ));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(ResponseHttpApi.responseHttpError(e.getMessage(), e.getStatus()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseHttpApi.responseHttpError(
                            "Error interno del servidor",
                            HttpStatus.INTERNAL_SERVER_ERROR
                    ));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getUserById(@PathVariable Long userId) {
        try {
            UsersDto response = authBusiness.getUserById(userId);
            return ResponseEntity.ok()
                    .body(ResponseHttpApi.responseHttpFindId(
                            response,
                            ResponseHttpApi.CODE_OK,
                            "Usuario encontrado"
                    ));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(ResponseHttpApi.responseHttpError(e.getMessage(), e.getStatus()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseHttpApi.responseHttpError(
                            "Error interno del servidor",
                            HttpStatus.INTERNAL_SERVER_ERROR
                    ));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        try {
            List<UsersDto> response = authBusiness.getAllUsers();
            return ResponseEntity.ok()
                    .body(ResponseHttpApi.responseHttpFindAllList(
                            response,
                            ResponseHttpApi.CODE_OK,
                            "Usuarios obtenidos exitosamente",
                            response.size()
                    ));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(ResponseHttpApi.responseHttpError(e.getMessage(), e.getStatus()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseHttpApi.responseHttpError(
                            "Error interno del servidor",
                            HttpStatus.INTERNAL_SERVER_ERROR
                    ));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/active")
    public ResponseEntity<?> getActiveUsers() {
        try {
            List<UsersDto> response = authBusiness.getActiveUsers();
            return ResponseEntity.ok()
                    .body(ResponseHttpApi.responseHttpFindAllList(
                            response,
                            ResponseHttpApi.CODE_OK,
                            "Usuarios activos obtenidos exitosamente",
                            response.size()
                    ));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(ResponseHttpApi.responseHttpError(e.getMessage(), e.getStatus()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseHttpApi.responseHttpError(
                            "Error interno del servidor",
                            HttpStatus.INTERNAL_SERVER_ERROR
                    ));
        }
    }
}