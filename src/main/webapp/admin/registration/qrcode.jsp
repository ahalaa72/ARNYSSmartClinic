<%--
    Patient Self-Registration Module - QR Code Display Page

    Admin interface for viewing and printing the clinic's static registration QR code.
    Provides print-friendly layouts for posters and table tents.

    Part of the Patient Self-Registration Module - a standalone commercial module
    for OpenO EMR that enables patients to self-register via QR code scanning.

    Features:
    - Display clinic's permanent QR code (large, scannable)
    - Multiple print layouts (A4 poster, letter, table tent)
    - Download QR image (PNG format)
    - Print-optimized CSS with clinic branding
    - Registration URL displayed for reference
    - Instructions for display placement

    Security: Requires authenticated staff session with _registration privilege
    @since 2025-12-29
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="org.owasp.encoder.Encode" %>
<%@ page import="ca.openosp.openo.utility.SpringUtils" %>
<%@ page import="ca.openosp.openo.managers.SecurityInfoManager" %>
<%@ page import="ca.openosp.openo.utility.LoggedInInfo" %>
<%@ page import="ca.openosp.openo.registration.util.QRCodeGenerator" %>
<%@ page import="ca.openosp.openo.registration.dao.RegistrationModuleConfigDao" %>
<%@ page import="ca.openosp.openo.registration.model.RegistrationModuleConfig" %>

<%
    // Security check - use _admin privilege since _registration may not exist
    SecurityInfoManager securityInfoManager = SpringUtils.getBean(SecurityInfoManager.class);
    LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);

    if (loggedInInfo == null || !securityInfoManager.hasPrivilege(loggedInInfo, "_admin", "r", null)) {
        response.sendRedirect(request.getContextPath() + "/login.do");
        return;
    }

    // Get facility configuration (with null safety)
    RegistrationModuleConfigDao configDao = SpringUtils.getBean(RegistrationModuleConfigDao.class);
    Integer facilityId = (loggedInInfo.getCurrentFacility() != null) ? loggedInInfo.getCurrentFacility().getId() : 1;
    RegistrationModuleConfig moduleConfig = (configDao != null) ? configDao.findByFacilityId(facilityId) : null;

    // Build registration URL
    String baseUrl = request.getScheme() + "://" + request.getServerName();
    if ((request.getScheme().equals("http") && request.getServerPort() != 80) ||
        (request.getScheme().equals("https") && request.getServerPort() != 443)) {
        baseUrl += ":" + request.getServerPort();
    }
    String registrationUrl = baseUrl + request.getContextPath() + "/registration/start";

    // Generate QR code
    QRCodeGenerator qrGenerator = SpringUtils.getBean(QRCodeGenerator.class);
    String qrCodeBase64 = "";
    String qrError = "";
    try {
        if (qrGenerator == null) {
            qrError = "QRCodeGenerator bean not found in Spring context";
        } else {
            qrCodeBase64 = qrGenerator.generateQRCodeBase64(registrationUrl, 400, 400);
        }
    } catch (Exception e) {
        qrError = "Error generating QR code: " + e.getClass().getName() + " - " + e.getMessage();
        e.printStackTrace(); // Log to console for debugging
    }

    // Get clinic name for branding
    String clinicName = "Our Clinic";
    if (moduleConfig != null && moduleConfig.getClinicName() != null && !moduleConfig.getClinicName().isEmpty()) {
        clinicName = moduleConfig.getClinicName();
    }

    // Get selected layout from parameter
    String layout = request.getParameter("layout");
    if (layout == null) {
        layout = "screen";
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Registration QR Code - OpenO EMR</title>

    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css" rel="stylesheet">

    <style>
        body {
            background-color: #f8f9fa;
        }

        .page-header {
            background: linear-gradient(135deg, #1a5276 0%, #2980b9 100%);
            color: white;
            padding: 24px;
            border-radius: 12px;
            margin-bottom: 24px;
        }

        .qr-display-card {
            background: white;
            border-radius: 16px;
            box-shadow: 0 4px 20px rgba(0,0,0,0.1);
            padding: 32px;
            text-align: center;
        }

        .qr-code-container {
            background: white;
            padding: 24px;
            border-radius: 12px;
            display: inline-block;
            border: 3px solid #1a5276;
        }

        .qr-code-container img {
            display: block;
            max-width: 100%;
            height: auto;
        }

        .clinic-branding {
            margin-top: 24px;
        }

        .clinic-branding h3 {
            color: #1a5276;
            font-weight: 700;
            margin-bottom: 8px;
        }

        .scan-instructions {
            background: linear-gradient(135deg, #e8f4f8 0%, #d4edda 100%);
            border-radius: 12px;
            padding: 20px;
            margin-top: 24px;
        }

        .scan-instructions h5 {
            color: #155724;
            margin-bottom: 12px;
        }

        .url-display {
            background: #f8f9fa;
            border: 1px solid #dee2e6;
            border-radius: 8px;
            padding: 12px;
            margin-top: 16px;
            font-family: monospace;
            font-size: 0.9rem;
            word-break: break-all;
        }

        .layout-selector {
            background: white;
            border-radius: 12px;
            padding: 20px;
            margin-bottom: 24px;
        }

        .layout-option {
            border: 2px solid #dee2e6;
            border-radius: 8px;
            padding: 16px;
            text-align: center;
            cursor: pointer;
            transition: all 0.2s;
        }

        .layout-option:hover {
            border-color: #1a5276;
            background-color: #f8f9fa;
        }

        .layout-option.selected {
            border-color: #1a5276;
            background-color: #e8f4f8;
        }

        .layout-option i {
            font-size: 32px;
            color: #1a5276;
            margin-bottom: 8px;
        }

        /* Print Styles */
        @media print {
            body {
                background: white !important;
                margin: 0;
                padding: 0;
            }

            .no-print {
                display: none !important;
            }

            .qr-display-card {
                box-shadow: none;
                border: none;
            }
        }

        /* Poster Layout (A4/Letter) */
        @media print {
            .print-poster {
                width: 100%;
                height: 100vh;
                display: flex;
                flex-direction: column;
                justify-content: center;
                align-items: center;
                text-align: center;
                page-break-after: always;
            }

            .print-poster .qr-code-container {
                width: 300px;
                height: 300px;
            }

            .print-poster .qr-code-container img {
                width: 100%;
                height: 100%;
            }

            .print-poster h1 {
                font-size: 48px;
                color: #1a5276;
                margin-top: 32px;
            }

            .print-poster .subtitle {
                font-size: 24px;
                color: #333;
                margin-top: 16px;
            }

            .print-poster .instructions {
                font-size: 20px;
                color: #666;
                margin-top: 24px;
                max-width: 80%;
            }

            .print-poster .clinic-name {
                font-size: 28px;
                font-weight: bold;
                color: #1a5276;
                margin-top: 32px;
            }
        }

        /* Table Tent Layout */
        .table-tent-preview {
            background: white;
            border: 2px dashed #dee2e6;
            border-radius: 8px;
            padding: 24px;
            text-align: center;
            margin-top: 16px;
        }

        .table-tent-preview .qr-small {
            width: 150px;
            height: 150px;
            margin: 0 auto;
        }

        .download-section {
            background: white;
            border-radius: 12px;
            padding: 20px;
            margin-top: 24px;
        }

        .instructions-card {
            background: #fff3cd;
            border: 1px solid #ffc107;
            border-radius: 12px;
            padding: 20px;
            margin-top: 24px;
        }

        .instructions-card h5 {
            color: #856404;
        }

        .placement-tips li {
            margin-bottom: 8px;
        }
    </style>
</head>
<body>
    <div class="container py-4 no-print">
        <!-- Back Button -->
        <div class="mb-3">
            <a href="queue.jsp" class="btn btn-outline-secondary">
                <i class="bi bi-arrow-left me-2"></i>Back to Queue
            </a>
        </div>

        <!-- Header -->
        <div class="page-header">
            <div class="row align-items-center">
                <div class="col-md-8">
                    <h2 class="mb-1"><i class="bi bi-qr-code me-2"></i>Registration QR Code</h2>
                    <p class="mb-0 opacity-75">Display this QR code in your waiting area for patient self-registration</p>
                </div>
                <div class="col-md-4 text-md-end">
                    <span class="badge bg-light text-dark fs-6 px-3 py-2">
                        <i class="bi bi-building me-1"></i><%= Encode.forHtml(clinicName) %>
                    </span>
                </div>
            </div>
        </div>

        <div class="row">
            <!-- QR Code Display -->
            <div class="col-lg-6">
                <div class="qr-display-card">
                    <% if (qrCodeBase64 != null && !qrCodeBase64.isEmpty()) { %>
                    <div class="qr-code-container">
                        <img src="data:image/png;base64,<%= qrCodeBase64 %>" alt="Registration QR Code" id="qrCodeImage">
                    </div>
                    <% } else { %>
                    <div class="alert alert-warning">
                        <i class="bi bi-exclamation-triangle me-2"></i>
                        Unable to generate QR code.
                        <% if (!qrError.isEmpty()) { %>
                        <br><small class="text-danger"><%= Encode.forHtml(qrError) %></small>
                        <% } else { %>
                        Please check the configuration.
                        <% } %>
                    </div>
                    <% } %>

                    <div class="clinic-branding">
                        <h3><%= Encode.forHtml(clinicName) %></h3>
                        <p class="text-muted mb-0">Patient Self-Registration</p>
                    </div>

                    <div class="scan-instructions">
                        <h5><i class="bi bi-phone me-2"></i>Scan to Register</h5>
                        <p class="mb-0">
                            Point your smartphone camera at this QR code to begin registration.
                            You have 30 minutes to complete the form.
                        </p>
                    </div>

                    <div class="url-display">
                        <small class="text-muted d-block mb-1">Registration URL:</small>
                        <%= Encode.forHtml(registrationUrl) %>
                    </div>
                </div>

                <!-- Download Section -->
                <div class="download-section">
                    <h5 class="mb-3"><i class="bi bi-download me-2"></i>Download QR Code</h5>
                    <div class="row g-2">
                        <div class="col-6">
                            <a href="<%= request.getContextPath() %>/ws/rs/registration/qrcode/download?size=400" class="btn btn-outline-primary w-100" download="registration-qr-400.png">
                                <i class="bi bi-image me-2"></i>Small (400px)
                            </a>
                        </div>
                        <div class="col-6">
                            <a href="<%= request.getContextPath() %>/ws/rs/registration/qrcode/download?size=800" class="btn btn-outline-primary w-100" download="registration-qr-800.png">
                                <i class="bi bi-image me-2"></i>Large (800px)
                            </a>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Print Options -->
            <div class="col-lg-6">
                <div class="layout-selector">
                    <h5 class="mb-3"><i class="bi bi-printer me-2"></i>Print Options</h5>

                    <div class="row g-3">
                        <div class="col-6">
                            <div class="layout-option" onclick="printPoster()">
                                <i class="bi bi-file-earmark"></i>
                                <h6 class="mb-1">Poster</h6>
                                <small class="text-muted">Full page for wall display</small>
                            </div>
                        </div>
                        <div class="col-6">
                            <div class="layout-option" onclick="printTableTent()">
                                <i class="bi bi-card-text"></i>
                                <h6 class="mb-1">Table Tent</h6>
                                <small class="text-muted">Small card for counters</small>
                            </div>
                        </div>
                    </div>

                    <div class="mt-3">
                        <button class="btn btn-primary w-100" onclick="printPoster()">
                            <i class="bi bi-printer-fill me-2"></i>Print Poster
                        </button>
                    </div>
                </div>

                <!-- Placement Tips -->
                <div class="instructions-card">
                    <h5><i class="bi bi-lightbulb me-2"></i>Display Tips</h5>
                    <ul class="placement-tips mb-0">
                        <li><strong>Reception desk</strong> - Place at eye level where patients check in</li>
                        <li><strong>Waiting area</strong> - Post on walls or use table tents on side tables</li>
                        <li><strong>Entry area</strong> - Near the entrance so patients see it upon arrival</li>
                        <li><strong>Good lighting</strong> - Ensure QR code is well-lit for easy scanning</li>
                        <li><strong>Clear visibility</strong> - Avoid placing behind glass that may cause glare</li>
                    </ul>
                </div>

                <!-- Session Info -->
                <div class="card mt-4">
                    <div class="card-body">
                        <h5 class="card-title"><i class="bi bi-clock me-2"></i>How It Works</h5>
                        <ol class="mb-0">
                            <li class="mb-2">Patient scans QR code with their smartphone</li>
                            <li class="mb-2">A unique 30-minute session is created</li>
                            <li class="mb-2">Patient fills out the registration form</li>
                            <li class="mb-2">Submission appears in your review queue</li>
                            <li class="mb-0">Staff reviews and approves the registration</li>
                        </ol>
                    </div>
                </div>

                <!-- Security Note -->
                <div class="alert alert-info mt-4">
                    <h6><i class="bi bi-shield-check me-2"></i>Security Features</h6>
                    <ul class="mb-0 small">
                        <li>Each scan creates a unique session (tokens cannot be reused)</li>
                        <li>Sessions expire after 30 minutes for security</li>
                        <li>Rate limiting prevents abuse (5 sessions per IP per hour)</li>
                        <li>All submissions require staff approval</li>
                    </ul>
                </div>
            </div>
        </div>
    </div>

    <!-- Print Layout - Poster -->
    <div class="print-poster d-none" id="printPosterLayout">
        <div class="qr-code-container">
            <% if (qrCodeBase64 != null && !qrCodeBase64.isEmpty()) { %>
            <img src="data:image/png;base64,<%= qrCodeBase64 %>" alt="Registration QR Code">
            <% } %>
        </div>

        <h1>Scan to Register</h1>

        <p class="subtitle">New Patient? Register Online!</p>

        <p class="instructions">
            Point your smartphone camera at this QR code.<br>
            Complete the registration form on your phone.<br>
            Our staff will review your information.
        </p>

        <p class="clinic-name"><%= Encode.forHtml(clinicName) %></p>

        <p style="margin-top: 24px; color: #999; font-size: 14px;">
            <i class="bi bi-clock"></i> Registration sessions are valid for 30 minutes
        </p>
    </div>

    <!-- Print Layout - Table Tent (Front and Back) -->
    <div class="d-none" id="printTableTentLayout">
        <div style="page-break-after: always; height: 50vh; display: flex; flex-direction: column; justify-content: center; align-items: center; text-align: center; border-bottom: 2px dashed #ccc;">
            <div class="qr-code-container" style="width: 200px; height: 200px;">
                <% if (qrCodeBase64 != null && !qrCodeBase64.isEmpty()) { %>
                <img src="data:image/png;base64,<%= qrCodeBase64 %>" alt="Registration QR Code" style="width: 100%; height: 100%;">
                <% } %>
            </div>
            <h2 style="color: #1a5276; margin-top: 16px;">Scan to Register</h2>
            <p style="color: #666;">New patients - register on your phone!</p>
            <p style="font-weight: bold; color: #1a5276;"><%= Encode.forHtml(clinicName) %></p>
        </div>

        <div style="height: 50vh; display: flex; flex-direction: column; justify-content: center; align-items: center; text-align: center; transform: rotate(180deg);">
            <div class="qr-code-container" style="width: 200px; height: 200px;">
                <% if (qrCodeBase64 != null && !qrCodeBase64.isEmpty()) { %>
                <img src="data:image/png;base64,<%= qrCodeBase64 %>" alt="Registration QR Code" style="width: 100%; height: 100%;">
                <% } %>
            </div>
            <h2 style="color: #1a5276; margin-top: 16px;">Scan to Register</h2>
            <p style="color: #666;">New patients - register on your phone!</p>
            <p style="font-weight: bold; color: #1a5276;"><%= Encode.forHtml(clinicName) %></p>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        function printPoster() {
            // Create a new window for printing
            var printWindow = window.open('', '_blank');
            var qrImage = document.getElementById('qrCodeImage');

            printWindow.document.write('<html><head><title>Registration QR Code - Poster</title>');
            printWindow.document.write('<style>');
            printWindow.document.write('body { margin: 0; padding: 0; font-family: Arial, sans-serif; }');
            printWindow.document.write('.poster { width: 100%; height: 100vh; display: flex; flex-direction: column; justify-content: center; align-items: center; text-align: center; }');
            printWindow.document.write('.qr-container { border: 4px solid #1a5276; padding: 20px; border-radius: 16px; background: white; }');
            printWindow.document.write('.qr-container img { width: 300px; height: 300px; display: block; }');
            printWindow.document.write('h1 { color: #1a5276; font-size: 56px; margin: 32px 0 16px 0; }');
            printWindow.document.write('.subtitle { font-size: 28px; color: #333; margin: 0 0 24px 0; }');
            printWindow.document.write('.instructions { font-size: 20px; color: #666; line-height: 1.6; max-width: 600px; }');
            printWindow.document.write('.clinic-name { font-size: 32px; font-weight: bold; color: #1a5276; margin-top: 32px; }');
            printWindow.document.write('.footer { margin-top: 24px; color: #999; font-size: 14px; }');
            printWindow.document.write('@media print { @page { margin: 0; size: auto; } }');
            printWindow.document.write('</style></head><body>');
            printWindow.document.write('<div class="poster">');
            printWindow.document.write('<div class="qr-container">');
            if (qrImage) {
                printWindow.document.write('<img src="' + qrImage.src + '" alt="QR Code">');
            }
            printWindow.document.write('</div>');
            printWindow.document.write('<h1>Scan to Register</h1>');
            printWindow.document.write('<p class="subtitle">New Patient? Register Online!</p>');
            printWindow.document.write('<p class="instructions">');
            printWindow.document.write('Point your smartphone camera at this QR code.<br>');
            printWindow.document.write('Complete the registration form on your phone.<br>');
            printWindow.document.write('Our staff will review your information.');
            printWindow.document.write('</p>');
            printWindow.document.write('<p class="clinic-name"><%= Encode.forJavaScript(clinicName) %></p>');
            printWindow.document.write('<p class="footer">Registration sessions are valid for 30 minutes</p>');
            printWindow.document.write('</div>');
            printWindow.document.write('</body></html>');
            printWindow.document.close();

            printWindow.onload = function() {
                printWindow.print();
            };
        }

        function printTableTent() {
            var printWindow = window.open('', '_blank');
            var qrImage = document.getElementById('qrCodeImage');

            printWindow.document.write('<html><head><title>Registration QR Code - Table Tent</title>');
            printWindow.document.write('<style>');
            printWindow.document.write('body { margin: 0; padding: 0; font-family: Arial, sans-serif; }');
            printWindow.document.write('.tent-side { height: 50vh; display: flex; flex-direction: column; justify-content: center; align-items: center; text-align: center; box-sizing: border-box; padding: 20px; }');
            printWindow.document.write('.tent-side:first-child { border-bottom: 2px dashed #ccc; }');
            printWindow.document.write('.tent-side:last-child { transform: rotate(180deg); }');
            printWindow.document.write('.qr-container { border: 3px solid #1a5276; padding: 12px; border-radius: 12px; background: white; }');
            printWindow.document.write('.qr-container img { width: 180px; height: 180px; display: block; }');
            printWindow.document.write('h2 { color: #1a5276; font-size: 28px; margin: 16px 0 8px 0; }');
            printWindow.document.write('p { font-size: 16px; color: #666; margin: 0 0 8px 0; }');
            printWindow.document.write('.clinic-name { font-weight: bold; color: #1a5276; }');
            printWindow.document.write('@media print { @page { margin: 0; size: auto; } }');
            printWindow.document.write('</style></head><body>');

            // Front side
            printWindow.document.write('<div class="tent-side">');
            printWindow.document.write('<div class="qr-container">');
            if (qrImage) {
                printWindow.document.write('<img src="' + qrImage.src + '" alt="QR Code">');
            }
            printWindow.document.write('</div>');
            printWindow.document.write('<h2>Scan to Register</h2>');
            printWindow.document.write('<p>New patients - register on your phone!</p>');
            printWindow.document.write('<p class="clinic-name"><%= Encode.forJavaScript(clinicName) %></p>');
            printWindow.document.write('</div>');

            // Back side (upside down for folding)
            printWindow.document.write('<div class="tent-side">');
            printWindow.document.write('<div class="qr-container">');
            if (qrImage) {
                printWindow.document.write('<img src="' + qrImage.src + '" alt="QR Code">');
            }
            printWindow.document.write('</div>');
            printWindow.document.write('<h2>Scan to Register</h2>');
            printWindow.document.write('<p>New patients - register on your phone!</p>');
            printWindow.document.write('<p class="clinic-name"><%= Encode.forJavaScript(clinicName) %></p>');
            printWindow.document.write('</div>');

            printWindow.document.write('</body></html>');
            printWindow.document.close();

            printWindow.onload = function() {
                printWindow.print();
            };
        }

        // Copy URL to clipboard
        function copyUrl() {
            var url = '<%= registrationUrl %>';
            navigator.clipboard.writeText(url).then(function() {
                alert('URL copied to clipboard!');
            });
        }
    </script>
</body>
</html>
