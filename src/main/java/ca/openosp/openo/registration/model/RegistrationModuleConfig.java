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
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Entity representing the registration module configuration per facility.
 *
 * <p>This entity stores clinic-specific settings for the self-registration module,
 * enabling multi-clinic deployments with different configurations.</p>
 *
 * <p>Part of the Patient Self-Registration Module - a standalone commercial module
 * for OpenO EMR that enables patients to self-register via QR code scanning.</p>
 *
 * @since 2025-12-29
 */
@Entity
@Table(name = "registration_module_config")
public class RegistrationModuleConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "facility_id", unique = true)
    private Integer facilityId = 1;

    // ========================================================================
    // Module Status
    // ========================================================================

    @Column(name = "module_enabled")
    private Boolean moduleEnabled = true;

    // ========================================================================
    // Token Configuration
    // ========================================================================

    @Column(name = "token_expiry_minutes")
    private Integer tokenExpiryMinutes = 30;

    @Column(name = "max_sessions_per_ip_per_hour")
    private Integer maxSessionsPerIpPerHour = 5;

    @Column(name = "max_submissions_per_ip_per_hour")
    private Integer maxSubmissionsPerIpPerHour = 5;

    // ========================================================================
    // QR Code Configuration
    // ========================================================================

    @Column(name = "qr_code_url", length = 500)
    private String qrCodeUrl;

    @Column(name = "clinic_name", length = 100)
    private String clinicName;

    @Column(name = "clinic_phone", length = 20)
    private String clinicPhone;

    @Column(name = "clinic_address", length = 255)
    private String clinicAddress;

    // ========================================================================
    // Default Values
    // ========================================================================

    @Column(name = "default_province", length = 20)
    private String defaultProvince = "ON";

    @Column(name = "default_country", length = 50)
    private String defaultCountry = "Canada";

    @Column(name = "default_language", length = 10)
    private String defaultLanguage = "en";

    // ========================================================================
    // Mandatory Fields
    // ========================================================================

    @Column(name = "mandatory_fields", columnDefinition = "TEXT")
    private String mandatoryFields;

    // ========================================================================
    // Email Configuration
    // ========================================================================

    @Column(name = "email_notifications_enabled")
    private Boolean emailNotificationsEnabled = true;

    @Column(name = "staff_notification_email", length = 100)
    private String staffNotificationEmail;

    @Column(name = "supported_languages", length = 100)
    private String supportedLanguages = "en,fr,ar,hi,zh";

    // ========================================================================
    // Branding
    // ========================================================================

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "primary_color", length = 7)
    private String primaryColor = "#007bff";

    // ========================================================================
    // Audit
    // ========================================================================

    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Column(name = "updated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    @Column(name = "last_update_user", length = 6)
    private String lastUpdateUser;

    // ========================================================================
    // Constructors
    // ========================================================================

    /**
     * Default constructor required by JPA.
     */
    public RegistrationModuleConfig() {
    }

    /**
     * Creates a new configuration for the specified facility.
     *
     * @param facilityId Integer the facility ID
     */
    public RegistrationModuleConfig(Integer facilityId) {
        this.facilityId = facilityId;
    }

    // ========================================================================
    // Lifecycle Callbacks
    // ========================================================================

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
        updatedAt = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }

    // ========================================================================
    // Business Methods
    // ========================================================================

    /**
     * Returns the list of mandatory field names.
     *
     * @return List of String field names that are required
     */
    public List<String> getMandatoryFieldsList() {
        if (mandatoryFields == null || mandatoryFields.isEmpty()) {
            return Collections.emptyList();
        }
        // Handle both JSON array format and comma-separated format
        String cleaned = mandatoryFields
                .replaceAll("[\\[\\]\"]", "")
                .trim();
        return Arrays.asList(cleaned.split("\\s*,\\s*"));
    }

    /**
     * Sets the mandatory fields from a list.
     *
     * @param fields List of String field names
     */
    public void setMandatoryFieldsList(List<String> fields) {
        if (fields == null || fields.isEmpty()) {
            this.mandatoryFields = null;
        } else {
            this.mandatoryFields = "[\"" + String.join("\",\"", fields) + "\"]";
        }
    }

    /**
     * Returns the list of supported language codes.
     *
     * @return List of String language codes (e.g., "en", "fr", "ar")
     */
    public List<String> getSupportedLanguagesList() {
        if (supportedLanguages == null || supportedLanguages.isEmpty()) {
            return Arrays.asList("en");
        }
        return Arrays.asList(supportedLanguages.split("\\s*,\\s*"));
    }

    /**
     * Checks if a specific field is mandatory.
     *
     * @param fieldName String the field name to check
     * @return boolean true if the field is mandatory
     */
    public boolean isFieldMandatory(String fieldName) {
        return getMandatoryFieldsList().contains(fieldName);
    }

    /**
     * Checks if a language is supported for emails.
     *
     * @param languageCode String the language code to check
     * @return boolean true if the language is supported
     */
    public boolean isLanguageSupported(String languageCode) {
        return getSupportedLanguagesList().contains(languageCode);
    }

    /**
     * Returns the full QR code URL, constructing it from base URL if not explicitly set.
     *
     * @param baseUrl String the application base URL
     * @return String the complete QR code URL
     */
    public String getEffectiveQrCodeUrl(String baseUrl) {
        if (qrCodeUrl != null && !qrCodeUrl.isEmpty()) {
            return qrCodeUrl;
        }
        if (baseUrl != null) {
            return baseUrl + "/registration/start";
        }
        return null;
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

    public Integer getFacilityId() {
        return facilityId;
    }

    public void setFacilityId(Integer facilityId) {
        this.facilityId = facilityId;
    }

    public Boolean getModuleEnabled() {
        return moduleEnabled;
    }

    public void setModuleEnabled(Boolean moduleEnabled) {
        this.moduleEnabled = moduleEnabled;
    }

    public Integer getTokenExpiryMinutes() {
        return tokenExpiryMinutes;
    }

    public void setTokenExpiryMinutes(Integer tokenExpiryMinutes) {
        this.tokenExpiryMinutes = tokenExpiryMinutes;
    }

    public Integer getMaxSessionsPerIpPerHour() {
        return maxSessionsPerIpPerHour;
    }

    public void setMaxSessionsPerIpPerHour(Integer maxSessionsPerIpPerHour) {
        this.maxSessionsPerIpPerHour = maxSessionsPerIpPerHour;
    }

    public Integer getMaxSubmissionsPerIpPerHour() {
        return maxSubmissionsPerIpPerHour;
    }

    public void setMaxSubmissionsPerIpPerHour(Integer maxSubmissionsPerIpPerHour) {
        this.maxSubmissionsPerIpPerHour = maxSubmissionsPerIpPerHour;
    }

    public String getQrCodeUrl() {
        return qrCodeUrl;
    }

    public void setQrCodeUrl(String qrCodeUrl) {
        this.qrCodeUrl = qrCodeUrl;
    }

    public String getClinicName() {
        return clinicName;
    }

    public void setClinicName(String clinicName) {
        this.clinicName = clinicName;
    }

    public String getClinicPhone() {
        return clinicPhone;
    }

    public void setClinicPhone(String clinicPhone) {
        this.clinicPhone = clinicPhone;
    }

    public String getClinicAddress() {
        return clinicAddress;
    }

    public void setClinicAddress(String clinicAddress) {
        this.clinicAddress = clinicAddress;
    }

    public String getDefaultProvince() {
        return defaultProvince;
    }

    public void setDefaultProvince(String defaultProvince) {
        this.defaultProvince = defaultProvince;
    }

    public String getDefaultCountry() {
        return defaultCountry;
    }

    public void setDefaultCountry(String defaultCountry) {
        this.defaultCountry = defaultCountry;
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }

    public String getMandatoryFields() {
        return mandatoryFields;
    }

    public void setMandatoryFields(String mandatoryFields) {
        this.mandatoryFields = mandatoryFields;
    }

    public Boolean getEmailNotificationsEnabled() {
        return emailNotificationsEnabled;
    }

    public void setEmailNotificationsEnabled(Boolean emailNotificationsEnabled) {
        this.emailNotificationsEnabled = emailNotificationsEnabled;
    }

    public String getStaffNotificationEmail() {
        return staffNotificationEmail;
    }

    public void setStaffNotificationEmail(String staffNotificationEmail) {
        this.staffNotificationEmail = staffNotificationEmail;
    }

    public String getSupportedLanguages() {
        return supportedLanguages;
    }

    public void setSupportedLanguages(String supportedLanguages) {
        this.supportedLanguages = supportedLanguages;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getPrimaryColor() {
        return primaryColor;
    }

    public void setPrimaryColor(String primaryColor) {
        this.primaryColor = primaryColor;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getLastUpdateUser() {
        return lastUpdateUser;
    }

    public void setLastUpdateUser(String lastUpdateUser) {
        this.lastUpdateUser = lastUpdateUser;
    }

    @Override
    public String toString() {
        return "RegistrationModuleConfig{" +
                "id=" + id +
                ", facilityId=" + facilityId +
                ", moduleEnabled=" + moduleEnabled +
                ", clinicName='" + clinicName + '\'' +
                ", defaultProvince='" + defaultProvince + '\'' +
                '}';
    }
}
