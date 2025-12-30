<%--
    Patient Self-Registration Form

    This is the public-facing patient registration form accessed via QR code scan.
    No authentication required - protected by session token.

    Features:
    - Mobile-responsive Bootstrap 5 layout
    - Real-time inline validation (HIN, postal code, phone)
    - AJAX submission with loading spinner
    - 30-minute session timeout countdown

    Part of the Patient Self-Registration Module
    @since 2025-12-29
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="robots" content="noindex, nofollow">
    <title>Patient Registration</title>

    <!-- Bootstrap 5 CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css" rel="stylesheet">

    <style>
        :root {
            --primary-color: #0d6efd;
            --success-color: #198754;
            --danger-color: #dc3545;
        }

        body {
            background-color: #f8f9fa;
            min-height: 100vh;
        }

        .registration-container {
            max-width: 600px;
            margin: 0 auto;
            padding: 20px;
        }

        .registration-header {
            text-align: center;
            padding: 20px 0;
        }

        .registration-header h1 {
            font-size: 1.75rem;
            color: #333;
        }

        .timer-badge {
            background-color: #fff3cd;
            color: #856404;
            padding: 8px 16px;
            border-radius: 20px;
            font-size: 0.9rem;
            display: inline-flex;
            align-items: center;
            gap: 8px;
        }

        .timer-badge.warning {
            background-color: #f8d7da;
            color: #721c24;
        }

        .form-card {
            background: white;
            border-radius: 12px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            padding: 24px;
            margin-bottom: 20px;
        }

        .section-title {
            font-size: 1.1rem;
            font-weight: 600;
            color: #333;
            margin-bottom: 16px;
            padding-bottom: 8px;
            border-bottom: 2px solid #e9ecef;
        }

        .form-label {
            font-weight: 500;
            color: #495057;
        }

        .required-asterisk {
            color: #dc3545;
        }

        .form-control:focus, .form-select:focus {
            border-color: var(--primary-color);
            box-shadow: 0 0 0 0.2rem rgba(13, 110, 253, 0.15);
        }

        .form-control.is-invalid {
            border-color: var(--danger-color);
            background-image: none;
        }

        .invalid-feedback {
            font-size: 0.85rem;
        }

        .btn-submit {
            width: 100%;
            padding: 12px;
            font-size: 1.1rem;
            font-weight: 500;
        }

        .loading-overlay {
            display: none;
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: rgba(255,255,255,0.9);
            z-index: 9999;
            justify-content: center;
            align-items: center;
            flex-direction: column;
        }

        .loading-overlay.show {
            display: flex;
        }

        .consent-section {
            background-color: #f8f9fa;
            border-radius: 8px;
            padding: 16px;
            margin-top: 16px;
        }

        .form-check-input:checked {
            background-color: var(--primary-color);
            border-color: var(--primary-color);
        }

        @media (max-width: 576px) {
            .registration-container {
                padding: 12px;
            }

            .form-card {
                padding: 16px;
            }

            .registration-header h1 {
                font-size: 1.5rem;
            }
        }
    </style>
</head>
<body>
    <!-- Loading Overlay -->
    <div class="loading-overlay" id="loadingOverlay">
        <div class="spinner-border text-primary mb-3" role="status" style="width: 3rem; height: 3rem;">
            <span class="visually-hidden">Loading...</span>
        </div>
        <p class="text-muted">Submitting your registration...</p>
    </div>

    <div class="registration-container">
        <!-- Header -->
        <div class="registration-header">
            <h1><i class="bi bi-clipboard-plus me-2"></i>Patient Registration</h1>
            <p class="text-muted mb-3">Please fill out the form below to register as a new patient</p>
            <div class="timer-badge" id="timerBadge">
                <i class="bi bi-clock"></i>
                <span id="timerText">Session expires in: <strong id="countdown">30:00</strong></span>
            </div>
        </div>

        <!-- Token Error (shown if invalid) -->
        <div class="alert alert-danger d-none" id="tokenError">
            <i class="bi bi-exclamation-triangle me-2"></i>
            <span id="tokenErrorMessage">Invalid or expired session</span>
        </div>

        <!-- Registration Form -->
        <form id="registrationForm" novalidate>
            <input type="hidden" id="token" name="token" value="">

            <!-- Personal Information -->
            <div class="form-card">
                <h2 class="section-title"><i class="bi bi-person me-2"></i>Personal Information</h2>

                <div class="row g-3">
                    <div class="col-md-6">
                        <label for="first_name" class="form-label">
                            First Name <span class="required-asterisk">*</span>
                        </label>
                        <input type="text" class="form-control" id="first_name" name="first_name"
                               maxlength="30" required autocomplete="given-name">
                        <div class="invalid-feedback" id="first_name_error"></div>
                    </div>

                    <div class="col-md-6">
                        <label for="last_name" class="form-label">
                            Last Name <span class="required-asterisk">*</span>
                        </label>
                        <input type="text" class="form-control" id="last_name" name="last_name"
                               maxlength="30" required autocomplete="family-name">
                        <div class="invalid-feedback" id="last_name_error"></div>
                    </div>

                    <div class="col-md-6">
                        <label for="middle_names" class="form-label">Middle Name(s)</label>
                        <input type="text" class="form-control" id="middle_names" name="middle_names"
                               maxlength="100" autocomplete="additional-name">
                    </div>

                    <div class="col-md-6">
                        <label for="pref_name" class="form-label">Preferred Name</label>
                        <input type="text" class="form-control" id="pref_name" name="pref_name"
                               maxlength="30" placeholder="If different from first name">
                    </div>

                    <div class="col-12">
                        <label class="form-label">
                            Date of Birth <span class="required-asterisk">*</span>
                        </label>
                        <div class="row g-2">
                            <div class="col-4">
                                <select class="form-select" id="year_of_birth" name="year_of_birth" required>
                                    <option value="">Year</option>
                                </select>
                            </div>
                            <div class="col-4">
                                <select class="form-select" id="month_of_birth" name="month_of_birth" required>
                                    <option value="">Month</option>
                                    <option value="01">January</option>
                                    <option value="02">February</option>
                                    <option value="03">March</option>
                                    <option value="04">April</option>
                                    <option value="05">May</option>
                                    <option value="06">June</option>
                                    <option value="07">July</option>
                                    <option value="08">August</option>
                                    <option value="09">September</option>
                                    <option value="10">October</option>
                                    <option value="11">November</option>
                                    <option value="12">December</option>
                                </select>
                            </div>
                            <div class="col-4">
                                <select class="form-select" id="date_of_birth" name="date_of_birth" required>
                                    <option value="">Day</option>
                                </select>
                            </div>
                        </div>
                        <div class="invalid-feedback d-block" id="dob_error"></div>
                    </div>

                    <div class="col-md-6">
                        <label for="sex" class="form-label">
                            Sex <span class="required-asterisk">*</span>
                        </label>
                        <select class="form-select" id="sex" name="sex" required>
                            <option value="">Select...</option>
                            <option value="M">Male</option>
                            <option value="F">Female</option>
                            <option value="O">Other</option>
                        </select>
                        <div class="invalid-feedback" id="sex_error"></div>
                    </div>

                    <div class="col-md-6">
                        <label for="gender" class="form-label">Gender Identity</label>
                        <input type="text" class="form-control" id="gender" name="gender"
                               maxlength="25" placeholder="Optional">
                    </div>
                </div>
            </div>

            <!-- Contact Information -->
            <div class="form-card">
                <h2 class="section-title"><i class="bi bi-telephone me-2"></i>Contact Information</h2>

                <div class="row g-3">
                    <div class="col-12">
                        <label for="address" class="form-label">
                            Street Address <span class="required-asterisk">*</span>
                        </label>
                        <input type="text" class="form-control" id="address" name="address"
                               maxlength="60" required autocomplete="street-address">
                        <div class="invalid-feedback" id="address_error"></div>
                    </div>

                    <div class="col-md-6">
                        <label for="city" class="form-label">
                            City <span class="required-asterisk">*</span>
                        </label>
                        <input type="text" class="form-control" id="city" name="city"
                               maxlength="50" required autocomplete="address-level2">
                        <div class="invalid-feedback" id="city_error"></div>
                    </div>

                    <div class="col-md-3">
                        <label for="province" class="form-label">
                            Province <span class="required-asterisk">*</span>
                        </label>
                        <select class="form-select" id="province" name="province" required>
                            <option value="">Select...</option>
                        </select>
                        <div class="invalid-feedback" id="province_error"></div>
                    </div>

                    <div class="col-md-3">
                        <label for="postal" class="form-label">
                            Postal Code <span class="required-asterisk">*</span>
                        </label>
                        <input type="text" class="form-control" id="postal" name="postal"
                               maxlength="7" required placeholder="A1A 1A1" autocomplete="postal-code">
                        <div class="invalid-feedback" id="postal_error"></div>
                    </div>

                    <div class="col-md-6">
                        <label for="cell_phone" class="form-label">
                            Cell Phone <span class="required-asterisk">*</span>
                        </label>
                        <input type="tel" class="form-control" id="cell_phone" name="cell_phone"
                               maxlength="14" required placeholder="416-555-1234" autocomplete="tel">
                        <div class="invalid-feedback" id="cell_phone_error"></div>
                    </div>

                    <div class="col-md-6">
                        <label for="phone" class="form-label">Home Phone</label>
                        <input type="tel" class="form-control" id="phone" name="phone"
                               maxlength="14" placeholder="Optional" autocomplete="tel-national">
                    </div>

                    <div class="col-12">
                        <label for="email" class="form-label">
                            Email Address <span class="required-asterisk">*</span>
                        </label>
                        <input type="email" class="form-control" id="email" name="email"
                               maxlength="100" required autocomplete="email">
                        <div class="invalid-feedback" id="email_error"></div>
                    </div>
                </div>
            </div>

            <!-- Health Card Information -->
            <div class="form-card">
                <h2 class="section-title"><i class="bi bi-card-text me-2"></i>Health Card Information</h2>

                <div class="row g-3">
                    <div class="col-md-4">
                        <label for="hc_type" class="form-label">Province</label>
                        <select class="form-select" id="hc_type" name="hc_type">
                            <option value="">Select...</option>
                        </select>
                        <div class="invalid-feedback" id="hc_type_error"></div>
                    </div>

                    <div class="col-md-5">
                        <label for="hin" class="form-label">Health Card Number</label>
                        <input type="text" class="form-control" id="hin" name="hin"
                               maxlength="12" placeholder="10 digits for Ontario">
                        <div class="invalid-feedback" id="hin_error"></div>
                    </div>

                    <div class="col-md-3">
                        <label for="ver" class="form-label">Version Code</label>
                        <input type="text" class="form-control" id="ver" name="ver"
                               maxlength="2" placeholder="2 letters">
                    </div>

                    <div class="col-12">
                        <small class="text-muted">
                            <i class="bi bi-info-circle me-1"></i>
                            Your health card number is optional but helps us verify your identity.
                        </small>
                    </div>
                </div>
            </div>

            <!-- Additional Information -->
            <div class="form-card">
                <h2 class="section-title"><i class="bi bi-info-circle me-2"></i>Additional Information</h2>

                <div class="row g-3">
                    <div class="col-md-6">
                        <label for="spoken_lang" class="form-label">Preferred Language</label>
                        <select class="form-select" id="spoken_lang" name="spoken_lang">
                            <option value="">Select...</option>
                        </select>
                    </div>

                    <div class="col-md-6">
                        <label for="emergency_contact_name" class="form-label">Emergency Contact Name</label>
                        <input type="text" class="form-control" id="emergency_contact_name"
                               name="emergency_contact_name" maxlength="60">
                    </div>

                    <div class="col-md-6">
                        <label for="emergency_contact_phone" class="form-label">Emergency Contact Phone</label>
                        <input type="tel" class="form-control" id="emergency_contact_phone"
                               name="emergency_contact_phone" maxlength="14" placeholder="416-555-1234">
                    </div>

                    <div class="col-md-6">
                        <label for="emergency_contact_relationship" class="form-label">Relationship</label>
                        <input type="text" class="form-control" id="emergency_contact_relationship"
                               name="emergency_contact_relationship" maxlength="30" placeholder="e.g., Spouse, Parent">
                    </div>
                </div>

                <!-- Consent -->
                <div class="consent-section">
                    <div class="form-check">
                        <input class="form-check-input" type="checkbox" id="consent_email" name="consent_email">
                        <label class="form-check-label" for="consent_email">
                            I consent to receive email communications regarding my healthcare
                        </label>
                    </div>
                </div>
            </div>

            <!-- Submit Button -->
            <div class="d-grid gap-2">
                <button type="submit" class="btn btn-primary btn-submit" id="submitBtn">
                    <i class="bi bi-check-circle me-2"></i>Submit Registration
                </button>
            </div>

            <p class="text-center text-muted mt-3 small">
                <i class="bi bi-shield-lock me-1"></i>
                Your information is protected and will only be used for healthcare purposes.
            </p>
        </form>
    </div>

    <!-- Bootstrap JS -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>

    <script>
        // Configuration
        const API_BASE = '../ws/rs/registration';
        let sessionExpiry = null;
        let countdownInterval = null;

        // Get token from URL
        const urlParams = new URLSearchParams(window.location.search);
        const token = urlParams.get('token');

        // Initialize on page load
        document.addEventListener('DOMContentLoaded', function() {
            if (!token) {
                showTokenError('No registration token provided. Please scan the QR code again.');
                return;
            }

            document.getElementById('token').value = token;

            // Load form configuration
            loadFormConfig();

            // Validate token
            validateToken();

            // Populate year dropdown
            populateYearDropdown();

            // Populate day dropdown
            populateDayDropdown();

            // Set up form validation
            setupValidation();

            // Set up form submission
            document.getElementById('registrationForm').addEventListener('submit', handleSubmit);
        });

        // Load form configuration (provinces, languages)
        async function loadFormConfig() {
            try {
                const response = await fetch(API_BASE + '/config');
                const config = await response.json();

                // Populate provinces
                const provinceSelect = document.getElementById('province');
                const hcTypeSelect = document.getElementById('hc_type');

                config.provinces.forEach(p => {
                    provinceSelect.add(new Option(p.name, p.code));
                    hcTypeSelect.add(new Option(p.name, p.code));
                });

                // Set defaults
                provinceSelect.value = config.defaults.province || 'ON';
                hcTypeSelect.value = config.defaults.province || 'ON';

                // Populate languages
                const langSelect = document.getElementById('spoken_lang');
                config.languages.forEach(l => {
                    langSelect.add(new Option(l.name, l.name));
                });
                langSelect.value = 'English';

            } catch (error) {
                console.error('Failed to load config:', error);
            }
        }

        // Validate token with server
        async function validateToken() {
            try {
                const response = await fetch(API_BASE + '/validate-token', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ token: token })
                });

                const result = await response.json();

                if (!result.valid) {
                    showTokenError(result.error || 'Session has expired. Please scan the QR code again.');
                    return;
                }

                // Start countdown timer
                sessionExpiry = new Date(result.expires_at);
                startCountdown(result.remaining_seconds);

            } catch (error) {
                console.error('Token validation failed:', error);
                showTokenError('Unable to verify session. Please try again.');
            }
        }

        // Show token error
        function showTokenError(message) {
            document.getElementById('tokenErrorMessage').textContent = message;
            document.getElementById('tokenError').classList.remove('d-none');
            document.getElementById('registrationForm').style.display = 'none';
        }

        // Start countdown timer
        function startCountdown(seconds) {
            let remaining = seconds;

            updateCountdownDisplay(remaining);

            countdownInterval = setInterval(() => {
                remaining--;

                if (remaining <= 0) {
                    clearInterval(countdownInterval);
                    showTokenError('Your session has expired. Please scan the QR code again.');
                    return;
                }

                updateCountdownDisplay(remaining);

                // Show warning when less than 5 minutes
                if (remaining < 300) {
                    document.getElementById('timerBadge').classList.add('warning');
                }
            }, 1000);
        }

        // Update countdown display
        function updateCountdownDisplay(seconds) {
            const minutes = Math.floor(seconds / 60);
            const secs = seconds % 60;
            document.getElementById('countdown').textContent =
                minutes.toString().padStart(2, '0') + ':' + secs.toString().padStart(2, '0');
        }

        // Populate year dropdown
        function populateYearDropdown() {
            const yearSelect = document.getElementById('year_of_birth');
            const currentYear = new Date().getFullYear();

            for (let year = currentYear; year >= 1900; year--) {
                yearSelect.add(new Option(year, year));
            }
        }

        // Populate day dropdown
        function populateDayDropdown() {
            const daySelect = document.getElementById('date_of_birth');

            for (let day = 1; day <= 31; day++) {
                const dayStr = day.toString().padStart(2, '0');
                daySelect.add(new Option(day, dayStr));
            }
        }

        // Setup real-time validation
        function setupValidation() {
            // HIN validation
            document.getElementById('hin').addEventListener('blur', validateHin);
            document.getElementById('hc_type').addEventListener('change', validateHin);

            // Postal code formatting
            document.getElementById('postal').addEventListener('blur', function() {
                let postal = this.value.toUpperCase().replace(/[^A-Z0-9]/g, '');
                if (postal.length === 6) {
                    this.value = postal.substring(0, 3) + ' ' + postal.substring(3);
                }
                validatePostal();
            });

            // Phone formatting
            ['cell_phone', 'phone', 'emergency_contact_phone'].forEach(id => {
                const el = document.getElementById(id);
                if (el) {
                    el.addEventListener('blur', function() {
                        formatPhone(this);
                    });
                }
            });

            // Email validation
            document.getElementById('email').addEventListener('blur', validateEmail);
        }

        // Validate HIN (Ontario MOD-10)
        function validateHin() {
            const hin = document.getElementById('hin').value.replace(/\D/g, '');
            const province = document.getElementById('hc_type').value;
            const hinInput = document.getElementById('hin');
            const hinError = document.getElementById('hin_error');

            if (!hin) {
                hinInput.classList.remove('is-invalid');
                hinError.textContent = '';
                return true;
            }

            if (province === 'ON') {
                if (hin.length !== 10) {
                    hinInput.classList.add('is-invalid');
                    hinError.textContent = 'Ontario health card must be exactly 10 digits';
                    return false;
                }

                if (!validateMod10(hin)) {
                    hinInput.classList.add('is-invalid');
                    hinError.textContent = 'Invalid health card number (checksum failed)';
                    return false;
                }
            }

            hinInput.classList.remove('is-invalid');
            hinError.textContent = '';
            return true;
        }

        // MOD-10 (Luhn) algorithm
        function validateMod10(number) {
            let sum = 0;
            let alternate = false;

            for (let i = number.length - 1; i >= 0; i--) {
                let digit = parseInt(number.charAt(i), 10);

                if (alternate) {
                    digit *= 2;
                    if (digit > 9) {
                        digit = Math.floor(digit / 10) + (digit % 10);
                    }
                }

                sum += digit;
                alternate = !alternate;
            }

            return (sum % 10) === 0;
        }

        // Validate postal code
        function validatePostal() {
            const postal = document.getElementById('postal').value;
            const postalInput = document.getElementById('postal');
            const postalError = document.getElementById('postal_error');

            if (!postal) return true;

            const pattern = /^[A-Za-z]\d[A-Za-z][ -]?\d[A-Za-z]\d$/;

            if (!pattern.test(postal)) {
                postalInput.classList.add('is-invalid');
                postalError.textContent = 'Please enter a valid postal code (e.g., A1A 1A1)';
                return false;
            }

            postalInput.classList.remove('is-invalid');
            postalError.textContent = '';
            return true;
        }

        // Validate email
        function validateEmail() {
            const email = document.getElementById('email').value;
            const emailInput = document.getElementById('email');
            const emailError = document.getElementById('email_error');

            if (!email) return true;

            const pattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

            if (!pattern.test(email)) {
                emailInput.classList.add('is-invalid');
                emailError.textContent = 'Please enter a valid email address';
                return false;
            }

            emailInput.classList.remove('is-invalid');
            emailError.textContent = '';
            return true;
        }

        // Format phone number
        function formatPhone(input) {
            let digits = input.value.replace(/\D/g, '');

            if (digits.length === 10) {
                input.value = digits.substring(0, 3) + '-' +
                              digits.substring(3, 6) + '-' +
                              digits.substring(6);
            }
        }

        // Handle form submission
        async function handleSubmit(event) {
            event.preventDefault();

            // Clear previous errors
            document.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
            document.querySelectorAll('.invalid-feedback').forEach(el => el.textContent = '');

            // Collect form data
            const formData = {
                token: document.getElementById('token').value,
                patient_data: {
                    first_name: document.getElementById('first_name').value.trim(),
                    last_name: document.getElementById('last_name').value.trim(),
                    middle_names: document.getElementById('middle_names').value.trim(),
                    pref_name: document.getElementById('pref_name').value.trim(),
                    year_of_birth: document.getElementById('year_of_birth').value,
                    month_of_birth: document.getElementById('month_of_birth').value,
                    date_of_birth: document.getElementById('date_of_birth').value,
                    sex: document.getElementById('sex').value,
                    gender: document.getElementById('gender').value.trim(),
                    address: document.getElementById('address').value.trim(),
                    city: document.getElementById('city').value.trim(),
                    province: document.getElementById('province').value,
                    postal: document.getElementById('postal').value.trim(),
                    cell_phone: document.getElementById('cell_phone').value.trim(),
                    phone: document.getElementById('phone').value.trim(),
                    email: document.getElementById('email').value.trim(),
                    hin: document.getElementById('hin').value.replace(/\D/g, ''),
                    ver: document.getElementById('ver').value.trim().toUpperCase(),
                    hc_type: document.getElementById('hc_type').value,
                    spoken_lang: document.getElementById('spoken_lang').value,
                    emergency_contact_name: document.getElementById('emergency_contact_name').value.trim(),
                    emergency_contact_phone: document.getElementById('emergency_contact_phone').value.trim(),
                    emergency_contact_relationship: document.getElementById('emergency_contact_relationship').value.trim(),
                    consent_email: document.getElementById('consent_email').checked
                }
            };

            // Show loading
            document.getElementById('loadingOverlay').classList.add('show');
            document.getElementById('submitBtn').disabled = true;

            try {
                const response = await fetch(API_BASE + '/submit', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(formData)
                });

                const result = await response.json();

                if (result.success) {
                    // Redirect to success page
                    window.location.href = 'success.jsp?id=' + result.queue_id;
                } else if (result.errors) {
                    // Show field errors
                    Object.entries(result.errors).forEach(([field, message]) => {
                        const input = document.getElementById(field);
                        const error = document.getElementById(field + '_error');

                        if (input) {
                            input.classList.add('is-invalid');
                        }
                        if (error) {
                            error.textContent = message;
                        }
                    });

                    // Scroll to first error
                    const firstError = document.querySelector('.is-invalid');
                    if (firstError) {
                        firstError.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    }
                } else {
                    alert(result.error || 'An error occurred. Please try again.');
                }

            } catch (error) {
                console.error('Submission failed:', error);
                alert('Unable to submit registration. Please check your connection and try again.');
            } finally {
                document.getElementById('loadingOverlay').classList.remove('show');
                document.getElementById('submitBtn').disabled = false;
            }
        }
    </script>
</body>
</html>
