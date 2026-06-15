package org.hibernate.bugs;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "items")
@GenericGenerator(name = "mixed-timing-generator", type = MixedTimingGenerator.class)
public class Item {

    @Id
    @GeneratedValue(generator = "mixed-timing-generator")
    @Column(name = "id")
    private Integer id;

    @Column(name = "name", nullable = false)
    private String name;

    @Version
    @Column(name = "version")
    private Long version;

    public Integer getId()          { return id; }
    public void setId(Integer id)   { this.id = id; }
    public String getName()         { return name; }
    public void setName(String name){ this.name = name; }
    public Long getVersion()        { return version; }
}
