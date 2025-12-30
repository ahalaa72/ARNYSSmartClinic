<%--
    Patient Self-Registration - Session Expired Page

    Displayed when a registration token has expired or is invalid.
    Instructs patient to scan the QR code again.

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
    <title>Session Expired</title>

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

        .expired-container {
            max-width: 500px;
            padding: 20px;
            text-align: center;
        }

        .expired-icon {
            width: 100px;
            height: 100px;
            background-color: #f8d7da;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            margin: 0 auto 24px;
        }

        .expired-icon i {
            font-size: 48px;
            color: #dc3545;
        }

        .expired-card {
            background: white;
            border-radius: 12px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            padding: 32px;
        }

        .expired-card h1 {
            color: #dc3545;
            font-size: 1.75rem;
            margin-bottom: 16px;
        }

        .steps-list {
            text-align: left;
            background-color: #f8f9fa;
            border-radius: 8px;
            padding: 16px;
            margin: 24px 0;
        }

        .steps-list li {
            margin-bottom: 12px;
        }

        .steps-list li:last-child {
            margin-bottom: 0;
        }

        .qr-icon {
            font-size: 64px;
            color: #6c757d;
            margin: 16px 0;
        }
    </style>
</head>
<body>
    <div class="expired-container">
        <div class="expired-card">
            <div class="expired-icon">
                <i class="bi bi-clock-history"></i>
            </div>

            <h1>Session Expired</h1>

            <p class="text-muted">
                Your registration session has expired or the link is no longer valid.
                Registration sessions are valid for 30 minutes for security purposes.
            </p>

            <div class="qr-icon">
                <i class="bi bi-qr-code-scan"></i>
            </div>

            <div class="steps-list">
                <h6 class="mb-3"><i class="bi bi-arrow-repeat me-2"></i>To continue:</h6>
                <ol class="mb-0">
                    <li>Return to the clinic waiting area</li>
                    <li>Scan the QR code again with your phone</li>
                    <li>Complete the registration form within 30 minutes</li>
                </ol>
            </div>

            <div class="alert alert-secondary mb-0">
                <i class="bi bi-question-circle me-2"></i>
                <small>If you need assistance, please speak with our reception staff.</small>
            </div>
        </div>

        <p class="text-muted mt-4 small">
            <i class="bi bi-shield-lock me-1"></i>
            This security measure protects your personal information.
        </p>
    </div>
</body>
</html>
