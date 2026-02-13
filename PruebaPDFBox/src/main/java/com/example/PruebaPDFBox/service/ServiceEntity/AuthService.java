package com.example.PruebaPDFBox.service.ServiceEntity;

import com.example.PruebaPDFBox.dto.DTOAuth.LoginRequestDto;
import com.example.PruebaPDFBox.dto.DTOAuth.RegisterRequestDto;
import com.example.PruebaPDFBox.dto.DTOAuth.RoleAssignmentDto;
import com.example.PruebaPDFBox.model.Role;
import com.example.PruebaPDFBox.model.Users;
import com.example.PruebaPDFBox.repository.RoleRepository;
import com.example.PruebaPDFBox.repository.UsersRepository;
import com.example.PruebaPDFBox.utilities.Infrastructure.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsersRepository usersRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Users register(RegisterRequestDto request) {
        try {

            // Verificar si el usuario ya existe
            if (usersRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new UserAlreadyExistsException("El email " + request.getEmail() + " ya está registrado");
            }

//            // Verificar si el nombre de usuario ya existe
//            if (usersRepository.findByname(request.getName()).isPresent()) {
//                throw new UserAlreadyExistsException("El nombre de usuario " + request.getName() + " ya está en uso");
//            }

            // Crear nuevo usuario
            Users user = new Users();
            user.setName(request.getName());
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setActive(true);

            // Asignar roles por defecto (USER) si no se especifican roles
            if (request.getRoles() == null || request.getRoles().isEmpty()) {
                Role defaultRole = roleRepository.findByNameIgnoreCase("USER")
                        .orElseThrow(() -> new RoleNotFoundException("Rol USER no encontrado en la base de datos"));
                user.setRoles(List.of(defaultRole));
            } else {
                // Asignar roles especificados
                List<Role> roles = request.getRoles().stream()
                        .map(roleName -> roleRepository.findByNameIgnoreCase(roleName)
                                .orElseThrow(() -> new RoleNotFoundException("Rol " + roleName + " no encontrado en la base de datos")))
                        .collect(Collectors.toList());
                user.setRoles(roles);
            }

            return usersRepository.save(user);

        } catch (UserAlreadyExistsException e) {
            throw new CustomException(e.getMessage(), HttpStatus.CONFLICT);
        } catch (RoleNotFoundException e) {
            throw new CustomException(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (DataAccessException e) {
            throw new CustomException("Error en la base de datos al registrar usuario: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            throw new CustomException("Error inesperado al registrar usuario: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public Users login(LoginRequestDto request) {
        try {

            Users user = usersRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> {
                        return new InvalidCredentialsException("Credenciales inválidas");
                    });

            // Verificar si el usuario está activo
            if (!user.isActive()) {
                throw new UserInactiveException("Usuario desactivado. Contacte al administrador.");
            }

            // Verificar contraseña
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new InvalidCredentialsException("Credenciales inválidas");
            }

            return user;

        } catch (InvalidCredentialsException e) {
            throw new CustomException(e.getMessage(), HttpStatus.UNAUTHORIZED);
        } catch (UserInactiveException e) {
            throw new CustomException(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (DataAccessException e) {
            throw new CustomException("Error en la base de datos al procesar login: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            throw new CustomException("Error inesperado al procesar login: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public Users activateUser(Long userId) {
        try {

            Users user = usersRepository.findById(userId)
                    .orElseThrow(() -> {
                        return new UserNotFoundException("Usuario no encontrado con ID: " + userId);
                    });

            if (user.isActive()) {
                return user;
            }

            user.setActive(true);
            return usersRepository.save(user);

        } catch (UserNotFoundException e) {
            throw new CustomException(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (DataAccessException e) {
            throw new CustomException("Error en la base de datos al activar usuario: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            throw new CustomException("Error inesperado al activar usuario: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public Users deactivateUser(Long userId) {
        try {

            Users user = usersRepository.findById(userId)
                    .orElseThrow(() -> {
                        return new UserNotFoundException("Usuario no encontrado con ID: " + userId);
                    });

            if (!user.isActive()) {
                return user;
            }

            user.setActive(false);
            return usersRepository.save(user);

        } catch (UserNotFoundException e) {
            throw new CustomException(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (DataAccessException e) {
            throw new CustomException("Error en la base de datos al desactivar usuario: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            throw new CustomException("Error inesperado al desactivar usuario: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public Users addRole(RoleAssignmentDto request) {
        try {

            Users user = usersRepository.findById(request.getUserId())
                    .orElseThrow(() -> {
                        return new UserNotFoundException("Usuario no encontrado con ID: " + request.getUserId());
                    });

            Role role = roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> {
                        return new RoleNotFoundException("Rol no encontrado con ID: " + request.getRoleId());
                    });

            // Verificar si el usuario ya tiene el rol
            if (user.getRoles().stream().anyMatch(r -> r.getId().equals(role.getId()))) {
                return user;
            }

            user.getRoles().add(role);
            return usersRepository.save(user);

        } catch (UserNotFoundException | RoleNotFoundException e) {
            throw new CustomException(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (DataAccessException e) {
            throw new CustomException("Error en la base de datos al agregar rol: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            throw new CustomException("Error inesperado al agregar rol: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public Users removeRole(RoleAssignmentDto request) {
        try {

            Users user = usersRepository.findById(request.getUserId())
                    .orElseThrow(() -> {
                        return new UserNotFoundException("Usuario no encontrado con ID: " + request.getUserId());
                    });

            Role role = roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> {
                        return new RoleNotFoundException("Rol no encontrado con ID: " + request.getRoleId());
                    });

            // Verificar que el usuario tenga al menos un rol después de eliminar
            if (user.getRoles().size() <= 1 && user.getRoles().contains(role)) {
                throw new IllegalStateException("El usuario debe tener al menos un rol");
            }

            boolean removed = user.getRoles().removeIf(r -> r.getId().equals(role.getId()));

            if (!removed) {
                return user;
            }

            return usersRepository.save(user);

        } catch (UserNotFoundException | RoleNotFoundException e) {
            throw new CustomException(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (IllegalStateException e) {
            throw new CustomException(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (DataAccessException e) {
            throw new CustomException("Error en la base de datos al eliminar rol: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            throw new CustomException("Error inesperado al eliminar rol: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public Users getUserById(Long userId) {
        try {
            return usersRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con ID: " + userId));

        } catch (UserNotFoundException e) {
            throw new CustomException(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (DataAccessException e) {
            throw new CustomException("Error en la base de datos al obtener usuario: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            throw new CustomException("Error inesperado al obtener usuario: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public List<Users> getAllUsers() {
        try {
            return usersRepository.findAll();

        } catch (DataAccessException e) {
            throw new CustomException("Error en la base de datos al obtener usuarios: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            throw new CustomException("Error inesperado al obtener usuarios: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public List<Users> getActiveUsers() {
        try {
            return usersRepository.findAll().stream()
                    .filter(Users::isActive)
                    .collect(Collectors.toList());

        } catch (DataAccessException e) {
            throw new CustomException("Error en la base de datos al obtener usuarios activos: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            throw new CustomException("Error inesperado al obtener usuarios activos: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}