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

import ca.openosp.openo.registration.dao.RegistrationTokenDao;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for rate limiting registration API endpoints.
 *
 * <p>Prevents abuse of the public registration endpoints by limiting
 * the number of requests per IP address per hour.</p>
 *
 * <p>Part of the Patient Self-Registration Module - a standalone commercial module
 * for OpenO EMR that enables patients to self-register via QR code scanning.</p>
 *
 * @since 2025-12-29
 */
@Service
public class RateLimitService {

    private static final Logger logger = LogManager.getLogger(RateLimitService.class);

    /** Maximum sessions (QR scans) per IP per hour */
    private int maxSessionsPerIpPerHour = 5;

    /** Maximum form submissions per IP per hour */
    private int maxSubmissionsPerIpPerHour = 5;

    /** Whether rate limiting is enabled */
    private boolean enabled = true;

    /** In-memory cache for rate limiting (consider Redis for clustered deployments) */
    private final Map<String, RateLimitEntry> sessionLimits = new ConcurrentHashMap<>();
    private final Map<String, RateLimitEntry> submissionLimits = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private RegistrationTokenDao registrationTokenDao;

    // ========================================================================
    // Rate Limit Entry
    // ========================================================================

    /**
     * Entry tracking rate limit for a single IP.
     */
    private static class RateLimitEntry {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStart;
        private static final long WINDOW_DURATION_MS = 60 * 60 * 1000; // 1 hour

        public RateLimitEntry() {
            this.windowStart = System.currentTimeMillis();
        }

        public boolean isWindowExpired() {
            return System.currentTimeMillis() - windowStart > WINDOW_DURATION_MS;
        }

        public void resetIfExpired() {
            if (isWindowExpired()) {
                count.set(0);
                windowStart = System.currentTimeMillis();
            }
        }

        public int incrementAndGet() {
            resetIfExpired();
            return count.incrementAndGet();
        }

        public int getCount() {
            resetIfExpired();
            return count.get();
        }
    }

    // ========================================================================
    // Rate Limit Checks
    // ========================================================================

    /**
     * Checks if a new session (QR scan) is allowed for the given IP.
     *
     * @param ipAddress String the client IP address
     * @return boolean true if the request is allowed
     */
    public boolean isSessionAllowed(String ipAddress) {
        if (!enabled || ipAddress == null) {
            return true;
        }

        RateLimitEntry entry = sessionLimits.computeIfAbsent(ipAddress, k -> new RateLimitEntry());
        int count = entry.getCount();

        if (count >= maxSessionsPerIpPerHour) {
            logger.warn("Rate limit exceeded for sessions from IP: {}", maskIp(ipAddress));
            return false;
        }

        return true;
    }

    /**
     * Records a session request from the given IP.
     *
     * @param ipAddress String the client IP address
     * @return int the current count for this IP
     */
    public int recordSession(String ipAddress) {
        if (!enabled || ipAddress == null) {
            return 0;
        }

        RateLimitEntry entry = sessionLimits.computeIfAbsent(ipAddress, k -> new RateLimitEntry());
        return entry.incrementAndGet();
    }

    /**
     * Checks if a form submission is allowed for the given IP.
     *
     * @param ipAddress String the client IP address
     * @return boolean true if the submission is allowed
     */
    public boolean isSubmissionAllowed(String ipAddress) {
        if (!enabled || ipAddress == null) {
            return true;
        }

        RateLimitEntry entry = submissionLimits.computeIfAbsent(ipAddress, k -> new RateLimitEntry());
        int count = entry.getCount();

        if (count >= maxSubmissionsPerIpPerHour) {
            logger.warn("Rate limit exceeded for submissions from IP: {}", maskIp(ipAddress));
            return false;
        }

        return true;
    }

    /**
     * Records a submission from the given IP.
     *
     * @param ipAddress String the client IP address
     * @return int the current count for this IP
     */
    public int recordSubmission(String ipAddress) {
        if (!enabled || ipAddress == null) {
            return 0;
        }

        RateLimitEntry entry = submissionLimits.computeIfAbsent(ipAddress, k -> new RateLimitEntry());
        return entry.incrementAndGet();
    }

    /**
     * Gets the remaining sessions allowed for an IP.
     *
     * @param ipAddress String the client IP address
     * @return int the number of remaining sessions allowed
     */
    public int getRemainingSessionsForIp(String ipAddress) {
        if (!enabled || ipAddress == null) {
            return Integer.MAX_VALUE;
        }

        RateLimitEntry entry = sessionLimits.get(ipAddress);
        if (entry == null) {
            return maxSessionsPerIpPerHour;
        }

        return Math.max(0, maxSessionsPerIpPerHour - entry.getCount());
    }

    /**
     * Gets the remaining submissions allowed for an IP.
     *
     * @param ipAddress String the client IP address
     * @return int the number of remaining submissions allowed
     */
    public int getRemainingSubmissionsForIp(String ipAddress) {
        if (!enabled || ipAddress == null) {
            return Integer.MAX_VALUE;
        }

        RateLimitEntry entry = submissionLimits.get(ipAddress);
        if (entry == null) {
            return maxSubmissionsPerIpPerHour;
        }

        return Math.max(0, maxSubmissionsPerIpPerHour - entry.getCount());
    }

    // ========================================================================
    // Cache Management
    // ========================================================================

    /**
     * Cleans up expired entries from the cache.
     * Should be called periodically.
     */
    public void cleanupExpiredEntries() {
        sessionLimits.entrySet().removeIf(e -> e.getValue().isWindowExpired());
        submissionLimits.entrySet().removeIf(e -> e.getValue().isWindowExpired());
        logger.debug("Cleaned up expired rate limit entries");
    }

    /**
     * Clears all rate limit entries.
     * Use with caution - mainly for testing.
     */
    public void clearAll() {
        sessionLimits.clear();
        submissionLimits.clear();
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    /**
     * Masks an IP address for logging (privacy).
     */
    private String maskIp(String ipAddress) {
        if (ipAddress == null) {
            return "unknown";
        }
        // For IPv4, mask last octet
        if (ipAddress.contains(".")) {
            int lastDot = ipAddress.lastIndexOf('.');
            return ipAddress.substring(0, lastDot) + ".***";
        }
        // For IPv6, just show first segment
        if (ipAddress.contains(":")) {
            int firstColon = ipAddress.indexOf(':');
            return ipAddress.substring(0, firstColon) + ":***";
        }
        return "***";
    }

    // ========================================================================
    // Configuration
    // ========================================================================

    public void setMaxSessionsPerIpPerHour(int maxSessionsPerIpPerHour) {
        this.maxSessionsPerIpPerHour = maxSessionsPerIpPerHour;
    }

    public void setMaxSubmissionsPerIpPerHour(int maxSubmissionsPerIpPerHour) {
        this.maxSubmissionsPerIpPerHour = maxSubmissionsPerIpPerHour;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getMaxSessionsPerIpPerHour() {
        return maxSessionsPerIpPerHour;
    }

    public int getMaxSubmissionsPerIpPerHour() {
        return maxSubmissionsPerIpPerHour;
    }
}
