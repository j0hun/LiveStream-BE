package com.jyhun.LiveStream.service;

import com.jyhun.LiveStream.dto.AuthDTO;
import com.jyhun.LiveStream.dto.LoginDTO;
import com.jyhun.LiveStream.dto.RegisterDTO;
import com.jyhun.LiveStream.entity.User;
import com.jyhun.LiveStream.enums.Role;
import com.jyhun.LiveStream.repository.UserRepository;
import com.jyhun.LiveStream.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

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

    public AuthDTO login(LoginDTO loginDTO) {
        User user = userRepository.findByEmail(loginDTO.getEmail()).orElse(null);
        if(!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new RuntimeException("패스워드가 맞지 않습니다.");
        }
        String token = jwtService.generateToken(user);
        return new AuthDTO(token, user.getRole().name());
    }

    @Transactional(readOnly = true)
    public User getLoggedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email).orElse(null);
    }

}
