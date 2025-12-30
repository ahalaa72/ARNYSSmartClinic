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
import java.util.Date;

/**
 * Entity representing a pending patient registration in the queue.
 *
 * <p>This entity stores patient information submitted through the self-registration
 * form. Records remain in the queue until approved (transferred to demographic table),
 * rejected, or expired.</p>
 *
 * <p>Part of the Patient Self-Registration Module - a standalone commercial module
 * for OpenO EMR that enables patients to self-register via QR code scanning.</p>
 *
 * @since 2025-12-29
 */
@Entity
@Table(name = "patient_registration_queue")
public class PatientRegistrationQueue implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Registration status values.
     */
    public enum RegistrationStatus {
        /** Registration submitted, awaiting staff review */
        PENDING("pending"),
        /** Registration approved, patient record created */
        APPROVED("approved"),
        /** Registration rejected by staff */
        REJECTED("rejected"),
        /** Registration expired (token timeout) */
        EXPIRED("expired");

        private final String value;

        RegistrationStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static RegistrationStatus fromValue(String value) {
            for (RegistrationStatus status : values()) {
                if (status.value.equalsIgnoreCase(value)) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Unknown status: " + value);
        }
    }

    /**
     * HIN validation status values.
     */
    public enum HinValidationStatus {
        VALID("valid"),
        INVALID("invalid"),
        NOT_VALIDATED("not_validated");

        private final String value;

        HinValidationStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static HinValidationStatus fromValue(String value) {
            for (HinValidationStatus status : values()) {
                if (status.value.equalsIgnoreCase(value)) {
                    return status;
                }
            }
            return NOT_VALIDATED;
        }
    }

    /**
     * Duplicate check status values.
     */
    public enum DuplicateCheckStatus {
        CLEAR("clear"),
        POTENTIAL_DUPLICATE("potential_duplicate"),
        CONFIRMED_DUPLICATE("confirmed_duplicate");

        private final String value;

        DuplicateCheckStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static DuplicateCheckStatus fromValue(String value) {
            for (DuplicateCheckStatus status : values()) {
                if (status.value.equalsIgnoreCase(value)) {
                    return status;
                }
            }
            return CLEAR;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // ========================================================================
    // Registration Metadata
    // ========================================================================

    @Column(name = "registration_token", length = 64, nullable = false, unique = true)
    private String registrationToken;

    @Column(name = "facility_id")
    private Integer facilityId = 1;

    @Column(name = "status", length = 20)
    @Enumerated(EnumType.STRING)
    private RegistrationStatus status = RegistrationStatus.PENDING;

    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Column(name = "expires_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date expiresAt;

    @Column(name = "submitted_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date submittedAt;

    @Column(name = "reviewed_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date reviewedAt;

    @Column(name = "reviewed_by", length = 6)
    private String reviewedBy;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    // ========================================================================
    // Patient Demographics
    // ========================================================================

    @Column(name = "first_name", length = 30)
    private String firstName;

    @Column(name = "last_name", length = 30)
    private String lastName;

    @Column(name = "middle_names", length = 100)
    private String middleNames;

    @Column(name = "pref_name", length = 30)
    private String preferredName;

    @Column(name = "title", length = 10)
    private String title;

    @Column(name = "year_of_birth", length = 4)
    private String yearOfBirth;

    @Column(name = "month_of_birth", length = 2)
    private String monthOfBirth;

    @Column(name = "date_of_birth", length = 2)
    private String dateOfBirth;

    @Column(name = "sex", length = 1)
    private String sex;

    @Column(name = "gender", length = 25)
    private String gender;

    @Column(name = "pronoun", length = 25)
    private String pronoun;

    // ========================================================================
    // Contact Information
    // ========================================================================

    @Column(name = "address", length = 60)
    private String address;

    @Column(name = "city", length = 50)
    private String city;

    @Column(name = "province", length = 20)
    private String province;

    @Column(name = "postal", length = 9)
    private String postal;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "phone2", length = 20)
    private String phone2;

    @Column(name = "cell_phone", length = 20)
    private String cellPhone;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "consent_email")
    private Boolean consentEmail = false;

    // ========================================================================
    // Health Card Information
    // ========================================================================

    @Column(name = "hin", length = 20)
    private String hin;

    @Column(name = "ver", length = 3)
    private String ver;

    @Column(name = "hc_type", length = 20)
    private String hcType;

    @Column(name = "hc_renew_date")
    @Temporal(TemporalType.DATE)
    private Date hcRenewDate;

    // ========================================================================
    // Additional Information
    // ========================================================================

    @Column(name = "official_lang", length = 60)
    private String officialLang;

    @Column(name = "spoken_lang", length = 60)
    private String spokenLang;

    @Column(name = "country_of_origin", length = 4)
    private String countryOfOrigin;

    @Column(name = "emergency_contact_name", length = 60)
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", length = 20)
    private String emergencyContactPhone;

    @Column(name = "emergency_contact_relationship", length = 30)
    private String emergencyContactRelationship;

    // ========================================================================
    // Validation Results
    // ========================================================================

    @Column(name = "hin_validation_status", length = 20)
    @Enumerated(EnumType.STRING)
    private HinValidationStatus hinValidationStatus = HinValidationStatus.NOT_VALIDATED;

    @Column(name = "hin_validation_error", length = 255)
    private String hinValidationError;

    @Column(name = "duplicate_check_status", length = 30)
    @Enumerated(EnumType.STRING)
    private DuplicateCheckStatus duplicateCheckStatus = DuplicateCheckStatus.CLEAR;

    @Column(name = "duplicate_warning", columnDefinition = "TEXT")
    private String duplicateWarning;

    @Column(name = "potential_duplicate_ids", length = 255)
    private String potentialDuplicateIds;

    // ========================================================================
    // Final Result
    // ========================================================================

    @Column(name = "demographic_no")
    private Integer demographicNo;

    // ========================================================================
    // Audit Fields
    // ========================================================================

    @Column(name = "last_update_user", length = 6)
    private String lastUpdateUser;

    @Column(name = "last_update_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastUpdateDate;

    // ========================================================================
    // Lifecycle Callbacks
    // ========================================================================

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = new Date();
        }
        lastUpdateDate = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdateDate = new Date();
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

    public String getRegistrationToken() {
        return registrationToken;
    }

    public void setRegistrationToken(String registrationToken) {
        this.registrationToken = registrationToken;
    }

    public Integer getFacilityId() {
        return facilityId;
    }

    public void setFacilityId(Integer facilityId) {
        this.facilityId = facilityId;
    }

    public RegistrationStatus getStatus() {
        return status;
    }

    public void setStatus(RegistrationStatus status) {
        this.status = status;
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

    public Date getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Date submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Date getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Date reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getMiddleNames() {
        return middleNames;
    }

    public void setMiddleNames(String middleNames) {
        this.middleNames = middleNames;
    }

    public String getPreferredName() {
        return preferredName;
    }

    public void setPreferredName(String preferredName) {
        this.preferredName = preferredName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getYearOfBirth() {
        return yearOfBirth;
    }

    public void setYearOfBirth(String yearOfBirth) {
        this.yearOfBirth = yearOfBirth;
    }

    public String getMonthOfBirth() {
        return monthOfBirth;
    }

    public void setMonthOfBirth(String monthOfBirth) {
        this.monthOfBirth = monthOfBirth;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPronoun() {
        return pronoun;
    }

    public void setPronoun(String pronoun) {
        this.pronoun = pronoun;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getPostal() {
        return postal;
    }

    public void setPostal(String postal) {
        this.postal = postal;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPhone2() {
        return phone2;
    }

    public void setPhone2(String phone2) {
        this.phone2 = phone2;
    }

    public String getCellPhone() {
        return cellPhone;
    }

    public void setCellPhone(String cellPhone) {
        this.cellPhone = cellPhone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getConsentEmail() {
        return consentEmail;
    }

    public void setConsentEmail(Boolean consentEmail) {
        this.consentEmail = consentEmail;
    }

    public String getHin() {
        return hin;
    }

    public void setHin(String hin) {
        this.hin = hin;
    }

    public String getVer() {
        return ver;
    }

    public void setVer(String ver) {
        this.ver = ver;
    }

    public String getHcType() {
        return hcType;
    }

    public void setHcType(String hcType) {
        this.hcType = hcType;
    }

    public Date getHcRenewDate() {
        return hcRenewDate;
    }

    public void setHcRenewDate(Date hcRenewDate) {
        this.hcRenewDate = hcRenewDate;
    }

    public String getOfficialLang() {
        return officialLang;
    }

    public void setOfficialLang(String officialLang) {
        this.officialLang = officialLang;
    }

    public String getSpokenLang() {
        return spokenLang;
    }

    public void setSpokenLang(String spokenLang) {
        this.spokenLang = spokenLang;
    }

    public String getCountryOfOrigin() {
        return countryOfOrigin;
    }

    public void setCountryOfOrigin(String countryOfOrigin) {
        this.countryOfOrigin = countryOfOrigin;
    }

    public String getEmergencyContactName() {
        return emergencyContactName;
    }

    public void setEmergencyContactName(String emergencyContactName) {
        this.emergencyContactName = emergencyContactName;
    }

    public String getEmergencyContactPhone() {
        return emergencyContactPhone;
    }

    public void setEmergencyContactPhone(String emergencyContactPhone) {
        this.emergencyContactPhone = emergencyContactPhone;
    }

    public String getEmergencyContactRelationship() {
        return emergencyContactRelationship;
    }

    public void setEmergencyContactRelationship(String emergencyContactRelationship) {
        this.emergencyContactRelationship = emergencyContactRelationship;
    }

    public HinValidationStatus getHinValidationStatus() {
        return hinValidationStatus;
    }

    public void setHinValidationStatus(HinValidationStatus hinValidationStatus) {
        this.hinValidationStatus = hinValidationStatus;
    }

    public String getHinValidationError() {
        return hinValidationError;
    }

    public void setHinValidationError(String hinValidationError) {
        this.hinValidationError = hinValidationError;
    }

    public DuplicateCheckStatus getDuplicateCheckStatus() {
        return duplicateCheckStatus;
    }

    public void setDuplicateCheckStatus(DuplicateCheckStatus duplicateCheckStatus) {
        this.duplicateCheckStatus = duplicateCheckStatus;
    }

    public String getDuplicateWarning() {
        return duplicateWarning;
    }

    public void setDuplicateWarning(String duplicateWarning) {
        this.duplicateWarning = duplicateWarning;
    }

    public String getPotentialDuplicateIds() {
        return potentialDuplicateIds;
    }

    public void setPotentialDuplicateIds(String potentialDuplicateIds) {
        this.potentialDuplicateIds = potentialDuplicateIds;
    }

    public Integer getDemographicNo() {
        return demographicNo;
    }

    public void setDemographicNo(Integer demographicNo) {
        this.demographicNo = demographicNo;
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
    // Utility Methods
    // ========================================================================

    /**
     * Returns the full date of birth as a formatted string (YYYY-MM-DD).
     *
     * @return String the formatted date of birth, or null if components are missing
     */
    public String getFullDateOfBirth() {
        if (yearOfBirth == null || monthOfBirth == null || dateOfBirth == null) {
            return null;
        }
        return String.format("%s-%s-%s", yearOfBirth, monthOfBirth, dateOfBirth);
    }

    /**
     * Returns the full name (First Last).
     *
     * @return String the formatted full name
     */
    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        if (firstName != null) {
            sb.append(firstName);
        }
        if (lastName != null) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(lastName);
        }
        return sb.toString();
    }

    /**
     * Returns the full name with title (Mr. John Doe).
     *
     * @return String the formatted full name with title
     */
    public String getFullNameWithTitle() {
        StringBuilder sb = new StringBuilder();
        if (title != null && !title.isEmpty()) {
            sb.append(title).append(" ");
        }
        sb.append(getFullName());
        return sb.toString();
    }

    /**
     * Checks if this registration is still valid (not expired).
     *
     * @return boolean true if the registration has not expired
     */
    public boolean isValid() {
        if (expiresAt == null) {
            return true;
        }
        return new Date().before(expiresAt);
    }

    /**
     * Checks if this registration is pending review.
     *
     * @return boolean true if status is PENDING
     */
    public boolean isPending() {
        return Status.PENDING.equals(status);
    }

    /**
     * Checks if this registration has been approved.
     *
     * @return boolean true if status is APPROVED
     */
    public boolean isApproved() {
        return Status.APPROVED.equals(status);
    }

    /**
     * Checks if this registration has been rejected.
     *
     * @return boolean true if status is REJECTED
     */
    public boolean isRejected() {
        return Status.REJECTED.equals(status);
    }

    /**
     * Checks if this registration has a potential duplicate warning.
     *
     * @return boolean true if duplicate check found potential matches
     */
    public boolean hasDuplicateWarning() {
        return DuplicateCheckStatus.POTENTIAL_DUPLICATE.equals(duplicateCheckStatus)
                || DuplicateCheckStatus.CONFIRMED_DUPLICATE.equals(duplicateCheckStatus);
    }

    /**
     * Checks if the HIN validation failed.
     *
     * @return boolean true if HIN validation failed
     */
    public boolean hasHinValidationError() {
        return HinValidationStatus.INVALID.equals(hinValidationStatus);
    }

    /**
     * Returns masked HIN for display (e.g., ****1234).
     *
     * @return String the masked HIN, or empty string if HIN is null
     */
    public String getMaskedHin() {
        if (hin == null || hin.length() < 4) {
            return "";
        }
        return "****" + hin.substring(hin.length() - 4);
    }

    @Override
    public String toString() {
        return "PatientRegistrationQueue{" +
                "id=" + id +
                ", registrationToken='" + (registrationToken != null ? registrationToken.substring(0, 8) + "..." : null) + '\'' +
                ", status=" + status +
                ", fullName='" + getFullName() + '\'' +
                ", hin='" + getMaskedHin() + '\'' +
                '}';
    }
}
