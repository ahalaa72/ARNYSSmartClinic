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
package ca.openosp.openo.registration.web;

import ca.openosp.openo.managers.SecurityInfoManager;
import ca.openosp.openo.registration.dao.PatientRegistrationQueueDao;
import ca.openosp.openo.registration.dao.RegistrationModuleConfigDao;
import ca.openosp.openo.registration.dao.RegistrationTokenDao;
import ca.openosp.openo.registration.model.PatientRegistrationQueue;
import ca.openosp.openo.registration.model.RegistrationModuleConfig;
import ca.openosp.openo.registration.model.RegistrationToken;
import ca.openosp.openo.registration.service.RegistrationNotificationService;
import ca.openosp.openo.registration.service.RegistrationTransferService;
import ca.openosp.openo.registration.util.QRCodeGenerator;
import ca.openosp.openo.utility.LoggedInInfo;
import ca.openosp.openo.utility.SpringUtils;
import com.opensymphony.xwork2.ActionSupport;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.struts2.ServletActionContext;
import org.owasp.encoder.Encode;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;

/**
 * Struts2 Action for managing patient registration queue.
 *
 * <p>Provides staff interface functionality for:</p>
 * <ul>
 *   <li>Listing pending registrations</li>
 *   <li>Viewing registration details</li>
 *   <li>Approving registrations (creates demographic record)</li>
 *   <li>Rejecting registrations</li>
 *   <li>Generating QR codes</li>
 *   <li>AJAX endpoint for pending count badge</li>
 * </ul>
 *
 * <p>Part of the Patient Self-Registration Module - a standalone commercial module
 * for OpenO EMR that enables patients to self-register via QR code scanning.</p>
 *
 * <p>Security: All methods require _registration privilege check.</p>
 *
 * @since 2025-12-29
 */
public class RegistrationQueue2Action extends ActionSupport {

    private static final Logger logger = LogManager.getLogger(RegistrationQueue2Action.class);

    private HttpServletRequest request = ServletActionContext.getRequest();
    private HttpServletResponse response = ServletActionContext.getResponse();

    private SecurityInfoManager securityInfoManager = SpringUtils.getBean(SecurityInfoManager.class);
    private PatientRegistrationQueueDao queueDao = SpringUtils.getBean(PatientRegistrationQueueDao.class);
    private RegistrationTokenDao tokenDao = SpringUtils.getBean(RegistrationTokenDao.class);
    private RegistrationModuleConfigDao configDao = SpringUtils.getBean(RegistrationModuleConfigDao.class);
    private RegistrationTransferService transferService = SpringUtils.getBean(RegistrationTransferService.class);
    private RegistrationNotificationService notificationService = SpringUtils.getBean(RegistrationNotificationService.class);
    private QRCodeGenerator qrCodeGenerator = SpringUtils.getBean(QRCodeGenerator.class);

    // Action properties
    private Integer id;
    private String status;
    private String providerNo;
    private String rejectionReason;
    private List<PatientRegistrationQueue> registrations;
    private PatientRegistrationQueue registration;
    private int pendingCount;
    private String qrCodeBase64;
    private String registrationUrl;
    private String errorMessage;
    private String successMessage;

    // ========================================================================
    // Action Methods
    // ========================================================================

    /**
     * Lists pending registrations in the queue.
     *
     * @return String result name for Struts navigation
     */
    public String list() {
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);

        if (!securityInfoManager.hasPrivilege(loggedInInfo, "_registration", "r", null)) {
            throw new SecurityException("Missing required security object: _registration read");
        }

        try {
            Integer facilityId = loggedInInfo.getCurrentFacility() != null ?
                    loggedInInfo.getCurrentFacility().getId() : null;

            if (status != null && !status.isEmpty()) {
                registrations = queueDao.findByStatus(status);
            } else {
                registrations = queueDao.findPendingByFacility(facilityId);
            }

            pendingCount = queueDao.countPending();

            logger.debug("Loaded {} registrations for facility {}", registrations.size(), facilityId);

        } catch (Exception e) {
            logger.error("Error loading registration queue", e);
            errorMessage = "Error loading registration queue: " + e.getMessage();
        }

        return SUCCESS;
    }

    /**
     * Views details of a single registration.
     *
     * @return String result name for Struts navigation
     */
    public String view() {
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);

        if (!securityInfoManager.hasPrivilege(loggedInInfo, "_registration", "r", null)) {
            throw new SecurityException("Missing required security object: _registration read");
        }

        if (id == null) {
            errorMessage = "Registration ID is required";
            return ERROR;
        }

        try {
            registration = queueDao.find(id);

            if (registration == null) {
                errorMessage = "Registration not found";
                return ERROR;
            }

            logger.debug("Viewing registration {}", id);

        } catch (Exception e) {
            logger.error("Error loading registration {}", id, e);
            errorMessage = "Error loading registration: " + e.getMessage();
            return ERROR;
        }

        return SUCCESS;
    }

    /**
     * Approves a registration and creates a demographic record.
     *
     * @return String result name for Struts navigation
     */
    public String approve() {
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);

        if (!securityInfoManager.hasPrivilege(loggedInInfo, "_registration", "w", null)) {
            throw new SecurityException("Missing required security object: _registration write");
        }

        if (id == null) {
            errorMessage = "Registration ID is required";
            return ERROR;
        }

        if (providerNo == null || providerNo.isEmpty()) {
            errorMessage = "Provider must be selected for approval";
            return ERROR;
        }

        try {
            registration = queueDao.find(id);

            if (registration == null) {
                errorMessage = "Registration not found";
                return ERROR;
            }

            if (registration.getStatus() != PatientRegistrationQueue.RegistrationStatus.PENDING) {
                errorMessage = "Only pending registrations can be approved";
                return ERROR;
            }

            // Transfer to demographic table
            Integer demographicNo = transferService.transferTodemographic(registration, providerNo);

            // Update queue record
            registration.setStatus(PatientRegistrationQueue.RegistrationStatus.APPROVED);
            registration.setReviewedAt(new Date());
            registration.setReviewedBy(loggedInInfo.getLoggedInProviderNo());
            registration.setDemographicNo(demographicNo);
            queueDao.merge(registration);

            // Send notification to patient
            if (registration.getEmail() != null && !registration.getEmail().isEmpty()) {
                try {
                    notificationService.sendPatientApprovalEmail(registration);
                } catch (Exception e) {
                    logger.warn("Failed to send approval email to patient", e);
                    // Don't fail the approval if email fails
                }
            }

            // Notify staff
            notificationService.notifyStaffRegistrationProcessed(registration, "approved");

            successMessage = "Registration approved. Patient record #" + demographicNo + " created.";
            logger.info("Approved registration {} -> demographic {}", id, demographicNo);

        } catch (Exception e) {
            logger.error("Error approving registration {}", id, e);
            errorMessage = "Error approving registration: " + e.getMessage();
            return ERROR;
        }

        return SUCCESS;
    }

    /**
     * Rejects a registration with a reason.
     *
     * @return String result name for Struts navigation
     */
    public String reject() {
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);

        if (!securityInfoManager.hasPrivilege(loggedInInfo, "_registration", "w", null)) {
            throw new SecurityException("Missing required security object: _registration write");
        }

        if (id == null) {
            errorMessage = "Registration ID is required";
            return ERROR;
        }

        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            errorMessage = "Rejection reason is required";
            return ERROR;
        }

        try {
            registration = queueDao.find(id);

            if (registration == null) {
                errorMessage = "Registration not found";
                return ERROR;
            }

            if (registration.getStatus() != PatientRegistrationQueue.RegistrationStatus.PENDING) {
                errorMessage = "Only pending registrations can be rejected";
                return ERROR;
            }

            // Update queue record
            registration.setStatus(PatientRegistrationQueue.RegistrationStatus.REJECTED);
            registration.setReviewedAt(new Date());
            registration.setReviewedBy(loggedInInfo.getLoggedInProviderNo());
            registration.setRejectionReason(rejectionReason.trim());
            queueDao.merge(registration);

            // Send notification to patient
            if (registration.getEmail() != null && !registration.getEmail().isEmpty()) {
                try {
                    notificationService.sendPatientRejectionEmail(registration, rejectionReason);
                } catch (Exception e) {
                    logger.warn("Failed to send rejection email to patient", e);
                }
            }

            // Notify staff
            notificationService.notifyStaffRegistrationProcessed(registration, "rejected");

            successMessage = "Registration rejected.";
            logger.info("Rejected registration {} with reason: {}", id, rejectionReason);

        } catch (Exception e) {
            logger.error("Error rejecting registration {}", id, e);
            errorMessage = "Error rejecting registration: " + e.getMessage();
            return ERROR;
        }

        return SUCCESS;
    }

    /**
     * Displays the QR code generation page.
     *
     * @return String result name for Struts navigation
     */
    public String qrcode() {
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);

        if (!securityInfoManager.hasPrivilege(loggedInInfo, "_registration", "r", null)) {
            throw new SecurityException("Missing required security object: _registration read");
        }

        try {
            // Build registration URL
            String baseUrl = request.getScheme() + "://" + request.getServerName();
            if ((request.getScheme().equals("http") && request.getServerPort() != 80) ||
                    (request.getScheme().equals("https") && request.getServerPort() != 443)) {
                baseUrl += ":" + request.getServerPort();
            }
            registrationUrl = baseUrl + request.getContextPath() + "/registration/start";

            // Generate QR code
            qrCodeBase64 = qrCodeGenerator.generateQRCodeBase64(registrationUrl, 400, 400);

            logger.debug("Generated QR code for URL: {}", registrationUrl);

        } catch (Exception e) {
            logger.error("Error generating QR code", e);
            errorMessage = "Error generating QR code: " + e.getMessage();
        }

        return SUCCESS;
    }

    /**
     * AJAX endpoint to get pending registration count for menu badge.
     *
     * @return String result name (null for direct response)
     */
    public String getPendingCount() {
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);

        if (!securityInfoManager.hasPrivilege(loggedInInfo, "_registration", "r", null)) {
            sendJsonResponse("{\"error\": \"Unauthorized\"}");
            return null;
        }

        try {
            pendingCount = queueDao.countPending();
            sendJsonResponse("{\"count\": " + pendingCount + "}");

        } catch (Exception e) {
            logger.error("Error getting pending count", e);
            sendJsonResponse("{\"error\": \"" + Encode.forJavaScript(e.getMessage()) + "\"}");
        }

        return null;
    }

    /**
     * AJAX endpoint to quick-approve a registration.
     *
     * @return String result name (null for direct response)
     */
    public String quickApprove() {
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);

        if (!securityInfoManager.hasPrivilege(loggedInInfo, "_registration", "w", null)) {
            sendJsonResponse("{\"success\": false, \"error\": \"Unauthorized\"}");
            return null;
        }

        if (id == null || providerNo == null) {
            sendJsonResponse("{\"success\": false, \"error\": \"Missing required parameters\"}");
            return null;
        }

        try {
            registration = queueDao.find(id);

            if (registration == null) {
                sendJsonResponse("{\"success\": false, \"error\": \"Registration not found\"}");
                return null;
            }

            if (registration.getStatus() != PatientRegistrationQueue.RegistrationStatus.PENDING) {
                sendJsonResponse("{\"success\": false, \"error\": \"Registration is not pending\"}");
                return null;
            }

            // Transfer to demographic table
            Integer demographicNo = transferService.transferTodemographic(registration, providerNo);

            // Update queue record
            registration.setStatus(PatientRegistrationQueue.RegistrationStatus.APPROVED);
            registration.setReviewedAt(new Date());
            registration.setReviewedBy(loggedInInfo.getLoggedInProviderNo());
            registration.setDemographicNo(demographicNo);
            queueDao.merge(registration);

            // Send notifications asynchronously (don't block response)
            if (registration.getEmail() != null && !registration.getEmail().isEmpty()) {
                try {
                    notificationService.sendPatientApprovalEmail(registration);
                } catch (Exception e) {
                    logger.warn("Failed to send approval email", e);
                }
            }

            sendJsonResponse("{\"success\": true, \"demographicNo\": " + demographicNo + "}");
            logger.info("Quick-approved registration {} -> demographic {}", id, demographicNo);

        } catch (Exception e) {
            logger.error("Error quick-approving registration {}", id, e);
            sendJsonResponse("{\"success\": false, \"error\": \"" + Encode.forJavaScript(e.getMessage()) + "\"}");
        }

        return null;
    }

    /**
     * AJAX endpoint to quick-reject a registration.
     *
     * @return String result name (null for direct response)
     */
    public String quickReject() {
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);

        if (!securityInfoManager.hasPrivilege(loggedInInfo, "_registration", "w", null)) {
            sendJsonResponse("{\"success\": false, \"error\": \"Unauthorized\"}");
            return null;
        }

        if (id == null || rejectionReason == null || rejectionReason.trim().isEmpty()) {
            sendJsonResponse("{\"success\": false, \"error\": \"Missing required parameters\"}");
            return null;
        }

        try {
            registration = queueDao.find(id);

            if (registration == null) {
                sendJsonResponse("{\"success\": false, \"error\": \"Registration not found\"}");
                return null;
            }

            if (registration.getStatus() != PatientRegistrationQueue.RegistrationStatus.PENDING) {
                sendJsonResponse("{\"success\": false, \"error\": \"Registration is not pending\"}");
                return null;
            }

            // Update queue record
            registration.setStatus(PatientRegistrationQueue.RegistrationStatus.REJECTED);
            registration.setReviewedAt(new Date());
            registration.setReviewedBy(loggedInInfo.getLoggedInProviderNo());
            registration.setRejectionReason(rejectionReason.trim());
            queueDao.merge(registration);

            // Send rejection email
            if (registration.getEmail() != null && !registration.getEmail().isEmpty()) {
                try {
                    notificationService.sendPatientRejectionEmail(registration, rejectionReason);
                } catch (Exception e) {
                    logger.warn("Failed to send rejection email", e);
                }
            }

            sendJsonResponse("{\"success\": true}");
            logger.info("Quick-rejected registration {}", id);

        } catch (Exception e) {
            logger.error("Error quick-rejecting registration {}", id, e);
            sendJsonResponse("{\"success\": false, \"error\": \"" + Encode.forJavaScript(e.getMessage()) + "\"}");
        }

        return null;
    }

    /**
     * Downloads QR code as PNG image.
     *
     * @return String result name (null for direct response)
     */
    public String downloadQRCode() {
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);

        if (!securityInfoManager.hasPrivilege(loggedInInfo, "_registration", "r", null)) {
            throw new SecurityException("Missing required security object: _registration read");
        }

        try {
            // Get size from parameter
            String sizeParam = request.getParameter("size");
            int size = 400;
            if (sizeParam != null) {
                try {
                    size = Integer.parseInt(sizeParam);
                    if (size < 100) size = 100;
                    if (size > 1000) size = 1000;
                } catch (NumberFormatException e) {
                    size = 400;
                }
            }

            // Build registration URL
            String baseUrl = request.getScheme() + "://" + request.getServerName();
            if ((request.getScheme().equals("http") && request.getServerPort() != 80) ||
                    (request.getScheme().equals("https") && request.getServerPort() != 443)) {
                baseUrl += ":" + request.getServerPort();
            }
            registrationUrl = baseUrl + request.getContextPath() + "/registration/start";

            // Generate QR code as bytes
            byte[] qrBytes = qrCodeGenerator.generateQRCode(registrationUrl, size, size);

            // Send as download
            response.setContentType("image/png");
            response.setHeader("Content-Disposition", "attachment; filename=\"registration-qr-" + size + ".png\"");
            response.setContentLength(qrBytes.length);
            response.getOutputStream().write(qrBytes);
            response.getOutputStream().flush();

            logger.debug("Downloaded QR code size {}", size);

        } catch (Exception e) {
            logger.error("Error downloading QR code", e);
        }

        return null;
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    /**
     * Sends a JSON response directly to the output stream.
     *
     * @param json String JSON content to send
     */
    private void sendJsonResponse(String json) {
        try {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            PrintWriter out = response.getWriter();
            out.print(json);
            out.flush();
        } catch (IOException e) {
            logger.error("Error sending JSON response", e);
        }
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProviderNo() {
        return providerNo;
    }

    public void setProviderNo(String providerNo) {
        this.providerNo = providerNo;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public List<PatientRegistrationQueue> getRegistrations() {
        return registrations;
    }

    public void setRegistrations(List<PatientRegistrationQueue> registrations) {
        this.registrations = registrations;
    }

    public PatientRegistrationQueue getRegistration() {
        return registration;
    }

    public void setRegistration(PatientRegistrationQueue registration) {
        this.registration = registration;
    }

    public int getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(int pendingCount) {
        this.pendingCount = pendingCount;
    }

    public String getQrCodeBase64() {
        return qrCodeBase64;
    }

    public void setQrCodeBase64(String qrCodeBase64) {
        this.qrCodeBase64 = qrCodeBase64;
    }

    public String getRegistrationUrl() {
        return registrationUrl;
    }

    public void setRegistrationUrl(String registrationUrl) {
        this.registrationUrl = registrationUrl;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getSuccessMessage() {
        return successMessage;
    }

    public void setSuccessMessage(String successMessage) {
        this.successMessage = successMessage;
    }
}
