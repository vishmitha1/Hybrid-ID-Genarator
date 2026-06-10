package com.visal.hybridIDGen.entity;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

import java.util.EnumSet;
import java.util.Properties;

import static org.hibernate.generator.EventTypeSets.INSERT_ONLY;

public  class IdentityOrAssignedGenerator extends IdentityGenerator implements IdentifierGenerator {
    @Override
    public Object generate(SharedSessionContractImplementor session, Object object) {
        final EntityPersister entityPersister = session.getEntityPersister( null, object );
        return entityPersister.getIdentifier( object, session );
    }

    @Override
    public boolean generatedOnExecution() {
        return true;
    }

    @Override
    public boolean generatedOnExecution(Object owner, SharedSessionContractImplementor session) {
        return generate( session, owner, null, null ) == null;
    }

    @Override
    public EnumSet<EventType> getEventTypes() {
        return INSERT_ONLY;
    }

    @Override
    public void configure(Type type, Properties params, ServiceRegistry serviceRegistry) {
    }
}
