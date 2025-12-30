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

import java.util.Date;
import java.util.List;

/**
 * Data Access Object interface for PatientRegistrationQueue entity.
 *
 * <p>Provides CRUD operations and specialized queries for managing patient
 * registrations in the queue.</p>
 *
 * <p>Part of the Patient Self-Registration Module - a standalone commercial module
 * for OpenO EMR that enables patients to self-register via QR code scanning.</p>
 *
 * @since 2025-12-29
 */
public interface PatientRegistrationQueueDao {

    // ========================================================================
    // Basic CRUD Operations
    // ========================================================================

    /**
     * Finds a registration by its primary key.
     *
     * @param id Integer the registration ID
     * @return PatientRegistrationQueue the registration entity, or null if not found
     */
    PatientRegistrationQueue find(Integer id);

    /**
     * Persists a new registration to the database.
     *
     * @param entity PatientRegistrationQueue the entity to persist
     */
    void persist(PatientRegistrationQueue entity);

    /**
     * Merges changes to an existing registration.
     *
     * @param entity PatientRegistrationQueue the entity to merge
     * @return PatientRegistrationQueue the merged entity
     */
    PatientRegistrationQueue merge(PatientRegistrationQueue entity);

    /**
     * Removes a registration from the database.
     *
     * @param entity PatientRegistrationQueue the entity to remove
     */
    void remove(PatientRegistrationQueue entity);

    /**
     * Removes a registration by its ID.
     *
     * @param id Integer the registration ID to remove
     */
    void remove(Integer id);

    // ========================================================================
    // Query by Token
    // ========================================================================

    /**
     * Finds a registration by its unique token.
     *
     * @param token String the registration token
     * @return PatientRegistrationQueue the registration entity, or null if not found
     */
    PatientRegistrationQueue findByToken(String token);

    // ========================================================================
    // Query by Status
    // ========================================================================

    /**
     * Finds all registrations with the specified status.
     *
     * @param status RegistrationStatus the status to filter by
     * @return List of PatientRegistrationQueue matching the status
     */
    List<PatientRegistrationQueue> findByStatus(RegistrationStatus status);

    /**
     * Finds all pending registrations for a specific facility.
     *
     * @param facilityId Integer the facility ID
     * @return List of PatientRegistrationQueue with pending status
     */
    List<PatientRegistrationQueue> findPendingByFacility(Integer facilityId);

    /**
     * Counts the number of pending registrations for a facility.
     *
     * @param facilityId Integer the facility ID
     * @return int the count of pending registrations
     */
    int countPendingByFacility(Integer facilityId);

    /**
     * Counts all pending registrations across all facilities.
     *
     * @return int the total count of pending registrations
     */
    int countPending();

    // ========================================================================
    // Query by Patient Data
    // ========================================================================

    /**
     * Finds registrations by HIN (Health Insurance Number).
     *
     * @param hin String the health insurance number
     * @return List of PatientRegistrationQueue with matching HIN
     */
    List<PatientRegistrationQueue> findByHin(String hin);

    /**
     * Checks if a registration with the given HIN already exists (pending or approved).
     *
     * @param hin String the health insurance number
     * @return boolean true if a registration with this HIN exists
     */
    boolean existsByHin(String hin);

    /**
     * Finds potential duplicate registrations by name and date of birth.
     *
     * @param lastName String the last name
     * @param firstName String the first name
     * @param yearOfBirth String the year of birth
     * @param monthOfBirth String the month of birth
     * @param dateOfBirth String the day of birth
     * @return List of PatientRegistrationQueue that are potential duplicates
     */
    List<PatientRegistrationQueue> findPotentialDuplicates(
            String lastName, String firstName,
            String yearOfBirth, String monthOfBirth, String dateOfBirth);

    // ========================================================================
    // Query by Date Range
    // ========================================================================

    /**
     * Finds registrations submitted within a date range.
     *
     * @param startDate Date the start of the range
     * @param endDate Date the end of the range
     * @return List of PatientRegistrationQueue submitted in the range
     */
    List<PatientRegistrationQueue> findBySubmittedDateRange(Date startDate, Date endDate);

    /**
     * Finds registrations created within a date range.
     *
     * @param startDate Date the start of the range
     * @param endDate Date the end of the range
     * @return List of PatientRegistrationQueue created in the range
     */
    List<PatientRegistrationQueue> findByCreatedDateRange(Date startDate, Date endDate);

    // ========================================================================
    // Cleanup Operations
    // ========================================================================

    /**
     * Marks all expired registrations (past expiry date) as expired.
     *
     * @return int the number of records updated
     */
    int expireOldRecords();

    /**
     * Marks registrations as expired if they were created before the cutoff
     * and are still pending.
     *
     * @param cutoff Date the cutoff date/time
     * @return int the number of records updated
     */
    int expireRecordsOlderThan(Date cutoff);

    /**
     * Permanently deletes rejected or expired records older than the specified date.
     *
     * @param cutoff Date the cutoff date for deletion
     * @return int the number of records deleted
     */
    int purgeOldRecords(Date cutoff);

    // ========================================================================
    // Statistics
    // ========================================================================

    /**
     * Gets registration statistics for a facility.
     *
     * @param facilityId Integer the facility ID
     * @return RegistrationStats object with counts by status
     */
    RegistrationStats getStatsByFacility(Integer facilityId);

    /**
     * Simple statistics container for registration counts.
     */
    class RegistrationStats {
        private int pending;
        private int approved;
        private int rejected;
        private int expired;
        private int total;

        public RegistrationStats() {}

        public RegistrationStats(int pending, int approved, int rejected, int expired) {
            this.pending = pending;
            this.approved = approved;
            this.rejected = rejected;
            this.expired = expired;
            this.total = pending + approved + rejected + expired;
        }

        public int getPending() { return pending; }
        public void setPending(int pending) { this.pending = pending; this.updateTotal(); }

        public int getApproved() { return approved; }
        public void setApproved(int approved) { this.approved = approved; this.updateTotal(); }

        public int getRejected() { return rejected; }
        public void setRejected(int rejected) { this.rejected = rejected; this.updateTotal(); }

        public int getExpired() { return expired; }
        public void setExpired(int expired) { this.expired = expired; this.updateTotal(); }

        public int getTotal() { return total; }

        private void updateTotal() {
            this.total = pending + approved + rejected + expired;
        }
    }
}
