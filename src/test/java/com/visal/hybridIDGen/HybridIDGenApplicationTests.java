package com.visal.hybridIDGen;

import com.visal.hybridIDGen.entity.User;
import com.visal.hybridIDGen.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify the HybridIdGenerator produces correctly
 * formatted IDs when entities are persisted through the full Spring/Hibernate
 * stack.
 */
@SpringBootTest
@Transactional
class HybridIDGenApplicationTests {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Application context loads successfully")
    void contextLoads() {
        // If the context fails to start the test will fail automatically.
    }

    @Test
    @DisplayName("Generated ID matches PREFIX-YYYYMMDD-NNNNN pattern")
    void generatedIdMatchesExpectedPattern() {
        User user = User.builder()
                .name("Alice")
                .email("alice@example.com")
                .build();

        User saved = userRepository.save(user);

        assertThat(saved.getId())
                .isNotNull()
                .matches("USR-\\d{8}-\\d{5}"
                );
    }

    @Test
    @DisplayName("Each saved entity receives a unique ID")
    void eachEntityReceivesUniqueId() {
        User u1 = userRepository.save(User.builder().name("Bob").email("bob@example.com").build());
        User u2 = userRepository.save(User.builder().name("Carol").email("carol@example.com").build());
        User u3 = userRepository.save(User.builder().name("Dave").email("dave@example.com").build());

        List<String> ids = List.of(u1.getId(), u2.getId(), u3.getId());

        assertThat(ids).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("Sequence portion increments monotonically within a single day")
    void sequenceIsMonotonicallyIncreasing() {
        User u1 = userRepository.save(User.builder().name("Eve").email("eve@example.com").build());
        User u2 = userRepository.save(User.builder().name("Frank").email("frank@example.com").build());

        long seq1 = extractSequence(u1.getId());
        long seq2 = extractSequence(u2.getId());

        assertThat(seq2).isGreaterThan(seq1);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Parses the numeric suffix from an ID like "USR-20240607-00003".
     */
    private long extractSequence(String id) {
        String[] parts = id.split("-");
        assertThat(parts).hasSize(3);
        return Long.parseLong(parts[2]);
    }
}
