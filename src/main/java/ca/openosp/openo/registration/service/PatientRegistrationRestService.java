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

import ca.openosp.openo.managers.SecurityInfoManager;
import ca.openosp.openo.registration.dao.PatientRegistrationQueueDao;
import ca.openosp.openo.registration.dao.RegistrationModuleConfigDao;
import ca.openosp.openo.registration.dao.RegistrationTokenDao;
import ca.openosp.openo.registration.model.PatientRegistrationQueue;
import ca.openosp.openo.registration.model.PatientRegistrationQueue.RegistrationStatus;
import ca.openosp.openo.registration.model.RegistrationModuleConfig;
import ca.openosp.openo.registration.model.RegistrationToken;
import ca.openosp.openo.registration.util.QRCodeGenerator;
import ca.openosp.openo.registration.util.RegistrationValidator;
import ca.openosp.openo.registration.util.RegistrationValidator.ValidationResult;
import ca.openosp.openo.utility.LoggedInInfo;
import ca.openosp.openo.utility.SpringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

/**
 * JAX-RS REST API for patient self-registration.
 *
 * <p>Provides public endpoints for patients and authenticated endpoints for staff.</p>
 *
 * <h3>Public Endpoints (No Authentication Required):</h3>
 * <ul>
 *   <li>POST /registration/validate-token - Validate a session token</li>
 *   <li>POST /registration/submit - Submit a registration</li>
 *   <li>GET /registration/config - Get form configuration</li>
 * </ul>
 *
 * <h3>Authenticated Endpoints (Staff Only):</h3>
 * <ul>
 *   <li>GET /registration/queue - List pending registrations</li>
 *   <li>GET /registration/queue/{id} - Get single registration</li>
 *   <li>PUT /registration/queue/{id}/approve - Approve registration</li>
 *   <li>PUT /registration/queue/{id}/reject - Reject registration</li>
 *   <li>GET /registration/qrcode - Get QR code for printing</li>
 * </ul>
 *
 * <p>Part of the Patient Self-Registration Module - a standalone commercial module
 * for OpenO EMR that enables patients to self-register via QR code scanning.</p>
 *
 * @since 2025-12-29
 */
@Path("/registration")
@Produces(MediaType.APPLICATION_JSON)
public class PatientRegistrationRestService {

    private static final Logger logger = LogManager.getLogger(PatientRegistrationRestService.class);

    @Context
    private HttpServletRequest request;

    // Lazy-loaded DAOs and services
    private PatientRegistrationQueueDao queueDao;
    private RegistrationTokenDao tokenDao;
    private RegistrationModuleConfigDao configDao;
    private RegistrationValidator validator;
    private QRCodeGenerator qrCodeGenerator;
    private RateLimitService rateLimitService;
    private RegistrationTransferService transferService;
    private RegistrationNotificationService notificationService;
    private SecurityInfoManager securityInfoManager;

    // ========================================================================
    // Lazy Initialization
    // ========================================================================

    private PatientRegistrationQueueDao getQueueDao() {
        if (queueDao == null) {
            queueDao = SpringUtils.getBean(PatientRegistrationQueueDao.class);
        }
        return queueDao;
    }

    private RegistrationTokenDao getTokenDao() {
        if (tokenDao == null) {
            tokenDao = SpringUtils.getBean(RegistrationTokenDao.class);
        }
        return tokenDao;
    }

    private RegistrationModuleConfigDao getConfigDao() {
        if (configDao == null) {
            configDao = SpringUtils.getBean(RegistrationModuleConfigDao.class);
        }
        return configDao;
    }

    private RegistrationValidator getValidator() {
        if (validator == null) {
            validator = SpringUtils.getBean(RegistrationValidator.class);
        }
        return validator;
    }

    private QRCodeGenerator getQrCodeGenerator() {
        if (qrCodeGenerator == null) {
            qrCodeGenerator = SpringUtils.getBean(QRCodeGenerator.class);
        }
        return qrCodeGenerator;
    }

    private RateLimitService getRateLimitService() {
        if (rateLimitService == null) {
            rateLimitService = SpringUtils.getBean(RateLimitService.class);
        }
        return rateLimitService;
    }

    private RegistrationTransferService getTransferService() {
        if (transferService == null) {
            transferService = SpringUtils.getBean(RegistrationTransferService.class);
        }
        return transferService;
    }

    private RegistrationNotificationService getNotificationService() {
        if (notificationService == null) {
            notificationService = SpringUtils.getBean(RegistrationNotificationService.class);
        }
        return notificationService;
    }

    private SecurityInfoManager getSecurityInfoManager() {
        if (securityInfoManager == null) {
            securityInfoManager = SpringUtils.getBean(SecurityInfoManager.class);
        }
        return securityInfoManager;
    }

    // ========================================================================
    // Public Endpoints (No Authentication)
    // ========================================================================

    /**
     * Validates a registration token.
     *
     * @param tokenRequest Map containing "token" key
     * @return Response with validity status and expiry time
     */
    @POST
    @Path("/validate-token")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response validateToken(Map<String, String> tokenRequest) {
        String tokenValue = tokenRequest.get("token");

        if (StringUtils.isBlank(tokenValue)) {
            return errorResponse(400, "Token is required");
        }

        RegistrationToken token = getTokenDao().findByToken(tokenValue);

        if (token == null) {
            return jsonResponse(Map.of(
                    "valid", false,
                    "error", "Token not found"
            ));
        }

        if (!token.isValid()) {
            return jsonResponse(Map.of(
                    "valid", false,
                    "error", token.isExpired() ? "Token has expired" : "Token has already been used"
            ));
        }

        RegistrationModuleConfig config = getConfigDao().findByFacilityId(token.getFacilityId());

        return jsonResponse(Map.of(
                "valid", true,
                "expires_at", token.getExpiresAt(),
                "remaining_seconds", token.getRemainingSeconds(),
                "facility_name", config != null ? config.getClinicName() : "Medical Clinic"
        ));
    }

    /**
     * Submits a patient registration.
     *
     * @param submission Map containing "token" and "patient_data"
     * @return Response with success status
     */
    @POST
    @Path("/submit")
    @Consumes(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public Response submitRegistration(Map<String, Object> submission) {
        String clientIp = getClientIp();

        // Rate limiting check
        if (!getRateLimitService().isSubmissionAllowed(clientIp)) {
            return errorResponse(429, "Too many submissions. Please try again later.");
        }

        String tokenValue = (String) submission.get("token");
        Map<String, Object> patientData = (Map<String, Object>) submission.get("patient_data");

        if (StringUtils.isBlank(tokenValue)) {
            return errorResponse(400, "Token is required");
        }

        if (patientData == null || patientData.isEmpty()) {
            return errorResponse(400, "Patient data is required");
        }

        // Validate token
        RegistrationToken token = getTokenDao().findByToken(tokenValue);

        if (token == null || !token.isValid()) {
            return errorResponse(400, "Invalid or expired token");
        }

        // Get configuration
        RegistrationModuleConfig config = getConfigDao().getOrCreateForFacility(token.getFacilityId());

        // Create queue entry
        PatientRegistrationQueue queue = new PatientRegistrationQueue();
        queue.setRegistrationToken(tokenValue);
        queue.setFacilityId(token.getFacilityId());
        queue.setStatus(RegistrationStatus.PENDING);
        queue.setExpiresAt(token.getExpiresAt());
        queue.setSubmittedAt(new Date());

        // Populate patient data
        populateQueueFromMap(queue, patientData);

        // Validate
        ValidationResult validationResult = getValidator().validate(queue, config);

        if (!validationResult.isValid()) {
            return jsonResponse(Map.of(
                    "success", false,
                    "errors", validationResult.getErrors()
            ));
        }

        // Check for duplicates
        List<RegistrationTransferService.DuplicateMatch> duplicates =
                getTransferService().checkForDuplicates(queue);

        if (!duplicates.isEmpty()) {
            queue.setDuplicateCheckStatus(PatientRegistrationQueue.DuplicateCheckStatus.POTENTIAL_DUPLICATE);
            queue.setDuplicateWarning(formatDuplicateWarning(duplicates));
            queue.setPotentialDuplicateIds(formatDuplicateIds(duplicates));
        }

        // Persist
        getQueueDao().persist(queue);

        // Mark token as used
        getTokenDao().invalidateToken(tokenValue);

        // Record rate limit
        getRateLimitService().recordSubmission(clientIp);

        // Notify staff
        try {
            getNotificationService().notifyStaffNewRegistration(queue, config);
        } catch (Exception e) {
            logger.warn("Failed to send staff notification", e);
        }

        logger.info("New registration submitted: {} (token: {}...)",
                queue.getId(), tokenValue.substring(0, 8));

        return jsonResponse(Map.of(
                "success", true,
                "queue_id", queue.getId(),
                "message", "Registration submitted successfully",
                "has_duplicate_warning", !duplicates.isEmpty()
        ));
    }

    /**
     * Gets form configuration (provinces, languages, etc.).
     *
     * @return Response with configuration data
     */
    @GET
    @Path("/config")
    public Response getFormConfig() {
        Map<String, Object> config = new HashMap<>();

        // Canadian provinces
        config.put("provinces", List.of(
                Map.of("code", "AB", "name", "Alberta"),
                Map.of("code", "BC", "name", "British Columbia"),
                Map.of("code", "MB", "name", "Manitoba"),
                Map.of("code", "NB", "name", "New Brunswick"),
                Map.of("code", "NL", "name", "Newfoundland and Labrador"),
                Map.of("code", "NS", "name", "Nova Scotia"),
                Map.of("code", "NT", "name", "Northwest Territories"),
                Map.of("code", "NU", "name", "Nunavut"),
                Map.of("code", "ON", "name", "Ontario"),
                Map.of("code", "PE", "name", "Prince Edward Island"),
                Map.of("code", "QC", "name", "Quebec"),
                Map.of("code", "SK", "name", "Saskatchewan"),
                Map.of("code", "YT", "name", "Yukon")
        ));

        // Languages
        config.put("languages", List.of(
                Map.of("code", "en", "name", "English"),
                Map.of("code", "fr", "name", "French"),
                Map.of("code", "ar", "name", "Arabic"),
                Map.of("code", "hi", "name", "Hindi"),
                Map.of("code", "zh", "name", "Mandarin")
        ));

        // Titles
        config.put("titles", List.of("", "Mr.", "Mrs.", "Ms.", "Miss", "Dr.", "Prof."));

        // Sex options
        config.put("sex_options", List.of(
                Map.of("code", "M", "name", "Male"),
                Map.of("code", "F", "name", "Female"),
                Map.of("code", "O", "name", "Other")
        ));

        // Default values
        config.put("defaults", Map.of(
                "province", "ON",
                "country", "Canada",
                "language", "en"
        ));

        return jsonResponse(config);
    }

    // ========================================================================
    // Authenticated Endpoints (Staff Only)
    // ========================================================================

    /**
     * Gets list of pending registrations.
     *
     * @return Response with list of registrations
     */
    @GET
    @Path("/queue")
    public Response getQueue() {
        if (!checkStaffPrivilege("r")) {
            return errorResponse(403, "Access denied");
        }

        List<PatientRegistrationQueue> pending = getQueueDao().findByStatus(RegistrationStatus.PENDING);

        List<Map<String, Object>> result = new ArrayList<>();
        for (PatientRegistrationQueue q : pending) {
            result.add(queueToMap(q, false));
        }

        return jsonResponse(Map.of(
                "registrations", result,
                "total", pending.size()
        ));
    }

    /**
     * Gets a single registration by ID.
     *
     * @param id the registration ID
     * @return Response with registration details
     */
    @GET
    @Path("/queue/{id}")
    public Response getQueueItem(@PathParam("id") Integer id) {
        if (!checkStaffPrivilege("r")) {
            return errorResponse(403, "Access denied");
        }

        PatientRegistrationQueue queue = getQueueDao().find(id);

        if (queue == null) {
            return errorResponse(404, "Registration not found");
        }

        return jsonResponse(queueToMap(queue, true));
    }

    /**
     * Approves a registration.
     *
     * @param id the registration ID
     * @param approvalRequest Map containing provider_no
     * @return Response with success status and demographic_no
     */
    @PUT
    @Path("/queue/{id}/approve")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response approve(@PathParam("id") Integer id, Map<String, Object> approvalRequest) {
        if (!checkStaffPrivilege("w")) {
            return errorResponse(403, "Access denied");
        }

        String providerNo = (String) approvalRequest.get("provider_no");
        String reviewerNo = getLoggedInProviderNo();

        if (StringUtils.isBlank(providerNo)) {
            providerNo = reviewerNo; // Default to reviewer
        }

        RegistrationTransferService.TransferResult result =
                getTransferService().transferToDemographic(id, providerNo, reviewerNo);

        if (!result.isSuccess()) {
            return errorResponse(400, result.getErrorMessage());
        }

        // Send patient notification
        try {
            PatientRegistrationQueue queue = getQueueDao().find(id);
            RegistrationModuleConfig config = getConfigDao().findByFacilityId(queue.getFacilityId());
            getNotificationService().sendPatientApprovalEmail(queue, config);
        } catch (Exception e) {
            logger.warn("Failed to send patient approval email", e);
        }

        logger.info("Registration {} approved by {}, created demographic {}",
                id, reviewerNo, result.getDemographicNo());

        return jsonResponse(Map.of(
                "success", true,
                "demographic_no", result.getDemographicNo(),
                "message", "Registration approved and patient created"
        ));
    }

    /**
     * Rejects a registration.
     *
     * @param id the registration ID
     * @param rejectionRequest Map containing reason
     * @return Response with success status
     */
    @PUT
    @Path("/queue/{id}/reject")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response reject(@PathParam("id") Integer id, Map<String, Object> rejectionRequest) {
        if (!checkStaffPrivilege("w")) {
            return errorResponse(403, "Access denied");
        }

        String reason = (String) rejectionRequest.get("reason");
        String reviewerNo = getLoggedInProviderNo();

        if (StringUtils.isBlank(reason)) {
            reason = "Registration rejected by staff";
        }

        boolean success = getTransferService().rejectRegistration(id, reason, reviewerNo);

        if (!success) {
            return errorResponse(400, "Failed to reject registration");
        }

        // Send patient notification
        try {
            PatientRegistrationQueue queue = getQueueDao().find(id);
            RegistrationModuleConfig config = getConfigDao().findByFacilityId(queue.getFacilityId());
            getNotificationService().sendPatientRejectionEmail(queue, config);
        } catch (Exception e) {
            logger.warn("Failed to send patient rejection email", e);
        }

        logger.info("Registration {} rejected by {}: {}", id, reviewerNo, reason);

        return jsonResponse(Map.of(
                "success", true,
                "message", "Registration rejected"
        ));
    }

    /**
     * Gets QR code for printing.
     *
     * @return Response with QR code data
     */
    @GET
    @Path("/qrcode")
    public Response getQRCode() {
        if (!checkStaffPrivilege("r")) {
            return errorResponse(403, "Access denied");
        }

        try {
            String baseUrl = request.getRequestURL().toString()
                    .replace("/ws/rs/registration/qrcode", "");

            String url = getQrCodeGenerator().buildRegistrationUrl(baseUrl);
            String dataUrl = getQrCodeGenerator().generateQRCodeDataUrl(url);

            return jsonResponse(Map.of(
                    "url", url,
                    "qr_image_base64", dataUrl,
                    "width", getQrCodeGenerator().getDefaultWidth(),
                    "height", getQrCodeGenerator().getDefaultHeight()
            ));

        } catch (Exception e) {
            logger.error("Failed to generate QR code", e);
            return errorResponse(500, "Failed to generate QR code");
        }
    }

    /**
     * Gets pending count for menu badge.
     *
     * @return Response with pending count
     */
    @GET
    @Path("/pending-count")
    public Response getPendingCount() {
        if (!checkStaffPrivilege("r")) {
            return errorResponse(403, "Access denied");
        }

        int count = getQueueDao().countPending();
        return jsonResponse(Map.of("count", count));
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private void populateQueueFromMap(PatientRegistrationQueue queue, Map<String, Object> data) {
        queue.setFirstName(getString(data, "first_name"));
        queue.setLastName(getString(data, "last_name"));
        queue.setMiddleNames(getString(data, "middle_names"));
        queue.setPreferredName(getString(data, "pref_name"));
        queue.setTitle(getString(data, "title"));
        queue.setYearOfBirth(getString(data, "year_of_birth"));
        queue.setMonthOfBirth(getString(data, "month_of_birth"));
        queue.setDateOfBirth(getString(data, "date_of_birth"));
        queue.setSex(getString(data, "sex"));
        queue.setGender(getString(data, "gender"));
        queue.setPronoun(getString(data, "pronoun"));
        queue.setAddress(getString(data, "address"));
        queue.setCity(getString(data, "city"));
        queue.setProvince(getString(data, "province"));
        queue.setPostal(getString(data, "postal"));
        queue.setPhone(getString(data, "phone"));
        queue.setPhone2(getString(data, "phone2"));
        queue.setCellPhone(getString(data, "cell_phone"));
        queue.setEmail(getString(data, "email"));
        queue.setConsentEmail(getBoolean(data, "consent_email"));
        queue.setHin(getString(data, "hin"));
        queue.setVer(getString(data, "ver"));
        queue.setHcType(getString(data, "hc_type"));
        queue.setOfficialLang(getString(data, "official_lang"));
        queue.setSpokenLang(getString(data, "spoken_lang"));
        queue.setCountryOfOrigin(getString(data, "country_of_origin"));
        queue.setEmergencyContactName(getString(data, "emergency_contact_name"));
        queue.setEmergencyContactPhone(getString(data, "emergency_contact_phone"));
        queue.setEmergencyContactRelationship(getString(data, "emergency_contact_relationship"));

        // HIN validation
        if (StringUtils.isNotBlank(queue.getHin())) {
            RegistrationValidator.HinValidationResult hinResult =
                    getValidator().validateHin(queue.getHin(), queue.getHcType());
            if (hinResult.isValid()) {
                queue.setHinValidationStatus(PatientRegistrationQueue.HinValidationStatus.VALID);
            } else {
                queue.setHinValidationStatus(PatientRegistrationQueue.HinValidationStatus.INVALID);
                queue.setHinValidationError(hinResult.getErrorMessage());
            }
        }
    }

    private Map<String, Object> queueToMap(PatientRegistrationQueue q, boolean includeDetails) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", q.getId());
        map.put("status", q.getStatus().getValue());
        map.put("submitted_at", q.getSubmittedAt());
        map.put("first_name", q.getFirstName());
        map.put("last_name", q.getLastName());
        map.put("full_name", q.getFullName());
        map.put("date_of_birth", q.getFullDateOfBirth());
        map.put("email", q.getEmail());
        map.put("cell_phone", q.getCellPhone());
        map.put("has_duplicate_warning", q.hasDuplicateWarning());
        map.put("has_hin_error", q.hasHinValidationError());

        if (includeDetails) {
            map.put("middle_names", q.getMiddleNames());
            map.put("pref_name", q.getPreferredName());
            map.put("title", q.getTitle());
            map.put("sex", q.getSex());
            map.put("gender", q.getGender());
            map.put("pronoun", q.getPronoun());
            map.put("address", q.getAddress());
            map.put("city", q.getCity());
            map.put("province", q.getProvince());
            map.put("postal", q.getPostal());
            map.put("phone", q.getPhone());
            map.put("phone2", q.getPhone2());
            map.put("hin", q.getHin());
            map.put("masked_hin", q.getMaskedHin());
            map.put("ver", q.getVer());
            map.put("hc_type", q.getHcType());
            map.put("hin_validation_error", q.getHinValidationError());
            map.put("duplicate_warning", q.getDuplicateWarning());
            map.put("potential_duplicate_ids", q.getPotentialDuplicateIds());
            map.put("spoken_lang", q.getSpokenLang());
            map.put("emergency_contact_name", q.getEmergencyContactName());
            map.put("emergency_contact_phone", q.getEmergencyContactPhone());
            map.put("consent_email", q.getConsentEmail());
        }

        return map;
    }

    private String getString(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value != null ? value.toString().trim() : null;
    }

    private Boolean getBoolean(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return "true".equalsIgnoreCase((String) value) || "1".equals(value);
        }
        return false;
    }

    private String formatDuplicateWarning(List<RegistrationTransferService.DuplicateMatch> duplicates) {
        StringBuilder sb = new StringBuilder();
        for (RegistrationTransferService.DuplicateMatch match : duplicates) {
            if (sb.length() > 0) sb.append("; ");
            sb.append(String.format("Demographic #%d: %s %s (DOB: %s, HIN: %s)",
                    match.getDemographicNo(),
                    match.getFirstName(),
                    match.getLastName(),
                    match.getDateOfBirth(),
                    match.getHin() != null ? "****" + match.getHin().substring(Math.max(0, match.getHin().length() - 4)) : "N/A"
            ));
        }
        return sb.toString();
    }

    private String formatDuplicateIds(List<RegistrationTransferService.DuplicateMatch> duplicates) {
        StringBuilder sb = new StringBuilder();
        for (RegistrationTransferService.DuplicateMatch match : duplicates) {
            if (sb.length() > 0) sb.append(",");
            sb.append(match.getDemographicNo());
        }
        return sb.toString();
    }

    private String getClientIp() {
        String xff = request.getHeader("X-Forwarded-For");
        if (StringUtils.isNotBlank(xff)) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private boolean checkStaffPrivilege(String privilege) {
        try {
            LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
            if (loggedInInfo == null) {
                return false;
            }
            return getSecurityInfoManager().hasPrivilege(loggedInInfo, "_admin", privilege, null);
        } catch (Exception e) {
            logger.warn("Error checking privilege", e);
            return false;
        }
    }

    private String getLoggedInProviderNo() {
        try {
            LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
            if (loggedInInfo != null && loggedInInfo.getLoggedInProvider() != null) {
                return loggedInInfo.getLoggedInProvider().getProviderNo();
            }
        } catch (Exception e) {
            logger.warn("Error getting logged in provider", e);
        }
        return "system";
    }

    private Response jsonResponse(Object data) {
        return Response.ok(data).build();
    }

    private Response errorResponse(int status, String message) {
        return Response.status(status)
                .entity(Map.of("error", message))
                .build();
    }
}
