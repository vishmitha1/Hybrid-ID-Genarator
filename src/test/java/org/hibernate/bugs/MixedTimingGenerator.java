package org.hibernate.bugs;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.id.IdentityGenerator;

import static java.util.Objects.isNull;

/**
 * Mixed-timing generator: delegates to DB IDENTITY (SERIAL) when id is null,
 * or uses the caller-supplied id when one is already set.
 *
 * generatedOnExecution() (no-arg / class-level) returns true so Hibernate
 * registers it as a mixed-timing generator that can switch per entity instance.
 *
 * generatedOnExecution(entity, session) (instance-level) returns:
 *   true  → id is null  → let the DB generate via IDENTITY column
 *   false → id is set   → call generate(), which returns the existing id
 *
 * This worked correctly in Hibernate 6.4.x.
 * In Hibernate 6.6.x it throws PropertyValueException on persist() when
 * id is manually set and @Version is present:
 *   "Detached entity with generated id '<id>' has an uninitialized version value 'null'"
 */
public class MixedTimingGenerator extends IdentityGenerator implements BeforeExecutionGenerator {

    @Override
    public Object generate(SharedSessionContractImplementor session, Object entity,
                           Object currentValue, EventType eventType) {
        return session.getEntityPersister(null, entity).getIdentifier(entity, session);
    }

    @Override
    public boolean generatedOnExecution(Object entity, SharedSessionContractImplementor session) {
        Object id = session.getEntityPersister(null, entity).getIdentifier(entity, session);
        return isNull(id);
    }

    @Override
    public boolean generatedOnExecution() {
        return true;
    }
}
