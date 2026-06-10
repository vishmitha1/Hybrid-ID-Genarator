package com.visal.hybridIDGen.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
// @GenericGenerator(name = "custom-identity-generator", type = CustomIdentityGenerator.class)
@GenericGenerator(name = "identity-or-assigned-generator", type = IdentityOrAssignedGenerator.class)
public class Item {

    @Id
    // @GeneratedValue(generator = "custom-identity-generator")
    @GeneratedValue(generator = "identity-or-assigned-generator")
    @Column(name = "id")
    private Integer id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Version
    @Column(name = "version")
    private Long version;
}
