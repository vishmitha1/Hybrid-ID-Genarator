package com.visal.hybridIDGen.entity;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PrePersist;
import org.springframework.stereotype.Component;

@Component
public class ItemIdListener {
    /*
    This id listener is a workaround to allow auto-generating IDs for items created with a null id,
    while still allowing clients to specify their own IDs when desired.
    Need to configure own sequence 'items_id_seq' in the database for this to work,
    and ensure that manually specified IDs do not conflict with the sequence-generated ones.

    @EntityListeners(ItemIdListener.class) must be added to the Item entity for this listener to be invoked on persist.
     */

    private static ItemIdListener INSTANCE;

    @PersistenceContext
    private EntityManager em;

    @PostConstruct
    public void init() {
        INSTANCE = this;
    }

    @PrePersist
    public void prePersist(Item item) {
        if (item.getId() == null && INSTANCE != null) {
            Integer nextId = ((Number) INSTANCE.em
                    .createNativeQuery("SELECT nextval('items_id_seq')")
                    .getSingleResult())
                    .intValue();
            item.setId(nextId);
        }
        // id already set → use the caller-supplied value as-is
    }
}
