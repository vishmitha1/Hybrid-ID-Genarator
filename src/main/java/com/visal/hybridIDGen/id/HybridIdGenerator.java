package com.visal.hybridIDGen.id;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Custom Hibernate ID generator that produces human-readable, prefixed IDs.
 *
 * Format: PREFIX-YYYYMMDD-SEQUENCE
 * Example: USR-20240607-00001
 *
 * Thread-safe via AtomicLong sequence counter. The sequence resets each day,
 * so the date segment acts as a natural partition boundary.
 */
@Component
public class HybridIdGenerator implements IdentifierGenerator {

    /** Parameter name used in the @Parameter annotation on the entity. */
    public static final String PREFIX_PARAM = "prefix";

    /** Default prefix when none is supplied by the entity mapping. */
    private static final String DEFAULT_PREFIX = "ID";

    /** Date format embedded in every generated ID. */
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    /** Sequence counter — shared across all calls within the same JVM day. */
    private static final AtomicLong SEQUENCE = new AtomicLong(0L);

    /** Tracks which date the current sequence is valid for. */
    private static volatile String currentDate = LocalDate.now().format(DATE_FORMATTER);

    /** Lock object used only for the lightweight date-rollover check. */
    private static final Object DATE_LOCK = new Object();

    // -----------------------------------------------------------------------
    // IdentifierGenerator contract
    // -----------------------------------------------------------------------

    /**
     * Called by Hibernate whenever a new identifier is needed.
     *
     * @param session  the current Hibernate session
     * @param object   the entity instance being persisted
     * @return         a unique String identifier
     */
    @Override
    public Object generate(SharedSessionContractImplementor session, Object object) {
        // Retrieve the prefix that was configured on the entity (or use the default).
        String prefix = resolvePrefix(session);
        String dateSegment = getDateSegment();
        long sequence = nextSequence(dateSegment);
        return String.format("%s-%s-%05d", prefix, dateSegment, sequence);
    }

    // -----------------------------------------------------------------------
    // Helper methods
    // -----------------------------------------------------------------------

    /**
     * Reads the "prefix" parameter from the generator configuration attached
     * to the entity mapping.  Falls back to {@link #DEFAULT_PREFIX} when the
     * parameter is absent or blank.
     */
    private String resolvePrefix(SharedSessionContractImplementor session) {
        if (session == null) {
            return DEFAULT_PREFIX;
        }
        // The generator configuration is accessible through the persister.
        // We store it via configure(), which Hibernate calls before generate().
        return storedPrefix != null && !storedPrefix.isBlank()
                ? storedPrefix
                : DEFAULT_PREFIX;
    }

    /**
     * Prefix value stored during {@link #configure(org.hibernate.type.Type, Properties, org.hibernate.service.ServiceRegistry)}.
     * Volatile so visibility is guaranteed across threads.
     */
    private volatile String storedPrefix = DEFAULT_PREFIX;

    /**
     * Hibernate calls this method once per generator instantiation so we can
     * read the parameters defined in the entity annotation.
     */
    @Override
    public void configure(org.hibernate.type.Type type,
                          Properties parameters,
                          org.hibernate.service.ServiceRegistry serviceRegistry) {
        String prefix = parameters.getProperty(PREFIX_PARAM, DEFAULT_PREFIX);
        this.storedPrefix = (prefix != null && !prefix.isBlank()) ? prefix.trim() : DEFAULT_PREFIX;
    }

    /**
     * Returns today's date formatted as {@code yyyyMMdd}.
     * Also triggers a sequence reset if the date has rolled over.
     */
    private static String getDateSegment() {
        String today = LocalDate.now().format(DATE_FORMATTER);
        if (!today.equals(currentDate)) {
            synchronized (DATE_LOCK) {
                // Double-checked locking — re-test inside the synchronized block.
                if (!today.equals(currentDate)) {
                    currentDate = today;
                    SEQUENCE.set(0L);
                }
            }
        }
        return today;
    }

    /**
     * Atomically increments and returns the next sequence number for
     * {@code dateSegment}.  The date parameter is accepted here (rather than
     * re-computing it) to keep the date snapshot consistent within a single
     * generate() call.
     */
    private static long nextSequence(String dateSegment) {
        return SEQUENCE.incrementAndGet();
    }
}
