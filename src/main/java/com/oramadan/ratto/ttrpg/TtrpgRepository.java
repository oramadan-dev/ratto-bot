package com.oramadan.ratto.ttrpg;

import com.oramadan.ratto.persistence.JpaManager;
import jakarta.persistence.EntityManager;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class TtrpgRepository {

    public TtrpgEventDetails saveEvent(long guildId, long gmUserId, String name, Instant scheduledAt, boolean recurringWeekly, List<Long> playerIds) {
        EntityManager entityManager = JpaManager.createEntityManager();
        try {
            entityManager.getTransaction().begin();

            TtrpgEventEntity eventEntity = new TtrpgEventEntity(null, guildId, gmUserId, name, scheduledAt, recurringWeekly);
            entityManager.persist(eventEntity);

            for (Long playerId : playerIds) {
                entityManager.persist(new TtrpgEventPlayerEntity(eventEntity.getId(), playerId));
            }

            entityManager.getTransaction().commit();
            return new TtrpgEventDetails(
                    eventEntity.getId(),
                    eventEntity.getGuildId(),
                    eventEntity.getGmUserId(),
                    eventEntity.getName(),
                    eventEntity.getScheduledAt(),
                    eventEntity.isRecurringWeekly(),
                    List.copyOf(playerIds)
            );
        } catch (RuntimeException exception) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw exception;
        } finally {
            entityManager.close();
        }
    }

    public List<TtrpgEventDetails> findAllByGuildId(long guildId) {
        EntityManager entityManager = JpaManager.createEntityManager();
        try {
            List<TtrpgEventEntity> eventEntities = entityManager.createQuery(
                    "SELECT event FROM TtrpgEventEntity event WHERE event.guildId = :guildId ORDER BY event.scheduledAt ASC, event.id ASC",
                    TtrpgEventEntity.class
            ).setParameter("guildId", guildId).getResultList();

            return eventEntities.stream()
                    .map(eventEntity -> new TtrpgEventDetails(
                            eventEntity.getId(),
                            eventEntity.getGuildId(),
                            eventEntity.getGmUserId(),
                            eventEntity.getName(),
                            eventEntity.getScheduledAt(),
                            eventEntity.isRecurringWeekly(),
                            findPlayerIdsByEventId(entityManager, eventEntity.getId())
                    ))
                    .toList();
        } finally {
            entityManager.close();
        }
    }

    public Optional<TtrpgEventDetails> updateEvent(
            long guildId,
            long eventId,
            long actorUserId,
            boolean bypassGmAuthorization,
            Long newGmUserId,
            String newName,
            Instant newScheduledAt,
            Boolean newRecurringWeekly,
            List<Long> replacementPlayerIds
    ) {
        EntityManager entityManager = JpaManager.createEntityManager();
        try {
            entityManager.getTransaction().begin();

            TtrpgEventEntity eventEntity = entityManager.find(TtrpgEventEntity.class, eventId);
            if (eventEntity == null || eventEntity.getGuildId() != guildId) {
                entityManager.getTransaction().rollback();
                return Optional.empty();
            }

            if (!bypassGmAuthorization && eventEntity.getGmUserId() != actorUserId) {
                entityManager.getTransaction().rollback();
                return Optional.empty();
            }

            if (newGmUserId != null) {
                eventEntity.setGmUserId(newGmUserId);
            }
            if (newName != null) {
                eventEntity.setName(newName);
            }
            if (newScheduledAt != null) {
                eventEntity.setScheduledAt(newScheduledAt);
            }
            if (newRecurringWeekly != null) {
                eventEntity.setRecurringWeekly(newRecurringWeekly);
            }

            long effectiveGmUserId = eventEntity.getGmUserId();
            if (replacementPlayerIds != null) {
                entityManager.createQuery("DELETE FROM TtrpgEventPlayerEntity player WHERE player.eventId = :eventId")
                        .setParameter("eventId", eventId)
                        .executeUpdate();

                for (Long playerId : replacementPlayerIds) {
                    entityManager.persist(new TtrpgEventPlayerEntity(eventId, playerId));
                }
            } else {
                boolean gmAlreadyPresent = !entityManager.createQuery(
                                "SELECT player.userId FROM TtrpgEventPlayerEntity player WHERE player.eventId = :eventId AND player.userId = :userId",
                                Long.class
                        )
                        .setParameter("eventId", eventId)
                        .setParameter("userId", effectiveGmUserId)
                        .getResultList()
                        .isEmpty();

                if (!gmAlreadyPresent) {
                    entityManager.persist(new TtrpgEventPlayerEntity(eventId, effectiveGmUserId));
                }
            }

            entityManager.getTransaction().commit();
            return Optional.of(new TtrpgEventDetails(
                    eventEntity.getId(),
                    eventEntity.getGuildId(),
                    eventEntity.getGmUserId(),
                    eventEntity.getName(),
                    eventEntity.getScheduledAt(),
                    eventEntity.isRecurringWeekly(),
                    findPlayerIdsByEventId(entityManager, eventEntity.getId())
            ));
        } catch (RuntimeException exception) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw exception;
        } finally {
            entityManager.close();
        }
    }

    public boolean deleteEvent(long guildId, long eventId, long actorUserId, boolean bypassGmAuthorization) {
        EntityManager entityManager = JpaManager.createEntityManager();
        try {
            entityManager.getTransaction().begin();

            TtrpgEventEntity eventEntity = entityManager.find(TtrpgEventEntity.class, eventId);
            if (eventEntity == null || eventEntity.getGuildId() != guildId) {
                entityManager.getTransaction().rollback();
                return false;
            }

            if (!bypassGmAuthorization && eventEntity.getGmUserId() != actorUserId) {
                entityManager.getTransaction().rollback();
                return false;
            }

            entityManager.createQuery("DELETE FROM TtrpgEventPlayerEntity player WHERE player.eventId = :eventId")
                    .setParameter("eventId", eventId)
                    .executeUpdate();
            entityManager.remove(eventEntity);

            entityManager.getTransaction().commit();
            return true;
        } catch (RuntimeException exception) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw exception;
        } finally {
            entityManager.close();
        }
    }

    private List<Long> findPlayerIdsByEventId(EntityManager entityManager, long eventId) {
        return entityManager.createQuery(
                "SELECT player.userId FROM TtrpgEventPlayerEntity player WHERE player.eventId = :eventId ORDER BY player.userId ASC",
                Long.class
        ).setParameter("eventId", eventId).getResultList();
    }
}
