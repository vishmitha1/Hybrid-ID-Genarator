package com.visal.hybridIDGen.entity;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.id.IdentityGenerator;

import java.util.EnumSet;

import static java.util.Objects.isNull;
import static org.hibernate.generator.EventTypeSets.INSERT_ONLY;

public class CustomIdentityGenerator extends IdentityGenerator implements BeforeExecutionGenerator {

    @Override
    public Object generate(SharedSessionContractImplementor session, Object entity, Object currentValue, EventType eventType) {
        return getId(entity, session);
    }

    private static Object getId(Object entity, SharedSessionContractImplementor session) {
        return session.getEntityPersister(null, entity)
                .getIdentifier(entity, session);
    }


    @Override
    public boolean generatedOnExecution(Object entity, SharedSessionContractImplementor session) {
        Object id = getId(entity, session);
        return isNull(id);
    }

    @Override
    public boolean generatedOnExecution() {
        return true;
    }

    @Override
    public EnumSet<EventType> getEventTypes() {
        return INSERT_ONLY;
    }
}
