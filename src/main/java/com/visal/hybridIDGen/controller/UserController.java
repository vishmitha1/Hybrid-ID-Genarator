package com.visal.hybridIDGen.controller;

import com.visal.hybridIDGen.entity.User;
import com.visal.hybridIDGen.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * REST controller that exposes endpoints for creating and listing {@link User} records.
 *
 * Endpoints:
 *   POST /users  – persist a new user and return it with its generated ID
 *   GET  /users  – return all users currently stored in the database
 */
@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    // -----------------------------------------------------------------------
    // POST /users
    // -----------------------------------------------------------------------

    /**
     * Creates a new user.
     *
     * Request body (JSON):
     * <pre>
     * {
     *   "name":  "Alice",
     *   "email": "alice@example.com"
     * }
     * </pre>
     *
     * @param payload map containing "name" and "email" fields
     * @return 201 Created with the persisted {@link User} (including generated ID)
     */
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody Map<String, String> payload) {
        String name  = payload.get("name");
        String email = payload.get("email");

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
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // -----------------------------------------------------------------------
    // GET /users
    // -----------------------------------------------------------------------

    /**
     * Retrieves all users.
     *
     * @return 200 OK with a JSON array of all {@link User} records
     */
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userRepository.findAll();
        log.info("Returning {} user(s)", users.size());
        return ResponseEntity.ok(users);
    }
}
