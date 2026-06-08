package com.visal.hybridIDGen.entity;

import com.visal.hybridIDGen.id.HybridIdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

/**
 * JPA entity that uses {@link HybridIdGenerator} to produce human-readable IDs.
 *
 * The "prefix" parameter is passed to the generator via @Parameter so that
 * each entity type can define its own prefix segment (e.g. "USR" for users).
 *
 * Generated ID example: USR-20240607-00001
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@GenericGenerator(
        name = "hybrid-id-generator",
        type = HybridIdGenerator.class,
        parameters = {
                @Parameter(name = HybridIdGenerator.PREFIX_PARAM, value = "USR")
        }
)
public class User {

    @Id
    @GeneratedValue(generator = "hybrid-id-generator")
    @Column(name = "id", updatable = false, nullable = false, length = 30)
    private String id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;
}
