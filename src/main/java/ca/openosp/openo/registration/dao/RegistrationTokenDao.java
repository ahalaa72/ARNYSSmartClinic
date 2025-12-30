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

import java.util.Date;
import java.util.List;

/**
 * Data Access Object interface for RegistrationToken entity.
 *
 * <p>Provides CRUD operations and specialized queries for managing registration
 * session tokens generated when patients scan the QR code.</p>
 *
 * <p>Part of the Patient Self-Registration Module - a standalone commercial module
 * for OpenO EMR that enables patients to self-register via QR code scanning.</p>
 *
 * @since 2025-12-29
 */
public interface RegistrationTokenDao {

    // ========================================================================
    // Basic CRUD Operations
    // ========================================================================

    /**
     * Finds a token record by its primary key.
     *
     * @param id Integer the token ID
     * @return RegistrationToken the token entity, or null if not found
     */
    RegistrationToken find(Integer id);

    /**
     * Persists a new token to the database.
     *
     * @param entity RegistrationToken the entity to persist
     */
    void persist(RegistrationToken entity);

    /**
     * Merges changes to an existing token.
     *
     * @param entity RegistrationToken the entity to merge
     * @return RegistrationToken the merged entity
     */
    RegistrationToken merge(RegistrationToken entity);

    /**
     * Removes a token from the database.
     *
     * @param entity RegistrationToken the entity to remove
     */
    void remove(RegistrationToken entity);

    // ========================================================================
    // Token Lookup
    // ========================================================================

    /**
     * Finds a token by its unique token string.
     *
     * @param token String the token value
     * @return RegistrationToken the token entity, or null if not found
     */
    RegistrationToken findByToken(String token);

    /**
     * Checks if a token exists and is valid (not expired, not used).
     *
     * @param token String the token value
     * @return boolean true if the token is valid
     */
    boolean isTokenValid(String token);

    // ========================================================================
    // Token Management
    // ========================================================================

    /**
     * Marks a token as used (consumed).
     *
     * @param token String the token value to invalidate
     * @return boolean true if the token was found and invalidated
     */
    boolean invalidateToken(String token);

    /**
     * Marks all expired tokens (past expiry time) as invalid.
     *
     * @return int the number of tokens invalidated
     */
    int invalidateExpiredTokens();

    // ========================================================================
    // Rate Limiting Queries
    // ========================================================================

    /**
     * Counts tokens created by a specific IP address within the last hour.
     *
     * @param ipAddress String the IP address to check
     * @return int the count of tokens created
     */
    int countTokensByIpLastHour(String ipAddress);

    /**
     * Finds all tokens created by a specific IP address within a time window.
     *
     * @param ipAddress String the IP address
     * @param since Date the start of the time window
     * @return List of RegistrationToken created by this IP
     */
    List<RegistrationToken> findByIpAddressSince(String ipAddress, Date since);

    // ========================================================================
    // Cleanup Operations
    // ========================================================================

    /**
     * Deletes tokens that expired before the specified date.
     *
     * @param cutoff Date tokens expiring before this date will be deleted
     * @return int the number of tokens deleted
     */
    int cleanupExpiredTokens(Date cutoff);

    /**
     * Deletes all used tokens older than the specified date.
     *
     * @param cutoff Date used tokens older than this will be deleted
     * @return int the number of tokens deleted
     */
    int cleanupUsedTokens(Date cutoff);

    // ========================================================================
    // Statistics
    // ========================================================================

    /**
     * Counts active (valid, unused, not expired) tokens for a facility.
     *
     * @param facilityId Integer the facility ID
     * @return int the count of active tokens
     */
    int countActiveTokensByFacility(Integer facilityId);

    /**
     * Gets the total number of tokens created today for a facility.
     *
     * @param facilityId Integer the facility ID
     * @return int the count of tokens created today
     */
    int countTokensCreatedToday(Integer facilityId);
}
