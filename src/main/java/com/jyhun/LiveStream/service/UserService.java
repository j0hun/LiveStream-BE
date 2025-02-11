package com.jyhun.LiveStream.service;

import com.jyhun.LiveStream.dto.RegisterDTO;
import com.jyhun.LiveStream.entity.User;
import com.jyhun.LiveStream.enums.Role;
import com.jyhun.LiveStream.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Void register(RegisterDTO registerDTO) {
        User user = User.builder()
                .username(registerDTO.getUsername())
                .email(registerDTO.getEmail())
                .password(passwordEncoder.encode(registerDTO.getPassword()))
                .role(Role.USER)
                .build();

        userRepository.save(user);

        return null;
    }

}
