<%--
    Patient Self-Registration - Success Page

    Displayed after successful registration submission.
    Informs patient that their registration is pending staff review.

    Part of the Patient Self-Registration Module
    @since 2025-12-29
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="robots" content="noindex, nofollow">
    <title>Registration Submitted</title>

    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css" rel="stylesheet">

    <style>
        body {
            background-color: #f8f9fa;
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
        }

        .success-container {
            max-width: 500px;
            padding: 20px;
            text-align: center;
        }

        .success-icon {
            width: 100px;
            height: 100px;
            background-color: #d1e7dd;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            margin: 0 auto 24px;
        }

        .success-icon i {
            font-size: 48px;
            color: #198754;
        }

        .success-card {
            background: white;
            border-radius: 12px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            padding: 32px;
        }

        .success-card h1 {
            color: #198754;
            font-size: 1.75rem;
            margin-bottom: 16px;
        }

        .info-list {
            text-align: left;
            background-color: #f8f9fa;
            border-radius: 8px;
            padding: 16px;
            margin: 24px 0;
        }

        .info-list li {
            margin-bottom: 8px;
        }

        .info-list li:last-child {
            margin-bottom: 0;
        }

        .confirmation-number {
            background-color: #e9ecef;
            padding: 12px;
            border-radius: 8px;
            font-family: monospace;
            font-size: 1.2rem;
            margin: 16px 0;
        }
    </style>
</head>
<body>
    <div class="success-container">
        <div class="success-card">
            <div class="success-icon">
                <i class="bi bi-check-lg"></i>
            </div>

            <h1>Registration Submitted!</h1>

            <p class="text-muted">
                Thank you for registering. Your information has been submitted
                and is now pending review by our clinic staff.
            </p>

            <div class="confirmation-number">
                Confirmation #: REG-<%= request.getParameter("id") != null ? request.getParameter("id") : "N/A" %>
            </div>

            <div class="info-list">
                <h6 class="mb-3"><i class="bi bi-info-circle me-2"></i>What happens next?</h6>
                <ul class="mb-0">
                    <li>Our staff will review your registration</li>
                    <li>You will receive an email confirmation once approved</li>
                    <li>This typically takes 1-2 business days</li>
                    <li>You can then book your first appointment</li>
                </ul>
            </div>

            <div class="alert alert-info mb-0">
                <i class="bi bi-envelope me-2"></i>
                <small>Please check your email (including spam folder) for updates about your registration.</small>
            </div>
        </div>

        <p class="text-muted mt-4 small">
            You may now close this window.
        </p>
    </div>
</body>
</html>
