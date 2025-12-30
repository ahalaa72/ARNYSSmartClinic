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

import ca.openosp.openo.registration.model.PatientRegistrationQueue;
import ca.openosp.openo.registration.model.PatientRegistrationQueue.RegistrationStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.Date;
import java.util.List;

/**
 * Implementation of PatientRegistrationQueueDao using JPA/Hibernate.
 *
 * <p>Part of the Patient Self-Registration Module - a standalone commercial module
 * for OpenO EMR that enables patients to self-register via QR code scanning.</p>
 *
 * @since 2025-12-29
 */
@Repository
@Transactional
public class PatientRegistrationQueueDaoImpl implements PatientRegistrationQueueDao {

    private static final Logger logger = LogManager.getLogger(PatientRegistrationQueueDaoImpl.class);

    @PersistenceContext
    private EntityManager entityManager;

    // ========================================================================
    // Basic CRUD Operations
    // ========================================================================

    @Override
    public PatientRegistrationQueue find(Integer id) {
        return entityManager.find(PatientRegistrationQueue.class, id);
    }

    @Override
    public void persist(PatientRegistrationQueue entity) {
        entityManager.persist(entity);
        logger.debug("Persisted PatientRegistrationQueue with token: {}",
                entity.getRegistrationToken() != null ?
                        entity.getRegistrationToken().substring(0, 8) + "..." : "null");
    }

    @Override
    public PatientRegistrationQueue merge(PatientRegistrationQueue entity) {
        return entityManager.merge(entity);
    }

    @Override
    public void remove(PatientRegistrationQueue entity) {
        if (entityManager.contains(entity)) {
            entityManager.remove(entity);
        } else {
            PatientRegistrationQueue attached = find(entity.getId());
            if (attached != null) {
                entityManager.remove(attached);
            }
        }
    }

    @Override
    public void remove(Integer id) {
        PatientRegistrationQueue entity = find(id);
        if (entity != null) {
            remove(entity);
        }
    }

    // ========================================================================
    // Query by Token
    // ========================================================================

    @Override
    public PatientRegistrationQueue findByToken(String token) {
        try {
            TypedQuery<PatientRegistrationQueue> query = entityManager.createQuery(
                    "SELECT p FROM PatientRegistrationQueue p WHERE p.registrationToken = :token",
                    PatientRegistrationQueue.class);
            query.setParameter("token", token);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    // ========================================================================
    // Query by Status
    // ========================================================================

    @Override
    public List<PatientRegistrationQueue> findByStatus(RegistrationStatus status) {
        TypedQuery<PatientRegistrationQueue> query = entityManager.createQuery(
                "SELECT p FROM PatientRegistrationQueue p WHERE p.status = :status ORDER BY p.submittedAt DESC",
                PatientRegistrationQueue.class);
        query.setParameter("status", status);
        return query.getResultList();
    }

    @Override
    public List<PatientRegistrationQueue> findPendingByFacility(Integer facilityId) {
        TypedQuery<PatientRegistrationQueue> query = entityManager.createQuery(
                "SELECT p FROM PatientRegistrationQueue p " +
                        "WHERE p.facilityId = :facilityId AND p.status = :status " +
                        "ORDER BY p.submittedAt DESC",
                PatientRegistrationQueue.class);
        query.setParameter("facilityId", facilityId);
        query.setParameter("status", RegistrationStatus.PENDING);
        return query.getResultList();
    }

    @Override
    public int countPendingByFacility(Integer facilityId) {
        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(p) FROM PatientRegistrationQueue p " +
                        "WHERE p.facilityId = :facilityId AND p.status = :status",
                Long.class);
        query.setParameter("facilityId", facilityId);
        query.setParameter("status", RegistrationStatus.PENDING);
        return query.getSingleResult().intValue();
    }

    @Override
    public int countPending() {
        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(p) FROM PatientRegistrationQueue p WHERE p.status = :status",
                Long.class);
        query.setParameter("status", RegistrationStatus.PENDING);
        return query.getSingleResult().intValue();
    }

    // ========================================================================
    // Query by Patient Data
    // ========================================================================

    @Override
    public List<PatientRegistrationQueue> findByHin(String hin) {
        TypedQuery<PatientRegistrationQueue> query = entityManager.createQuery(
                "SELECT p FROM PatientRegistrationQueue p WHERE p.hin = :hin ORDER BY p.createdAt DESC",
                PatientRegistrationQueue.class);
        query.setParameter("hin", hin);
        return query.getResultList();
    }

    @Override
    public boolean existsByHin(String hin) {
        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(p) FROM PatientRegistrationQueue p " +
                        "WHERE p.hin = :hin AND p.status IN (:pending, :approved)",
                Long.class);
        query.setParameter("hin", hin);
        query.setParameter("pending", RegistrationStatus.PENDING);
        query.setParameter("approved", RegistrationStatus.APPROVED);
        return query.getSingleResult() > 0;
    }

    @Override
    public List<PatientRegistrationQueue> findPotentialDuplicates(
            String lastName, String firstName,
            String yearOfBirth, String monthOfBirth, String dateOfBirth) {

        TypedQuery<PatientRegistrationQueue> query = entityManager.createQuery(
                "SELECT p FROM PatientRegistrationQueue p " +
                        "WHERE UPPER(p.lastName) = UPPER(:lastName) " +
                        "AND UPPER(p.firstName) = UPPER(:firstName) " +
                        "AND p.yearOfBirth = :yearOfBirth " +
                        "AND p.monthOfBirth = :monthOfBirth " +
                        "AND p.dateOfBirth = :dateOfBirth " +
                        "AND p.status IN (:pending, :approved) " +
                        "ORDER BY p.createdAt DESC",
                PatientRegistrationQueue.class);
        query.setParameter("lastName", lastName);
        query.setParameter("firstName", firstName);
        query.setParameter("yearOfBirth", yearOfBirth);
        query.setParameter("monthOfBirth", monthOfBirth);
        query.setParameter("dateOfBirth", dateOfBirth);
        query.setParameter("pending", RegistrationStatus.PENDING);
        query.setParameter("approved", RegistrationStatus.APPROVED);
        return query.getResultList();
    }

    // ========================================================================
    // Query by Date Range
    // ========================================================================

    @Override
    public List<PatientRegistrationQueue> findBySubmittedDateRange(Date startDate, Date endDate) {
        TypedQuery<PatientRegistrationQueue> query = entityManager.createQuery(
                "SELECT p FROM PatientRegistrationQueue p " +
                        "WHERE p.submittedAt >= :startDate AND p.submittedAt <= :endDate " +
                        "ORDER BY p.submittedAt DESC",
                PatientRegistrationQueue.class);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        return query.getResultList();
    }

    @Override
    public List<PatientRegistrationQueue> findByCreatedDateRange(Date startDate, Date endDate) {
        TypedQuery<PatientRegistrationQueue> query = entityManager.createQuery(
                "SELECT p FROM PatientRegistrationQueue p " +
                        "WHERE p.createdAt >= :startDate AND p.createdAt <= :endDate " +
                        "ORDER BY p.createdAt DESC",
                PatientRegistrationQueue.class);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        return query.getResultList();
    }

    // ========================================================================
    // Cleanup Operations
    // ========================================================================

    @Override
    public int expireOldRecords() {
        Query query = entityManager.createQuery(
                "UPDATE PatientRegistrationQueue p SET p.status = :expired " +
                        "WHERE p.status = :pending AND p.expiresAt < :now");
        query.setParameter("expired", RegistrationStatus.EXPIRED);
        query.setParameter("pending", RegistrationStatus.PENDING);
        query.setParameter("now", new Date());
        int count = query.executeUpdate();
        logger.info("Expired {} registration records", count);
        return count;
    }

    @Override
    public int expireRecordsOlderThan(Date cutoff) {
        Query query = entityManager.createQuery(
                "UPDATE PatientRegistrationQueue p SET p.status = :expired " +
                        "WHERE p.status = :pending AND p.createdAt < :cutoff");
        query.setParameter("expired", RegistrationStatus.EXPIRED);
        query.setParameter("pending", RegistrationStatus.PENDING);
        query.setParameter("cutoff", cutoff);
        int count = query.executeUpdate();
        logger.info("Expired {} registration records older than {}", count, cutoff);
        return count;
    }

    @Override
    public int purgeOldRecords(Date cutoff) {
        Query query = entityManager.createQuery(
                "DELETE FROM PatientRegistrationQueue p " +
                        "WHERE p.status IN (:rejected, :expired) " +
                        "AND p.createdAt < :cutoff");
        query.setParameter("rejected", RegistrationStatus.REJECTED);
        query.setParameter("expired", RegistrationStatus.EXPIRED);
        query.setParameter("cutoff", cutoff);
        int count = query.executeUpdate();
        logger.info("Purged {} old registration records", count);
        return count;
    }

    // ========================================================================
    // Statistics
    // ========================================================================

    @Override
    public RegistrationStats getStatsByFacility(Integer facilityId) {
        RegistrationStats stats = new RegistrationStats();

        String baseQuery = "SELECT COUNT(p) FROM PatientRegistrationQueue p " +
                "WHERE p.facilityId = :facilityId AND p.status = :status";

        // Count pending
        TypedQuery<Long> pendingQuery = entityManager.createQuery(baseQuery, Long.class);
        pendingQuery.setParameter("facilityId", facilityId);
        pendingQuery.setParameter("status", RegistrationStatus.PENDING);
        stats.setPending(pendingQuery.getSingleResult().intValue());

        // Count approved
        TypedQuery<Long> approvedQuery = entityManager.createQuery(baseQuery, Long.class);
        approvedQuery.setParameter("facilityId", facilityId);
        approvedQuery.setParameter("status", RegistrationStatus.APPROVED);
        stats.setApproved(approvedQuery.getSingleResult().intValue());

        // Count rejected
        TypedQuery<Long> rejectedQuery = entityManager.createQuery(baseQuery, Long.class);
        rejectedQuery.setParameter("facilityId", facilityId);
        rejectedQuery.setParameter("status", RegistrationStatus.REJECTED);
        stats.setRejected(rejectedQuery.getSingleResult().intValue());

        // Count expired
        TypedQuery<Long> expiredQuery = entityManager.createQuery(baseQuery, Long.class);
        expiredQuery.setParameter("facilityId", facilityId);
        expiredQuery.setParameter("status", RegistrationStatus.EXPIRED);
        stats.setExpired(expiredQuery.getSingleResult().intValue());

        return stats;
    }
}
