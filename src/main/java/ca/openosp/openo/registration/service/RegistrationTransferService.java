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

import ca.openosp.openo.commn.dao.DemographicDao;
import ca.openosp.openo.commn.dao.DemographicExtDao;
import ca.openosp.openo.commn.model.Demographic;
import ca.openosp.openo.commn.model.DemographicExt;
import ca.openosp.openo.registration.dao.PatientRegistrationQueueDao;
import ca.openosp.openo.registration.model.PatientRegistrationQueue;
import ca.openosp.openo.registration.model.PatientRegistrationQueue.RegistrationStatus;
import ca.openosp.openo.utility.SpringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Service for transferring approved registrations to the demographic table.
 *
 * <p>When staff approves a registration, this service creates the actual
 * patient record in the demographic table and updates the queue entry.</p>
 *
 * <p>Part of the Patient Self-Registration Module - a standalone commercial module
 * for OpenO EMR that enables patients to self-register via QR code scanning.</p>
 *
 * @since 2025-12-29
 */
@Service
@Transactional
public class RegistrationTransferService {

    private static final Logger logger = LogManager.getLogger(RegistrationTransferService.class);

    @Autowired
    private PatientRegistrationQueueDao patientRegistrationQueueDao;

    private DemographicDao demographicDao;
    private DemographicExtDao demographicExtDao;

    // ========================================================================
    // Lazy initialization for DAOs (to avoid circular dependencies)
    // ========================================================================

    private DemographicDao getDemographicDao() {
        if (demographicDao == null) {
            demographicDao = SpringUtils.getBean(DemographicDao.class);
        }
        return demographicDao;
    }

    private DemographicExtDao getDemographicExtDao() {
        if (demographicExtDao == null) {
            demographicExtDao = SpringUtils.getBean(DemographicExtDao.class);
        }
        return demographicExtDao;
    }

    // ========================================================================
    // Transfer Result Class
    // ========================================================================

    /**
     * Result of a transfer operation.
     */
    public static class TransferResult {
        private final boolean success;
        private final Integer demographicNo;
        private final String errorMessage;

        private TransferResult(boolean success, Integer demographicNo, String errorMessage) {
            this.success = success;
            this.demographicNo = demographicNo;
            this.errorMessage = errorMessage;
        }

        public static TransferResult success(Integer demographicNo) {
            return new TransferResult(true, demographicNo, null);
        }

        public static TransferResult failure(String errorMessage) {
            return new TransferResult(false, null, errorMessage);
        }

        public boolean isSuccess() {
            return success;
        }

        public Integer getDemographicNo() {
            return demographicNo;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * Potential duplicate match for display.
     */
    public static class DuplicateMatch {
        private final Integer demographicNo;
        private final String lastName;
        private final String firstName;
        private final String dateOfBirth;
        private final String hin;
        private final double matchScore;

        public DuplicateMatch(Integer demographicNo, String lastName, String firstName,
                              String dateOfBirth, String hin, double matchScore) {
            this.demographicNo = demographicNo;
            this.lastName = lastName;
            this.firstName = firstName;
            this.dateOfBirth = dateOfBirth;
            this.hin = hin;
            this.matchScore = matchScore;
        }

        public Integer getDemographicNo() { return demographicNo; }
        public String getLastName() { return lastName; }
        public String getFirstName() { return firstName; }
        public String getDateOfBirth() { return dateOfBirth; }
        public String getHin() { return hin; }
        public double getMatchScore() { return matchScore; }
    }

    // ========================================================================
    // Transfer Operations
    // ========================================================================

    /**
     * Transfers an approved registration to the demographic table.
     *
     * @param queueId Integer the registration queue ID
     * @param providerNo String the assigned provider number
     * @param reviewerProviderNo String the provider who approved
     * @return TransferResult with success status and demographic_no
     */
    public TransferResult transferToDemographic(Integer queueId, String providerNo, String reviewerProviderNo) {
        PatientRegistrationQueue queue = patientRegistrationQueueDao.find(queueId);

        if (queue == null) {
            return TransferResult.failure("Registration not found");
        }

        if (!RegistrationStatus.PENDING.equals(queue.getStatus())) {
            return TransferResult.failure("Registration is not in pending status");
        }

        try {
            // Create demographic record
            Demographic demographic = createDemographicFromQueue(queue, providerNo);

            // Persist demographic
            getDemographicDao().save(demographic);
            Integer demographicNo = demographic.getDemographicNo();

            logger.info("Created demographic {} from registration queue {}", demographicNo, queueId);

            // Create demographic extensions (cell phone, etc.)
            createDemographicExtensions(queue, demographicNo);

            // Update queue entry
            queue.setStatus(RegistrationStatus.APPROVED);
            queue.setReviewedAt(new Date());
            queue.setReviewedBy(reviewerProviderNo);
            queue.setDemographicNo(demographicNo);
            queue.setLastUpdateUser(reviewerProviderNo);
            patientRegistrationQueueDao.merge(queue);

            return TransferResult.success(demographicNo);

        } catch (Exception e) {
            logger.error("Failed to transfer registration {} to demographic", queueId, e);
            return TransferResult.failure("Failed to create patient record: " + e.getMessage());
        }
    }

    /**
     * Creates a Demographic entity from queue data.
     */
    private Demographic createDemographicFromQueue(PatientRegistrationQueue queue, String providerNo) {
        Demographic demographic = new Demographic();

        // Basic info
        demographic.setFirstName(toUpperCase(queue.getFirstName()));
        demographic.setLastName(toUpperCase(queue.getLastName()));
        demographic.setTitle(queue.getTitle());

        // Date of birth
        demographic.setYearOfBirth(queue.getYearOfBirth());
        demographic.setMonthOfBirth(queue.getMonthOfBirth());
        demographic.setDateOfBirth(queue.getDateOfBirth());

        // Gender/Sex
        demographic.setSex(queue.getSex());

        // Contact
        demographic.setAddress(queue.getAddress());
        demographic.setCity(queue.getCity());
        demographic.setProvince(queue.getProvince());
        demographic.setPostal(formatPostalCode(queue.getPostal()));
        demographic.setPhone(formatPhone(queue.getPhone()));
        demographic.setPhone2(formatPhone(queue.getPhone2()));
        demographic.setEmail(queue.getEmail());

        // Health Card
        demographic.setHin(queue.getHin());
        demographic.setVer(queue.getVer());
        demographic.setHcType(queue.getHcType());
        demographic.setHcRenewDate(queue.getHcRenewDate());

        // Languages
        demographic.setOfficialLanguage(queue.getOfficialLang());
        demographic.setSpokenLanguage(queue.getSpokenLang());

        // Clinic assignments
        demographic.setProviderNo(providerNo);
        demographic.setPatientStatus("AC"); // Active
        demographic.setDateJoined(new Date());

        // Consent
        if (Boolean.TRUE.equals(queue.getConsentEmail())) {
            demographic.setConsentToUseEmailForCare(true);
        }

        return demographic;
    }

    /**
     * Creates demographic extensions for additional data.
     */
    private void createDemographicExtensions(PatientRegistrationQueue queue, Integer demographicNo) {
        // Cell phone
        if (StringUtils.isNotBlank(queue.getCellPhone())) {
            createDemographicExt(demographicNo, "demo_cell", formatPhone(queue.getCellPhone()));
        }

        // Emergency contact name
        if (StringUtils.isNotBlank(queue.getEmergencyContactName())) {
            createDemographicExt(demographicNo, "emergencyContactName", queue.getEmergencyContactName());
        }

        // Emergency contact phone
        if (StringUtils.isNotBlank(queue.getEmergencyContactPhone())) {
            createDemographicExt(demographicNo, "emergencyContactPhone", formatPhone(queue.getEmergencyContactPhone()));
        }

        // Emergency contact relationship
        if (StringUtils.isNotBlank(queue.getEmergencyContactRelationship())) {
            createDemographicExt(demographicNo, "emergencyContactRelationship", queue.getEmergencyContactRelationship());
        }

        // Gender (if different from sex)
        if (StringUtils.isNotBlank(queue.getGender())) {
            createDemographicExt(demographicNo, "gender", queue.getGender());
        }

        // Pronouns
        if (StringUtils.isNotBlank(queue.getPronoun())) {
            createDemographicExt(demographicNo, "pronoun", queue.getPronoun());
        }

        // Preferred name
        if (StringUtils.isNotBlank(queue.getPreferredName())) {
            createDemographicExt(demographicNo, "preferredName", queue.getPreferredName());
        }

        // Middle names
        if (StringUtils.isNotBlank(queue.getMiddleNames())) {
            createDemographicExt(demographicNo, "middleNames", queue.getMiddleNames());
        }

        // Country of origin
        if (StringUtils.isNotBlank(queue.getCountryOfOrigin())) {
            createDemographicExt(demographicNo, "countryOfOrigin", queue.getCountryOfOrigin());
        }
    }

    /**
     * Creates a single demographic extension.
     */
    private void createDemographicExt(Integer demographicNo, String key, String value) {
        try {
            DemographicExt ext = new DemographicExt();
            ext.setDemographicNo(demographicNo);
            ext.setKey(key);
            ext.setValue(value);
            ext.setDateCreated(new Date());
            getDemographicExtDao().persist(ext);
        } catch (Exception e) {
            logger.warn("Failed to create demographic extension {} for demographic {}", key, demographicNo, e);
        }
    }

    // ========================================================================
    // Duplicate Detection
    // ========================================================================

    /**
     * Checks for potential duplicate patients in the demographic table.
     *
     * @param queue PatientRegistrationQueue the registration to check
     * @return List of DuplicateMatch with potential matches
     */
    public List<DuplicateMatch> checkForDuplicates(PatientRegistrationQueue queue) {
        List<DuplicateMatch> matches = new ArrayList<>();

        // Check by HIN first (most definitive match)
        if (StringUtils.isNotBlank(queue.getHin())) {
            List<Demographic> hinMatches = getDemographicDao().findByHin(queue.getHin(), "", 100);
            for (Demographic d : hinMatches) {
                matches.add(new DuplicateMatch(
                        d.getDemographicNo(),
                        d.getLastName(),
                        d.getFirstName(),
                        formatDob(d),
                        d.getHin(),
                        1.0 // Exact HIN match
                ));
            }
        }

        // Check by name + DOB
        if (StringUtils.isNotBlank(queue.getLastName()) &&
                StringUtils.isNotBlank(queue.getYearOfBirth())) {

            // Build Calendar from registration queue DOB parts
            Calendar dob = Calendar.getInstance();
            try {
                int year = Integer.parseInt(queue.getYearOfBirth());
                int month = StringUtils.isNotBlank(queue.getMonthOfBirth()) ?
                        Integer.parseInt(queue.getMonthOfBirth()) - 1 : 0; // Calendar months are 0-based
                int day = StringUtils.isNotBlank(queue.getDateOfBirth()) ?
                        Integer.parseInt(queue.getDateOfBirth()) : 1;
                dob.set(year, month, day, 0, 0, 0);
                dob.set(Calendar.MILLISECOND, 0);
            } catch (NumberFormatException e) {
                logger.warn("Invalid DOB format in queue {}", queue.getId());
                dob = null;
            }

            List<Demographic> nameMatches = dob != null ?
                    getDemographicDao().findByLastNameAndDob(queue.getLastName(), dob) :
                    new ArrayList<>();

            for (Demographic d : nameMatches) {
                // Skip if already found by HIN
                boolean alreadyFound = matches.stream()
                        .anyMatch(m -> m.getDemographicNo().equals(d.getDemographicNo()));

                if (!alreadyFound) {
                    double score = calculateMatchScore(queue, d);
                    if (score >= 0.8) { // 80% match threshold
                        matches.add(new DuplicateMatch(
                                d.getDemographicNo(),
                                d.getLastName(),
                                d.getFirstName(),
                                formatDob(d),
                                d.getHin(),
                                score
                        ));
                    }
                }
            }
        }

        return matches;
    }

    /**
     * Calculates a match score between a registration and existing demographic.
     */
    private double calculateMatchScore(PatientRegistrationQueue queue, Demographic demo) {
        double score = 0.0;
        int factors = 0;

        // Last name match
        if (StringUtils.isNotBlank(queue.getLastName()) && StringUtils.isNotBlank(demo.getLastName())) {
            if (queue.getLastName().equalsIgnoreCase(demo.getLastName())) {
                score += 0.25;
            }
            factors++;
        }

        // First name match
        if (StringUtils.isNotBlank(queue.getFirstName()) && StringUtils.isNotBlank(demo.getFirstName())) {
            if (queue.getFirstName().equalsIgnoreCase(demo.getFirstName())) {
                score += 0.25;
            }
            factors++;
        }

        // DOB match
        if (StringUtils.isNotBlank(queue.getYearOfBirth()) && StringUtils.isNotBlank(demo.getYearOfBirth())) {
            boolean yearMatch = queue.getYearOfBirth().equals(demo.getYearOfBirth());
            boolean monthMatch = queue.getMonthOfBirth() != null &&
                    queue.getMonthOfBirth().equals(demo.getMonthOfBirth());
            boolean dayMatch = queue.getDateOfBirth() != null &&
                    queue.getDateOfBirth().equals(demo.getDateOfBirth());

            if (yearMatch && monthMatch && dayMatch) {
                score += 0.3;
            } else if (yearMatch && monthMatch) {
                score += 0.2;
            } else if (yearMatch) {
                score += 0.1;
            }
            factors++;
        }

        // Phone match
        if (StringUtils.isNotBlank(queue.getCellPhone()) && StringUtils.isNotBlank(demo.getPhone())) {
            String queuePhone = queue.getCellPhone().replaceAll("[^0-9]", "");
            String demoPhone = demo.getPhone().replaceAll("[^0-9]", "");
            if (queuePhone.equals(demoPhone)) {
                score += 0.1;
            }
            factors++;
        }

        // Email match
        if (StringUtils.isNotBlank(queue.getEmail()) && StringUtils.isNotBlank(demo.getEmail())) {
            if (queue.getEmail().equalsIgnoreCase(demo.getEmail())) {
                score += 0.1;
            }
            factors++;
        }

        return factors > 0 ? score : 0.0;
    }

    // ========================================================================
    // Rejection
    // ========================================================================

    /**
     * Rejects a registration with a reason.
     *
     * @param queueId Integer the registration queue ID
     * @param reason String the rejection reason
     * @param reviewerProviderNo String the provider who rejected
     * @return boolean true if successfully rejected
     */
    public boolean rejectRegistration(Integer queueId, String reason, String reviewerProviderNo) {
        PatientRegistrationQueue queue = patientRegistrationQueueDao.find(queueId);

        if (queue == null || !RegistrationStatus.PENDING.equals(queue.getStatus())) {
            return false;
        }

        queue.setStatus(RegistrationStatus.REJECTED);
        queue.setReviewedAt(new Date());
        queue.setReviewedBy(reviewerProviderNo);
        queue.setRejectionReason(reason);
        queue.setLastUpdateUser(reviewerProviderNo);

        patientRegistrationQueueDao.merge(queue);
        logger.info("Rejected registration {} with reason: {}", queueId, reason);

        return true;
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private String toUpperCase(String value) {
        return value != null ? value.toUpperCase() : null;
    }

    private String formatPhone(String phone) {
        if (StringUtils.isBlank(phone)) {
            return phone;
        }
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() == 10) {
            return digits.substring(0, 3) + "-" + digits.substring(3, 6) + "-" + digits.substring(6);
        }
        return phone;
    }

    private String formatPostalCode(String postal) {
        if (StringUtils.isBlank(postal)) {
            return postal;
        }
        String cleaned = postal.replaceAll("[\\s\\-]", "").toUpperCase();
        if (cleaned.length() == 6) {
            return cleaned.substring(0, 3) + " " + cleaned.substring(3);
        }
        return postal;
    }

    private String formatDob(Demographic d) {
        if (d.getYearOfBirth() == null) {
            return "";
        }
        return String.format("%s-%s-%s",
                d.getYearOfBirth(),
                StringUtils.defaultString(d.getMonthOfBirth(), "??"),
                StringUtils.defaultString(d.getDateOfBirth(), "??"));
    }

    // ========================================================================
    // Setters for Spring injection
    // ========================================================================

    public void setPatientRegistrationQueueDao(PatientRegistrationQueueDao patientRegistrationQueueDao) {
        this.patientRegistrationQueueDao = patientRegistrationQueueDao;
    }
}
