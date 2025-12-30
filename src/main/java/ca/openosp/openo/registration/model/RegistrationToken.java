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
package ca.openosp.openo.registration.model;

import javax.persistence.*;
import java.io.Serializable;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;

/**
 * Entity representing a registration session token.
 *
 * <p>Tokens are generated when a patient scans the QR code and expire after
 * a configurable duration (default 30 minutes). Each token can only be used
 * once to submit a registration.</p>
 *
 * <p>Part of the Patient Self-Registration Module - a standalone commercial module
 * for OpenO EMR that enables patients to self-register via QR code scanning.</p>
 *
 * @since 2025-12-29
 */
@Entity
@Table(name = "registration_tokens")
public class RegistrationToken implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Default token length in characters */
    private static final int DEFAULT_TOKEN_LENGTH = 64;

    /** Default expiry time in minutes */
    private static final int DEFAULT_EXPIRY_MINUTES = 30;

    /** Secure random generator for token creation */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "token", length = 64, nullable = false, unique = true)
    private String token;

    @Column(name = "facility_id")
    private Integer facilityId = 1;

    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Column(name = "expires_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date expiresAt;

    @Column(name = "used_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date usedAt;

    @Column(name = "used")
    private Boolean used = false;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "last_update_user", length = 6)
    private String lastUpdateUser;

    @Column(name = "last_update_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastUpdateDate;

    // ========================================================================
    // Constructors
    // ========================================================================

    /**
     * Default constructor required by JPA.
     */
    public RegistrationToken() {
    }

    /**
     * Creates a new registration token with default expiry (30 minutes).
     *
     * @param facilityId Integer the facility ID for multi-clinic support
     */
    public RegistrationToken(Integer facilityId) {
        this(facilityId, DEFAULT_EXPIRY_MINUTES);
    }

    /**
     * Creates a new registration token with specified expiry.
     *
     * @param facilityId Integer the facility ID for multi-clinic support
     * @param expiryMinutes int the number of minutes until token expires
     */
    public RegistrationToken(Integer facilityId, int expiryMinutes) {
        this.facilityId = facilityId;
        this.token = generateToken();
        this.createdAt = new Date();
        this.expiresAt = new Date(System.currentTimeMillis() + (expiryMinutes * 60 * 1000L));
        this.used = false;
    }

    // ========================================================================
    // Static Factory Methods
    // ========================================================================

    /**
     * Creates a new token with the specified parameters.
     *
     * @param facilityId Integer the facility ID
     * @param expiryMinutes int minutes until expiry
     * @param ipAddress String the client IP address
     * @param userAgent String the client user agent
     * @return RegistrationToken the newly created token
     */
    public static RegistrationToken create(Integer facilityId, int expiryMinutes,
                                            String ipAddress, String userAgent) {
        RegistrationToken token = new RegistrationToken(facilityId, expiryMinutes);
        token.setIpAddress(ipAddress);
        token.setUserAgent(truncateUserAgent(userAgent));
        return token;
    }

    /**
     * Generates a cryptographically secure random token.
     *
     * @return String a 64-character URL-safe token
     */
    public static String generateToken() {
        return generateToken(DEFAULT_TOKEN_LENGTH);
    }

    /**
     * Generates a cryptographically secure random token of specified length.
     *
     * @param length int the desired token length
     * @return String a URL-safe token of the specified length
     */
    public static String generateToken(int length) {
        // Generate random bytes (3 bytes = 4 base64 chars)
        int byteLength = (int) Math.ceil(length * 0.75);
        byte[] randomBytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(randomBytes);

        // Encode to URL-safe Base64 and trim to exact length
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        return encoded.substring(0, Math.min(length, encoded.length()));
    }

    // ========================================================================
    // Lifecycle Callbacks
    // ========================================================================

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = new Date();
        }
        if (token == null) {
            token = generateToken();
        }
        if (expiresAt == null) {
            expiresAt = new Date(System.currentTimeMillis() + (DEFAULT_EXPIRY_MINUTES * 60 * 1000L));
        }
        lastUpdateDate = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdateDate = new Date();
    }

    // ========================================================================
    // Business Methods
    // ========================================================================

    /**
     * Checks if this token is valid (not expired and not used).
     *
     * @return boolean true if the token can still be used
     */
    public boolean isValid() {
        if (used != null && used) {
            return false;
        }
        if (expiresAt == null) {
            return false;
        }
        return new Date().before(expiresAt);
    }

    /**
     * Checks if this token has expired.
     *
     * @return boolean true if the token has expired
     */
    public boolean isExpired() {
        if (expiresAt == null) {
            return true;
        }
        return new Date().after(expiresAt);
    }

    /**
     * Marks this token as used.
     */
    public void markAsUsed() {
        this.used = true;
        this.usedAt = new Date();
    }

    /**
     * Gets the remaining time until expiry in minutes.
     *
     * @return long the remaining minutes, or 0 if expired
     */
    public long getRemainingMinutes() {
        if (expiresAt == null) {
            return 0;
        }
        long remaining = expiresAt.getTime() - System.currentTimeMillis();
        return remaining > 0 ? remaining / (60 * 1000) : 0;
    }

    /**
     * Gets the remaining time until expiry in seconds.
     *
     * @return long the remaining seconds, or 0 if expired
     */
    public long getRemainingSeconds() {
        if (expiresAt == null) {
            return 0;
        }
        long remaining = expiresAt.getTime() - System.currentTimeMillis();
        return remaining > 0 ? remaining / 1000 : 0;
    }

    // ========================================================================
    // Getters and Setters
    // ========================================================================

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Integer getFacilityId() {
        return facilityId;
    }

    public void setFacilityId(Integer facilityId) {
        this.facilityId = facilityId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Date expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Date getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(Date usedAt) {
        this.usedAt = usedAt;
    }

    public Boolean getUsed() {
        return used;
    }

    public void setUsed(Boolean used) {
        this.used = used;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = truncateUserAgent(userAgent);
    }

    public String getLastUpdateUser() {
        return lastUpdateUser;
    }

    public void setLastUpdateUser(String lastUpdateUser) {
        this.lastUpdateUser = lastUpdateUser;
    }

    public Date getLastUpdateDate() {
        return lastUpdateDate;
    }

    public void setLastUpdateDate(Date lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }

    // ========================================================================
    // Private Helper Methods
    // ========================================================================

    /**
     * Truncates user agent string to maximum allowed length.
     *
     * @param userAgent String the user agent to truncate
     * @return String the truncated user agent
     */
    private static String truncateUserAgent(String userAgent) {
        if (userAgent == null) {
            return null;
        }
        return userAgent.length() > 500 ? userAgent.substring(0, 500) : userAgent;
    }

    // ========================================================================
    // Object Methods
    // ========================================================================

    @Override
    public String toString() {
        return "RegistrationToken{" +
                "id=" + id +
                ", token='" + (token != null ? token.substring(0, 8) + "..." : null) + '\'' +
                ", facilityId=" + facilityId +
                ", used=" + used +
                ", isValid=" + isValid() +
                ", remainingMinutes=" + getRemainingMinutes() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RegistrationToken that = (RegistrationToken) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        return token != null ? token.equals(that.token) : that.token == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (token != null ? token.hashCode() : 0);
        return result;
    }
}
