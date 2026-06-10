# persist() of a new entity with an assigned id fails on 6.6 (worked on 6.4)

**Affects:** 6.6.49.Final (worked on 6.4.10.Final)

## Description

I have an identity-or-assigned id generator that extends `IdentityGenerator`: if the
entity already has an id, use it; otherwise let the DB generate one.

```java
public class CustomIdentityGenerator extends IdentityGenerator implements BeforeExecutionGenerator {

    @Override
    public Object generate(SharedSessionContractImplementor session, Object entity,
                           Object currentValue, EventType eventType) {
        return session.getEntityPersister(null, entity).getIdentifier(entity, session);
    }

    @Override
    public boolean generatedOnExecution(Object entity, SharedSessionContractImplementor session) {
        return session.getEntityPersister(null, entity).getIdentifier(entity, session) == null;
    }

    @Override
    public boolean generatedOnExecution() {
        return true;
    }
}
```

The entity has an `@Version` column:

```java
@Entity
public class Item {
    @Id
    @GeneratedValue(generator = "g")
    @GenericGenerator(name = "g", type = CustomIdentityGenerator.class)
    private Integer id;

    private String name;

    @Version
    private Long version;
}
```

Persisting **without** an id works (DB generates it). Persisting a **new** entity with an
**assigned** id worked on 6.4.10 but fails on 6.6.49:

```java
Item item = new Item();
item.setId(987654);
item.setName("manual-item");
entityManager.persist(item);
```

## Expected

The row is inserted with id 987654 (as on 6.4.10).

## Actual (6.6.49)

```
org.hibernate.PropertyValueException: Detached entity with generated id '987654'
    has an uninitialized version value 'null': Item.version
    at org.hibernate.persister.entity.AbstractEntityPersister.isTransient(AbstractEntityPersister.java:4320)
    at org.hibernate.engine.internal.ForeignKeys.isTransient(ForeignKeys.java:316)
    at org.hibernate.event.internal.EntityState.getEntityState(EntityState.java:64)
    at org.hibernate.event.internal.DefaultPersistEventListener.persist(DefaultPersistEventListener.java:87)
```

`isTransient()` treats the assigned, non-null id as a detached row, so a brand-new
versioned entity with an assigned id can no longer be persisted.

## Environment

Hibernate 6.6.49.Final, JDK 21, PostgreSQL (DB-independent — fails before any SQL).

## Question

Is this intentional in 6.6? If so, what is the supported way to persist a new `@Version`
entity with an application-assigned id under an `IdentityGenerator`-based generator?
