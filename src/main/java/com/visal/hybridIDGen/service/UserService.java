package com.visal.hybridIDGen.service;

import com.visal.hybridIDGen.entity.User;
import com.visal.hybridIDGen.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User createUser(String name, String email) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'name' is required");
        }
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'email' is required");
        }
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A user with email '" + email + "' already exists");
        }

        User user = User.builder()
                .name(name.trim())
                .email(email.trim())
                .build();

        User saved = userRepository.save(user);
        log.info("Created user: {}", saved);
        return saved;
    }

    public List<User> getAllUsers() {
        List<User> users = userRepository.findAll();
        log.info("Returning {} user(s)", users.size());
        return users;
    }
}
