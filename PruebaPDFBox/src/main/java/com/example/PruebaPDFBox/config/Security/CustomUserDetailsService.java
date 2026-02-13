package com.example.PruebaPDFBox.config.Security;

import com.example.PruebaPDFBox.model.Users;
import com.example.PruebaPDFBox.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.UserDetailsService;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UsersRepository usersRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Users users = usersRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));
        return User
                .withUsername(users.getEmail())
                .password(users.getPassword())
                .authorities(users.getRoles().getClass().getName())
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(!users.isActive())
                .build();
    }
}
