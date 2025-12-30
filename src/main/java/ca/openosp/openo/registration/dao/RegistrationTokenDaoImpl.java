/**
 * Copyright (c) 2025. OpenOSP. All Rights Reserved.
 * This software is published under the GPL GNU General Public License.
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * This software was written for OpenOSP - Open Source Patient Registration Module.
 *
 * @since 2025-12-29
 */
package ca.openosp.openo.registration.dao;

import ca.openosp.openo.registration.model.RegistrationToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Implementation of RegistrationTokenDao using JPA/Hibernate.
 *
 * <p>Part of the Patient Self-Registration Module - a standalone commercial module
 * for OpenO EMR that enables patients to self-register via QR code scanning.</p>
 *
 * @since 2025-12-29
 */
@Repository
@Transactional
public class RegistrationTokenDaoImpl implements RegistrationTokenDao {

    private static final Logger logger = LogManager.getLogger(RegistrationTokenDaoImpl.class);

    @PersistenceContext
    private EntityManager entityManager;

    // ========================================================================
    // Basic CRUD Operations
    // ========================================================================

    @Override
    public RegistrationToken find(Integer id) {
        return entityManager.find(RegistrationToken.class, id);
    }

    @Override
    public void persist(RegistrationToken entity) {
        entityManager.persist(entity);
        logger.debug("Persisted RegistrationToken: {}",
                entity.getToken() != null ?
                        entity.getToken().substring(0, 8) + "..." : "null");
    }

    @Override
    public RegistrationToken merge(RegistrationToken entity) {
        return entityManager.merge(entity);
    }

    @Override
    public void remove(RegistrationToken entity) {
        if (entityManager.contains(entity)) {
            entityManager.remove(entity);
        } else {
            RegistrationToken attached = find(entity.getId());
            if (attached != null) {
                entityManager.remove(attached);
            }
        }
    }

    // ========================================================================
    // Token Lookup
    // ========================================================================

    @Override
    public RegistrationToken findByToken(String token) {
        try {
            TypedQuery<RegistrationToken> query = entityManager.createQuery(
                    "SELECT t FROM RegistrationToken t WHERE t.token = :token",
                    RegistrationToken.class);
            query.setParameter("token", token);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public boolean isTokenValid(String token) {
        try {
            TypedQuery<Long> query = entityManager.createQuery(
                    "SELECT COUNT(t) FROM RegistrationToken t " +
                            "WHERE t.token = :token " +
                            "AND t.used = false " +
                            "AND t.expiresAt > :now",
                    Long.class);
            query.setParameter("token", token);
            query.setParameter("now", new Date());
            return query.getSingleResult() > 0;
        } catch (Exception e) {
            logger.error("Error checking token validity", e);
            return false;
        }
    }

    // ========================================================================
    // Token Management
    // ========================================================================

    @Override
    public boolean invalidateToken(String token) {
        Query query = entityManager.createQuery(
                "UPDATE RegistrationToken t " +
                        "SET t.used = true, t.usedAt = :now " +
                        "WHERE t.token = :token AND t.used = false");
        query.setParameter("token", token);
        query.setParameter("now", new Date());
        int count = query.executeUpdate();
        if (count > 0) {
            logger.debug("Invalidated token: {}...", token.substring(0, 8));
        }
        return count > 0;
    }

    @Override
    public int invalidateExpiredTokens() {
        Query query = entityManager.createQuery(
                "UPDATE RegistrationToken t " +
                        "SET t.used = true " +
                        "WHERE t.used = false AND t.expiresAt < :now");
        query.setParameter("now", new Date());
        int count = query.executeUpdate();
        logger.info("Invalidated {} expired tokens", count);
        return count;
    }

    // ========================================================================
    // Rate Limiting Queries
    // ========================================================================

    @Override
    public int countTokensByIpLastHour(String ipAddress) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, -1);
        Date oneHourAgo = cal.getTime();

        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(t) FROM RegistrationToken t " +
                        "WHERE t.ipAddress = :ipAddress " +
                        "AND t.createdAt >= :since",
                Long.class);
        query.setParameter("ipAddress", ipAddress);
        query.setParameter("since", oneHourAgo);
        return query.getSingleResult().intValue();
    }

    @Override
    public List<RegistrationToken> findByIpAddressSince(String ipAddress, Date since) {
        TypedQuery<RegistrationToken> query = entityManager.createQuery(
                "SELECT t FROM RegistrationToken t " +
                        "WHERE t.ipAddress = :ipAddress " +
                        "AND t.createdAt >= :since " +
                        "ORDER BY t.createdAt DESC",
                RegistrationToken.class);
        query.setParameter("ipAddress", ipAddress);
        query.setParameter("since", since);
        return query.getResultList();
    }

    // ========================================================================
    // Cleanup Operations
    // ========================================================================

    @Override
    public int cleanupExpiredTokens(Date cutoff) {
        Query query = entityManager.createQuery(
                "DELETE FROM RegistrationToken t WHERE t.expiresAt < :cutoff");
        query.setParameter("cutoff", cutoff);
        int count = query.executeUpdate();
        logger.info("Deleted {} expired tokens older than {}", count, cutoff);
        return count;
    }

    @Override
    public int cleanupUsedTokens(Date cutoff) {
        Query query = entityManager.createQuery(
                "DELETE FROM RegistrationToken t " +
                        "WHERE t.used = true AND t.usedAt < :cutoff");
        query.setParameter("cutoff", cutoff);
        int count = query.executeUpdate();
        logger.info("Deleted {} used tokens older than {}", count, cutoff);
        return count;
    }

    // ========================================================================
    // Statistics
    // ========================================================================

    @Override
    public int countActiveTokensByFacility(Integer facilityId) {
        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(t) FROM RegistrationToken t " +
                        "WHERE t.facilityId = :facilityId " +
                        "AND t.used = false " +
                        "AND t.expiresAt > :now",
                Long.class);
        query.setParameter("facilityId", facilityId);
        query.setParameter("now", new Date());
        return query.getSingleResult().intValue();
    }

    @Override
    public int countTokensCreatedToday(Integer facilityId) {
        // Get start of today
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date startOfDay = cal.getTime();

        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(t) FROM RegistrationToken t " +
                        "WHERE t.facilityId = :facilityId " +
                        "AND t.createdAt >= :startOfDay",
                Long.class);
        query.setParameter("facilityId", facilityId);
        query.setParameter("startOfDay", startOfDay);
        return query.getSingleResult().intValue();
    }
}
