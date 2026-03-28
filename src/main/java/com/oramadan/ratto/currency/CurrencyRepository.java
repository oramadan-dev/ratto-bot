package com.oramadan.ratto.currency;

import com.oramadan.ratto.config.CurrencyConfig;
import com.oramadan.ratto.persistence.JpaManager;
import jakarta.persistence.EntityManager;

import java.util.List;

public class CurrencyRepository {

    private final int defaultChedda;

    public CurrencyRepository(CurrencyConfig currencyConfig) {
        this.defaultChedda = currencyConfig.getDefaultChedda();
    }

    public CurrencyEntity findByUserId(long userId) {
        EntityManager entityManager = JpaManager.createEntityManager();
        try {
            // Try and find an existing currency entry
            CurrencyEntity currencyEntity = entityManager.find(CurrencyEntity.class, userId);
            if (currencyEntity != null) {
                return currencyEntity;
            }

            // Create and persist the default amount of currency
            entityManager.getTransaction().begin();
            CurrencyEntity newCurrencyEntity = new CurrencyEntity(userId, defaultChedda);
            entityManager.persist(newCurrencyEntity);
            entityManager.getTransaction().commit();

            return newCurrencyEntity;
        } catch (RuntimeException exception) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw exception;
        } finally {
            entityManager.close();
        }
    }

    public CurrencyEntity save(CurrencyEntity currencyEntity) {
        EntityManager entityManager = JpaManager.createEntityManager();
        try {
            entityManager.getTransaction().begin();
            CurrencyEntity savedEntity = entityManager.merge(currencyEntity);
            entityManager.getTransaction().commit();
            return savedEntity;
        } catch (RuntimeException exception) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw exception;
        } finally {
            entityManager.close();
        }
    }

    public List<CurrencyEntity> findAllOrderByCheddaDesc() {
        EntityManager entityManager = JpaManager.createEntityManager();
        try {
            return entityManager.createQuery(
                    "SELECT currency FROM CurrencyEntity currency ORDER BY currency.chedda DESC, currency.userId ASC",
                    CurrencyEntity.class
            ).getResultList();
        } finally {
            entityManager.close();
        }
    }

    public void deleteByUserId(long userId) {
        EntityManager entityManager = JpaManager.createEntityManager();
        try {
            entityManager.getTransaction().begin();
            CurrencyEntity currencyEntity = entityManager.find(CurrencyEntity.class, userId);
            if (currencyEntity != null) {
                entityManager.remove(currencyEntity);
            }
            entityManager.getTransaction().commit();
        } catch (RuntimeException exception) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw exception;
        } finally {
            entityManager.close();
        }
    }
}
