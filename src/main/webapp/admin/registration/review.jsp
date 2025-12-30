<%--
    Patient Self-Registration Module - Registration Review Page

    Staff interface for reviewing individual patient registration submissions.
    Allows staff to view full details, edit fields, check for duplicates,
    and approve or reject the registration.

    Part of the Patient Self-Registration Module - a standalone commercial module
    for OpenO EMR that enables patients to self-register via QR code scanning.

    Features:
    - Full patient data display with validation status
    - Side-by-side duplicate comparison
    - Editable fields before approval
    - Provider assignment dropdown
    - Approve/Reject actions with confirmation
    - Rejection reason capture
    - HIN validation errors displayed inline

    Security: Requires authenticated staff session with _registration privilege
    @since 2025-12-29
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page import="org.owasp.encoder.Encode" %>
<%@ page import="ca.openosp.openo.utility.SpringUtils" %>
<%@ page import="ca.openosp.openo.managers.SecurityInfoManager" %>
<%@ page import="ca.openosp.openo.utility.LoggedInInfo" %>
<%@ page import="ca.openosp.openo.registration.model.PatientRegistrationQueue" %>
<%@ page import="ca.openosp.openo.registration.dao.PatientRegistrationQueueDao" %>
<%@ page import="ca.openosp.openo.commn.dao.ProviderDao" %>
<%@ page import="ca.openosp.openo.commn.model.Provider" %>
<%@ page import="java.util.List" %>
<%@ page import="java.text.SimpleDateFormat" %>

<%
    // Security check
    SecurityInfoManager securityInfoManager = SpringUtils.getBean(SecurityInfoManager.class);
    LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);

    if (loggedInInfo == null || !securityInfoManager.hasPrivilege(loggedInInfo, "_registration", "r", null)) {
        response.sendRedirect(request.getContextPath() + "/login.do");
        return;
    }

    boolean canWrite = securityInfoManager.hasPrivilege(loggedInInfo, "_registration", "w", null);

    // Get registration ID from request
    String idParam = request.getParameter("id");
    if (idParam == null || idParam.isEmpty()) {
        response.sendRedirect("queue.jsp?error=missing_id");
        return;
    }

    Integer registrationId;
    try {
        registrationId = Integer.parseInt(idParam);
    } catch (NumberFormatException e) {
        response.sendRedirect("queue.jsp?error=invalid_id");
        return;
    }

    // Load registration record
    PatientRegistrationQueueDao queueDao = SpringUtils.getBean(PatientRegistrationQueueDao.class);
    PatientRegistrationQueue registration = queueDao.find(registrationId);

    if (registration == null) {
        response.sendRedirect("queue.jsp?error=not_found");
        return;
    }

    // Load providers for assignment dropdown
    ProviderDao providerDao = SpringUtils.getBean(ProviderDao.class);
    List<Provider> activeProviders = providerDao.getActiveProviders();

    // Format dates
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    String submittedDate = registration.getSubmittedAt() != null ?
            dateFormat.format(registration.getSubmittedAt()) : "N/A";
    String expiresDate = registration.getExpiresAt() != null ?
            dateFormat.format(registration.getExpiresAt()) : "N/A";

    // Check for success/error messages
    String successMsg = request.getParameter("success");
    String errorMsg = request.getParameter("error");

    // Patient full DOB
    String fullDob = "";
    if (registration.getYearOfBirth() != null && registration.getMonthOfBirth() != null && registration.getDateOfBirth() != null) {
        fullDob = registration.getYearOfBirth() + "-" +
                  String.format("%02d", Integer.parseInt(registration.getMonthOfBirth())) + "-" +
                  String.format("%02d", Integer.parseInt(registration.getDateOfBirth()));
    }

    // Get status badge class
    String statusBadgeClass = "bg-secondary";
    String statusText = registration.getStatus() != null ? registration.getStatus().name() : "UNKNOWN";
    if (registration.getStatus() == PatientRegistrationQueue.RegistrationStatus.PENDING) {
        statusBadgeClass = "bg-warning text-dark";
    } else if (registration.getStatus() == PatientRegistrationQueue.RegistrationStatus.APPROVED) {
        statusBadgeClass = "bg-success";
    } else if (registration.getStatus() == PatientRegistrationQueue.RegistrationStatus.REJECTED) {
        statusBadgeClass = "bg-danger";
    } else if (registration.getStatus() == PatientRegistrationQueue.RegistrationStatus.EXPIRED) {
        statusBadgeClass = "bg-secondary";
    }

    // CSRF token
    String csrfToken = (String) session.getAttribute("OWASP_CSRFTOKEN");
    if (csrfToken == null) {
        csrfToken = "";
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Review Registration - OpenO EMR</title>

    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css" rel="stylesheet">

    <style>
        body {
            background-color: #f8f9fa;
        }

        .review-header {
            background: linear-gradient(135deg, #1a5276 0%, #2980b9 100%);
            color: white;
            padding: 24px;
            border-radius: 12px;
            margin-bottom: 24px;
        }

        .section-card {
            background: white;
            border-radius: 12px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.08);
            margin-bottom: 24px;
        }

        .section-header {
            background-color: #f8f9fa;
            padding: 16px 20px;
            border-bottom: 1px solid #e9ecef;
            border-radius: 12px 12px 0 0;
            font-weight: 600;
        }

        .section-body {
            padding: 20px;
        }

        .field-label {
            font-weight: 500;
            color: #6c757d;
            font-size: 0.85rem;
            margin-bottom: 4px;
        }

        .field-value {
            font-size: 1rem;
            color: #212529;
            margin-bottom: 16px;
        }

        .field-value.empty {
            color: #adb5bd;
            font-style: italic;
        }

        .validation-error {
            color: #dc3545;
            font-size: 0.85rem;
            margin-top: 4px;
        }

        .validation-success {
            color: #198754;
            font-size: 0.85rem;
        }

        .duplicate-warning {
            background-color: #fff3cd;
            border: 1px solid #ffc107;
            border-radius: 8px;
            padding: 16px;
            margin-bottom: 24px;
        }

        .duplicate-item {
            background: white;
            border: 1px solid #dee2e6;
            border-radius: 8px;
            padding: 12px;
            margin-top: 12px;
        }

        .action-buttons {
            position: sticky;
            bottom: 0;
            background: white;
            padding: 16px 24px;
            border-top: 1px solid #e9ecef;
            box-shadow: 0 -4px 12px rgba(0,0,0,0.08);
        }

        .btn-approve {
            background-color: #198754;
            border-color: #198754;
        }

        .btn-approve:hover {
            background-color: #157347;
            border-color: #146c43;
        }

        .btn-reject {
            background-color: #dc3545;
            border-color: #dc3545;
        }

        .btn-reject:hover {
            background-color: #bb2d3b;
            border-color: #b02a37;
        }

        .hin-display {
            font-family: monospace;
            font-size: 1.1rem;
            letter-spacing: 1px;
        }

        .status-timeline {
            border-left: 3px solid #e9ecef;
            padding-left: 20px;
            margin-left: 10px;
        }

        .timeline-item {
            position: relative;
            padding-bottom: 16px;
        }

        .timeline-item::before {
            content: '';
            position: absolute;
            left: -26px;
            top: 4px;
            width: 12px;
            height: 12px;
            border-radius: 50%;
            background: #6c757d;
        }

        .timeline-item.active::before {
            background: #198754;
        }

        .editable-field {
            background-color: #fff3cd;
            border: 1px dashed #ffc107;
            padding: 8px;
            border-radius: 4px;
        }

        @media print {
            .action-buttons, .btn, .no-print {
                display: none !important;
            }
        }
    </style>
</head>
<body>
    <div class="container-fluid py-4">
        <!-- Back Button -->
        <div class="mb-3 no-print">
            <a href="queue.jsp" class="btn btn-outline-secondary">
                <i class="bi bi-arrow-left me-2"></i>Back to Queue
            </a>
        </div>

        <!-- Alert Messages -->
        <% if (successMsg != null && !successMsg.isEmpty()) { %>
        <div class="alert alert-success alert-dismissible fade show" role="alert">
            <i class="bi bi-check-circle me-2"></i>
            <% if ("approved".equals(successMsg)) { %>
                Registration has been approved and patient record created.
            <% } else if ("rejected".equals(successMsg)) { %>
                Registration has been rejected.
            <% } else if ("updated".equals(successMsg)) { %>
                Registration details have been updated.
            <% } %>
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
        <% } %>

        <% if (errorMsg != null && !errorMsg.isEmpty()) { %>
        <div class="alert alert-danger alert-dismissible fade show" role="alert">
            <i class="bi bi-exclamation-triangle me-2"></i>
            <%= Encode.forHtml(errorMsg) %>
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
        <% } %>

        <!-- Header -->
        <div class="review-header">
            <div class="row align-items-center">
                <div class="col-md-8">
                    <h2 class="mb-1">
                        <%= Encode.forHtml(registration.getFirstName() != null ? registration.getFirstName() : "") %>
                        <%= Encode.forHtml(registration.getLastName() != null ? registration.getLastName() : "") %>
                    </h2>
                    <p class="mb-0 opacity-75">
                        Registration #<%= registration.getId() %> |
                        Submitted: <%= submittedDate %>
                    </p>
                </div>
                <div class="col-md-4 text-md-end">
                    <span class="badge <%= statusBadgeClass %> fs-6 px-3 py-2">
                        <%= statusText %>
                    </span>
                </div>
            </div>
        </div>

        <% if (registration.getDuplicateWarning() != null && !registration.getDuplicateWarning().isEmpty()) { %>
        <!-- Duplicate Warning -->
        <div class="duplicate-warning">
            <h5 class="mb-2">
                <i class="bi bi-exclamation-triangle-fill text-warning me-2"></i>
                Potential Duplicate Detected
            </h5>
            <p class="mb-2">This registration may match an existing patient record. Please review carefully before approving.</p>
            <div class="duplicate-item">
                <strong>Matching Criteria:</strong>
                <%= Encode.forHtml(registration.getDuplicateWarning()) %>
            </div>
        </div>
        <% } %>

        <div class="row">
            <!-- Left Column - Patient Information -->
            <div class="col-lg-8">
                <!-- Personal Information -->
                <div class="section-card">
                    <div class="section-header">
                        <i class="bi bi-person me-2"></i>Personal Information
                    </div>
                    <div class="section-body">
                        <div class="row">
                            <div class="col-md-4">
                                <div class="field-label">First Name</div>
                                <div class="field-value"><%= Encode.forHtml(registration.getFirstName() != null ? registration.getFirstName() : "") %></div>
                            </div>
                            <div class="col-md-4">
                                <div class="field-label">Middle Names</div>
                                <div class="field-value <%= registration.getMiddleNames() == null || registration.getMiddleNames().isEmpty() ? "empty" : "" %>">
                                    <%= registration.getMiddleNames() != null && !registration.getMiddleNames().isEmpty() ? Encode.forHtml(registration.getMiddleNames()) : "Not provided" %>
                                </div>
                            </div>
                            <div class="col-md-4">
                                <div class="field-label">Last Name</div>
                                <div class="field-value"><%= Encode.forHtml(registration.getLastName() != null ? registration.getLastName() : "") %></div>
                            </div>
                        </div>
                        <div class="row">
                            <div class="col-md-4">
                                <div class="field-label">Preferred Name</div>
                                <div class="field-value <%= registration.getPrefName() == null || registration.getPrefName().isEmpty() ? "empty" : "" %>">
                                    <%= registration.getPrefName() != null && !registration.getPrefName().isEmpty() ? Encode.forHtml(registration.getPrefName()) : "Not provided" %>
                                </div>
                            </div>
                            <div class="col-md-4">
                                <div class="field-label">Title</div>
                                <div class="field-value <%= registration.getTitle() == null || registration.getTitle().isEmpty() ? "empty" : "" %>">
                                    <%= registration.getTitle() != null && !registration.getTitle().isEmpty() ? Encode.forHtml(registration.getTitle()) : "Not provided" %>
                                </div>
                            </div>
                            <div class="col-md-4">
                                <div class="field-label">Date of Birth</div>
                                <div class="field-value"><%= Encode.forHtml(fullDob) %></div>
                            </div>
                        </div>
                        <div class="row">
                            <div class="col-md-4">
                                <div class="field-label">Sex</div>
                                <div class="field-value">
                                    <%
                                        String sexDisplay = "";
                                        if ("M".equals(registration.getSex())) sexDisplay = "Male";
                                        else if ("F".equals(registration.getSex())) sexDisplay = "Female";
                                        else if ("O".equals(registration.getSex())) sexDisplay = "Other";
                                        else sexDisplay = registration.getSex() != null ? registration.getSex() : "";
                                    %>
                                    <%= Encode.forHtml(sexDisplay) %>
                                </div>
                            </div>
                            <div class="col-md-4">
                                <div class="field-label">Gender Identity</div>
                                <div class="field-value <%= registration.getGender() == null || registration.getGender().isEmpty() ? "empty" : "" %>">
                                    <%= registration.getGender() != null && !registration.getGender().isEmpty() ? Encode.forHtml(registration.getGender()) : "Not provided" %>
                                </div>
                            </div>
                            <div class="col-md-4">
                                <div class="field-label">Pronouns</div>
                                <div class="field-value <%= registration.getPronoun() == null || registration.getPronoun().isEmpty() ? "empty" : "" %>">
                                    <%= registration.getPronoun() != null && !registration.getPronoun().isEmpty() ? Encode.forHtml(registration.getPronoun()) : "Not provided" %>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Contact Information -->
                <div class="section-card">
                    <div class="section-header">
                        <i class="bi bi-geo-alt me-2"></i>Contact Information
                    </div>
                    <div class="section-body">
                        <div class="row">
                            <div class="col-md-12">
                                <div class="field-label">Address</div>
                                <div class="field-value">
                                    <%= Encode.forHtml(registration.getAddress() != null ? registration.getAddress() : "") %>
                                </div>
                            </div>
                        </div>
                        <div class="row">
                            <div class="col-md-4">
                                <div class="field-label">City</div>
                                <div class="field-value"><%= Encode.forHtml(registration.getCity() != null ? registration.getCity() : "") %></div>
                            </div>
                            <div class="col-md-4">
                                <div class="field-label">Province</div>
                                <div class="field-value"><%= Encode.forHtml(registration.getProvince() != null ? registration.getProvince() : "") %></div>
                            </div>
                            <div class="col-md-4">
                                <div class="field-label">Postal Code</div>
                                <div class="field-value"><%= Encode.forHtml(registration.getPostal() != null ? registration.getPostal() : "") %></div>
                            </div>
                        </div>
                        <hr>
                        <div class="row">
                            <div class="col-md-4">
                                <div class="field-label">Cell Phone</div>
                                <div class="field-value"><%= Encode.forHtml(registration.getCellPhone() != null ? registration.getCellPhone() : "") %></div>
                            </div>
                            <div class="col-md-4">
                                <div class="field-label">Home Phone</div>
                                <div class="field-value <%= registration.getPhone() == null || registration.getPhone().isEmpty() ? "empty" : "" %>">
                                    <%= registration.getPhone() != null && !registration.getPhone().isEmpty() ? Encode.forHtml(registration.getPhone()) : "Not provided" %>
                                </div>
                            </div>
                            <div class="col-md-4">
                                <div class="field-label">Work Phone</div>
                                <div class="field-value <%= registration.getPhone2() == null || registration.getPhone2().isEmpty() ? "empty" : "" %>">
                                    <%= registration.getPhone2() != null && !registration.getPhone2().isEmpty() ? Encode.forHtml(registration.getPhone2()) : "Not provided" %>
                                </div>
                            </div>
                        </div>
                        <div class="row">
                            <div class="col-md-8">
                                <div class="field-label">Email</div>
                                <div class="field-value">
                                    <%= Encode.forHtml(registration.getEmail() != null ? registration.getEmail() : "") %>
                                    <% if (registration.getConsentEmail() != null && registration.getConsentEmail()) { %>
                                    <span class="badge bg-success ms-2">
                                        <i class="bi bi-check-circle me-1"></i>Consent to email
                                    </span>
                                    <% } else { %>
                                    <span class="badge bg-secondary ms-2">
                                        <i class="bi bi-x-circle me-1"></i>No email consent
                                    </span>
                                    <% } %>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Health Card Information -->
                <div class="section-card">
                    <div class="section-header">
                        <i class="bi bi-credit-card me-2"></i>Health Card Information
                    </div>
                    <div class="section-body">
                        <div class="row">
                            <div class="col-md-6">
                                <div class="field-label">Health Card Number (HIN)</div>
                                <div class="field-value">
                                    <% if (registration.getHin() != null && !registration.getHin().isEmpty()) { %>
                                    <span class="hin-display"><%= Encode.forHtml(registration.getHin()) %></span>
                                    <% if (registration.getVer() != null && !registration.getVer().isEmpty()) { %>
                                    <span class="text-muted ms-2">Ver: <%= Encode.forHtml(registration.getVer()) %></span>
                                    <% } %>

                                    <% if (registration.getHinValidated() != null && registration.getHinValidated()) { %>
                                    <div class="validation-success mt-1">
                                        <i class="bi bi-check-circle me-1"></i>Valid health card number
                                    </div>
                                    <% } else if (registration.getHinValidationError() != null && !registration.getHinValidationError().isEmpty()) { %>
                                    <div class="validation-error mt-1">
                                        <i class="bi bi-exclamation-circle me-1"></i><%= Encode.forHtml(registration.getHinValidationError()) %>
                                    </div>
                                    <% } %>
                                    <% } else { %>
                                    <span class="empty">Not provided</span>
                                    <% } %>
                                </div>
                            </div>
                            <div class="col-md-3">
                                <div class="field-label">Province</div>
                                <div class="field-value"><%= Encode.forHtml(registration.getHcType() != null ? registration.getHcType() : "Not specified") %></div>
                            </div>
                            <div class="col-md-3">
                                <div class="field-label">Expiry Date</div>
                                <div class="field-value <%= registration.getHcRenewDate() == null ? "empty" : "" %>">
                                    <%= registration.getHcRenewDate() != null ? new SimpleDateFormat("yyyy-MM-dd").format(registration.getHcRenewDate()) : "Not provided" %>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Additional Information -->
                <div class="section-card">
                    <div class="section-header">
                        <i class="bi bi-info-circle me-2"></i>Additional Information
                    </div>
                    <div class="section-body">
                        <div class="row">
                            <div class="col-md-4">
                                <div class="field-label">Official Language</div>
                                <div class="field-value <%= registration.getOfficialLang() == null || registration.getOfficialLang().isEmpty() ? "empty" : "" %>">
                                    <%= registration.getOfficialLang() != null && !registration.getOfficialLang().isEmpty() ? Encode.forHtml(registration.getOfficialLang()) : "Not specified" %>
                                </div>
                            </div>
                            <div class="col-md-4">
                                <div class="field-label">Spoken Language</div>
                                <div class="field-value <%= registration.getSpokenLang() == null || registration.getSpokenLang().isEmpty() ? "empty" : "" %>">
                                    <%= registration.getSpokenLang() != null && !registration.getSpokenLang().isEmpty() ? Encode.forHtml(registration.getSpokenLang()) : "Not specified" %>
                                </div>
                            </div>
                            <div class="col-md-4">
                                <div class="field-label">Country of Origin</div>
                                <div class="field-value <%= registration.getCountryOfOrigin() == null || registration.getCountryOfOrigin().isEmpty() ? "empty" : "" %>">
                                    <%= registration.getCountryOfOrigin() != null && !registration.getCountryOfOrigin().isEmpty() ? Encode.forHtml(registration.getCountryOfOrigin()) : "Not specified" %>
                                </div>
                            </div>
                        </div>
                        <div class="row">
                            <div class="col-md-6">
                                <div class="field-label">Emergency Contact Name</div>
                                <div class="field-value <%= registration.getEmergencyContactName() == null || registration.getEmergencyContactName().isEmpty() ? "empty" : "" %>">
                                    <%= registration.getEmergencyContactName() != null && !registration.getEmergencyContactName().isEmpty() ? Encode.forHtml(registration.getEmergencyContactName()) : "Not provided" %>
                                </div>
                            </div>
                            <div class="col-md-6">
                                <div class="field-label">Emergency Contact Phone</div>
                                <div class="field-value <%= registration.getEmergencyContactPhone() == null || registration.getEmergencyContactPhone().isEmpty() ? "empty" : "" %>">
                                    <%= registration.getEmergencyContactPhone() != null && !registration.getEmergencyContactPhone().isEmpty() ? Encode.forHtml(registration.getEmergencyContactPhone()) : "Not provided" %>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Right Column - Status and Actions -->
            <div class="col-lg-4">
                <!-- Status Timeline -->
                <div class="section-card">
                    <div class="section-header">
                        <i class="bi bi-clock-history me-2"></i>Status Timeline
                    </div>
                    <div class="section-body">
                        <div class="status-timeline">
                            <div class="timeline-item active">
                                <div class="text-muted small">Created</div>
                                <div><%= registration.getCreatedAt() != null ? dateFormat.format(registration.getCreatedAt()) : "N/A" %></div>
                            </div>
                            <% if (registration.getSubmittedAt() != null) { %>
                            <div class="timeline-item active">
                                <div class="text-muted small">Submitted</div>
                                <div><%= dateFormat.format(registration.getSubmittedAt()) %></div>
                            </div>
                            <% } %>
                            <% if (registration.getReviewedAt() != null) { %>
                            <div class="timeline-item active">
                                <div class="text-muted small">Reviewed</div>
                                <div><%= dateFormat.format(registration.getReviewedAt()) %></div>
                                <div class="small text-muted">by <%= Encode.forHtml(registration.getReviewedBy() != null ? registration.getReviewedBy() : "Unknown") %></div>
                            </div>
                            <% } %>
                            <% if (registration.getStatus() == PatientRegistrationQueue.RegistrationStatus.APPROVED && registration.getDemographicNo() != null) { %>
                            <div class="timeline-item active">
                                <div class="text-muted small">Patient Created</div>
                                <div>
                                    <a href="<%= request.getContextPath() %>/demographic/demographiccontrol.jsp?demographic_no=<%= registration.getDemographicNo() %>" target="_blank">
                                        View Patient #<%= registration.getDemographicNo() %>
                                    </a>
                                </div>
                            </div>
                            <% } %>
                        </div>
                    </div>
                </div>

                <% if (registration.getStatus() == PatientRegistrationQueue.RegistrationStatus.REJECTED) { %>
                <!-- Rejection Details -->
                <div class="section-card">
                    <div class="section-header bg-danger text-white">
                        <i class="bi bi-x-circle me-2"></i>Rejection Details
                    </div>
                    <div class="section-body">
                        <div class="field-label">Reason</div>
                        <div class="field-value">
                            <%= Encode.forHtml(registration.getRejectionReason() != null ? registration.getRejectionReason() : "No reason provided") %>
                        </div>
                        <div class="field-label">Rejected By</div>
                        <div class="field-value">
                            <%= Encode.forHtml(registration.getReviewedBy() != null ? registration.getReviewedBy() : "Unknown") %>
                        </div>
                    </div>
                </div>
                <% } %>

                <% if (registration.getStatus() == PatientRegistrationQueue.RegistrationStatus.PENDING && canWrite) { %>
                <!-- Approval Form -->
                <div class="section-card">
                    <div class="section-header bg-success text-white">
                        <i class="bi bi-check-circle me-2"></i>Approve Registration
                    </div>
                    <div class="section-body">
                        <form id="approveForm" method="POST" action="<%= request.getContextPath() %>/ws/rs/registration/queue/<%= registration.getId() %>/approve">
                            <input type="hidden" name="OWASP_CSRFTOKEN" value="<%= Encode.forHtmlAttribute(csrfToken) %>">

                            <div class="mb-3">
                                <label class="form-label">Assign Provider <span class="text-danger">*</span></label>
                                <select class="form-select" name="provider_no" id="providerSelect" required>
                                    <option value="">Select a provider...</option>
                                    <% for (Provider provider : activeProviders) { %>
                                    <option value="<%= provider.getProviderNo() %>">
                                        <%= Encode.forHtml(provider.getFormattedName()) %>
                                    </option>
                                    <% } %>
                                </select>
                            </div>

                            <div class="alert alert-info small">
                                <i class="bi bi-info-circle me-1"></i>
                                Approving will create a new patient record and send a confirmation email to the patient.
                            </div>

                            <button type="button" class="btn btn-approve w-100" onclick="confirmApprove()">
                                <i class="bi bi-check-lg me-2"></i>Approve Registration
                            </button>
                        </form>
                    </div>
                </div>

                <!-- Rejection Form -->
                <div class="section-card">
                    <div class="section-header bg-danger text-white">
                        <i class="bi bi-x-circle me-2"></i>Reject Registration
                    </div>
                    <div class="section-body">
                        <form id="rejectForm" method="POST" action="<%= request.getContextPath() %>/ws/rs/registration/queue/<%= registration.getId() %>/reject">
                            <input type="hidden" name="OWASP_CSRFTOKEN" value="<%= Encode.forHtmlAttribute(csrfToken) %>">

                            <div class="mb-3">
                                <label class="form-label">Rejection Reason <span class="text-danger">*</span></label>
                                <textarea class="form-control" name="reason" id="rejectionReason" rows="3"
                                          placeholder="Enter reason for rejection..." required></textarea>
                            </div>

                            <div class="alert alert-warning small">
                                <i class="bi bi-exclamation-triangle me-1"></i>
                                The patient will be notified of the rejection via email with the reason provided.
                            </div>

                            <button type="button" class="btn btn-reject w-100" onclick="confirmReject()">
                                <i class="bi bi-x-lg me-2"></i>Reject Registration
                            </button>
                        </form>
                    </div>
                </div>
                <% } %>

                <!-- Print Button -->
                <div class="section-card no-print">
                    <div class="section-body text-center">
                        <button class="btn btn-outline-secondary" onclick="window.print()">
                            <i class="bi bi-printer me-2"></i>Print Registration
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- Confirmation Modals -->
    <div class="modal fade" id="confirmApproveModal" tabindex="-1">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header bg-success text-white">
                    <h5 class="modal-title"><i class="bi bi-check-circle me-2"></i>Confirm Approval</h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <p>Are you sure you want to approve this registration?</p>
                    <p class="mb-0"><strong>This will:</strong></p>
                    <ul class="mb-0">
                        <li>Create a new patient record in the system</li>
                        <li>Send a confirmation email to the patient</li>
                        <li>Assign the patient to the selected provider</li>
                    </ul>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                    <button type="button" class="btn btn-success" onclick="submitApprove()">
                        <i class="bi bi-check-lg me-2"></i>Yes, Approve
                    </button>
                </div>
            </div>
        </div>
    </div>

    <div class="modal fade" id="confirmRejectModal" tabindex="-1">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header bg-danger text-white">
                    <h5 class="modal-title"><i class="bi bi-x-circle me-2"></i>Confirm Rejection</h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <p>Are you sure you want to reject this registration?</p>
                    <p class="mb-0"><strong>The patient will be notified with the rejection reason:</strong></p>
                    <div class="alert alert-secondary mt-2" id="rejectReasonPreview"></div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                    <button type="button" class="btn btn-danger" onclick="submitReject()">
                        <i class="bi bi-x-lg me-2"></i>Yes, Reject
                    </button>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        function confirmApprove() {
            var providerSelect = document.getElementById('providerSelect');
            if (!providerSelect.value) {
                providerSelect.classList.add('is-invalid');
                providerSelect.focus();
                return;
            }
            providerSelect.classList.remove('is-invalid');
            var modal = new bootstrap.Modal(document.getElementById('confirmApproveModal'));
            modal.show();
        }

        function submitApprove() {
            document.getElementById('approveForm').submit();
        }

        function confirmReject() {
            var reasonInput = document.getElementById('rejectionReason');
            if (!reasonInput.value.trim()) {
                reasonInput.classList.add('is-invalid');
                reasonInput.focus();
                return;
            }
            reasonInput.classList.remove('is-invalid');
            document.getElementById('rejectReasonPreview').textContent = reasonInput.value;
            var modal = new bootstrap.Modal(document.getElementById('confirmRejectModal'));
            modal.show();
        }

        function submitReject() {
            document.getElementById('rejectForm').submit();
        }
    </script>
</body>
</html>
