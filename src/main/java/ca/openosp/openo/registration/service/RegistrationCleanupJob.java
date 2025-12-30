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
package ca.openosp.openo.registration.service;

import ca.openosp.openo.registration.dao.PatientRegistrationQueueDao;
import ca.openosp.openo.registration.dao.RegistrationTokenDao;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;

/**
 * Scheduled job for cleaning up expired registration tokens and records.
 *
 * <p>Runs periodically to:
 * <ul>
 *   <li>Expire pending registrations that have passed their token expiry</li>
 *   <li>Purge old rejected/expired records after retention period</li>
 *   <li>Clean up used/expired tokens</li>
 * </ul>
 * </p>
 *
 * <p>Part of the Patient Self-Registration Module - a standalone commercial module
 * for OpenO EMR that enables patients to self-register via QR code scanning.</p>
 *
 * @since 2025-12-29
 */
@Component
public class RegistrationCleanupJob {

    private static final Logger logger = LogManager.getLogger(RegistrationCleanupJob.class);

    private RegistrationTokenDao tokenDao;
    private PatientRegistrationQueueDao queueDao;

    @Autowired(required = false)
    private RateLimitService rateLimitService;

    /** Number of days to retain rejected/expired records before purging */
    private int retentionDays = 30;

    // ========================================================================
    // Scheduled Tasks
    // ========================================================================

    /**
     * Expires tokens and registrations that have passed their expiry time.
     * Runs every 15 minutes.
     */
    @Scheduled(cron = "0 */15 * * * *")
    @Transactional
    public void expireOldTokensAndRegistrations() {
        logger.debug("Running token and registration expiry job");

        try {
            // Expire old tokens
            if (tokenDao != null) {
                int expiredTokens = tokenDao.invalidateExpiredTokens();
                if (expiredTokens > 0) {
                    logger.info("Invalidated {} expired registration tokens", expiredTokens);
                }
            }

            // Expire old registrations
            if (queueDao != null) {
                int expiredRegistrations = queueDao.expireOldRecords();
                if (expiredRegistrations > 0) {
                    logger.info("Expired {} pending registrations", expiredRegistrations);
                }
            }

            // Clean up rate limit cache
            if (rateLimitService != null) {
                rateLimitService.cleanupExpiredEntries();
            }

        } catch (Exception e) {
            logger.error("Error during token/registration expiry job", e);
        }
    }

    /**
     * Purges old rejected and expired records.
     * Runs daily at 2 AM.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void purgeOldRecords() {
        logger.info("Running registration record purge job");

        try {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -retentionDays);
            Date cutoff = cal.getTime();

            // Purge old queue records
            if (queueDao != null) {
                int purgedRecords = queueDao.purgeOldRecords(cutoff);
                if (purgedRecords > 0) {
                    logger.info("Purged {} old registration records (older than {} days)",
                            purgedRecords, retentionDays);
                }
            }

            // Purge old tokens
            if (tokenDao != null) {
                // Delete tokens expired more than 24 hours ago
                Calendar tokenCutoff = Calendar.getInstance();
                tokenCutoff.add(Calendar.HOUR, -24);
                int purgedTokens = tokenDao.cleanupExpiredTokens(tokenCutoff.getTime());
                if (purgedTokens > 0) {
                    logger.info("Purged {} expired tokens", purgedTokens);
                }

                // Delete used tokens older than retention period
                int purgedUsedTokens = tokenDao.cleanupUsedTokens(cutoff);
                if (purgedUsedTokens > 0) {
                    logger.info("Purged {} old used tokens", purgedUsedTokens);
                }
            }

        } catch (Exception e) {
            logger.error("Error during record purge job", e);
        }
    }

    /**
     * Manual trigger for expiry job (for testing/admin).
     */
    public void triggerExpiryNow() {
        expireOldTokensAndRegistrations();
    }

    /**
     * Manual trigger for purge job (for testing/admin).
     */
    public void triggerPurgeNow() {
        purgeOldRecords();
    }

    // ========================================================================
    // Configuration
    // ========================================================================

    public void setTokenDao(RegistrationTokenDao tokenDao) {
        this.tokenDao = tokenDao;
    }

    public void setQueueDao(PatientRegistrationQueueDao queueDao) {
        this.queueDao = queueDao;
    }

    public void setRetentionDays(int retentionDays) {
        this.retentionDays = retentionDays;
    }

    public int getRetentionDays() {
        return retentionDays;
    }
}
