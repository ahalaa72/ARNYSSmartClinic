# Patient Self-Registration Module

## Overview

The Patient Self-Registration Module is a standalone commercial module for OpenO EMR that enables patients to self-register via QR code scanning. This module is designed to be completely independent from the core OpenO EMR implementation, making it easy to deploy, maintain, and sell to multiple clinics.

### Key Features

- **QR Code-Based Registration**: Static QR code displayed in clinic waiting area
- **Mobile-Responsive Form**: Patients complete registration on their smartphones
- **Staff Review Queue**: All registrations require staff approval before creating patient records
- **Multi-Language Support**: Email notifications in English, French, Arabic (RTL), Hindi, and Mandarin
- **Security Features**: Rate limiting, token expiration, CSRF protection
- **Duplicate Detection**: Automatic checking for potential duplicate patients
- **HIN Validation**: Ontario health card number validation with MOD-10 checksum

---

## Table of Contents

1. [System Requirements](#system-requirements)
2. [Installation Guide](#installation-guide)
3. [Configuration](#configuration)
4. [Security Setup](#security-setup)
5. [Usage Guide](#usage-guide)
6. [API Reference](#api-reference)
7. [Troubleshooting](#troubleshooting)
8. [Customization](#customization)

---

## System Requirements

### Software Requirements

| Component | Version | Notes |
|-----------|---------|-------|
| OpenO EMR | 1.0+ | Base EMR system |
| Java | 21+ | Runtime environment |
| MariaDB/MySQL | 10.4+ | Database |
| Tomcat | 9.0+ | Application server |
| SMTP Server | - | For email notifications |

### Browser Support

- Chrome 90+
- Firefox 88+
- Safari 14+
- Edge 90+
- Mobile browsers (iOS Safari, Chrome for Android)

---

## Installation Guide

### Step 1: Database Migration

Run the database migration script to create the required tables:

```bash
# Connect to your database
mysql -u root -p oscar

# Run the migration script
source /path/to/update-2025-12-29-patient-registration-queue.sql
```

Or execute directly:

```sql
-- See: database/mysql/updates/update-2025-12-29-patient-registration-queue.sql
```

This creates the following tables:
- `registration_module_config` - Module configuration per facility
- `registration_tokens` - Session tokens for QR code scans
- `patient_registration_queue` - Pending registration records
- `registration_rate_limit` - Rate limiting for IP addresses

### Step 2: Import Spring Context

Add the registration module context to your application. In `applicationContext.xml` or your main Spring configuration:

```xml
<import resource="classpath:applicationContext-registration.xml"/>
```

### Step 3: Configure REST Endpoints

Ensure the registration REST service is included in your CXF configuration. Add to `applicationContextREST.xml`:

```xml
<jaxrs:server id="registrationRestServer" address="/registration">
    <jaxrs:serviceBeans>
        <ref bean="patientRegistrationRestService"/>
    </jaxrs:serviceBeans>
</jaxrs:server>
```

### Step 4: Add Struts2 Mappings

Add the action mappings to `struts.xml`:

```xml
<package name="registration" namespace="/registration" extends="struts-default">
    <action name="queue" class="ca.openosp.openo.registration.web.RegistrationQueue2Action" method="list">
        <result name="success">/admin/registration/queue.jsp</result>
    </action>
    <action name="review" class="ca.openosp.openo.registration.web.RegistrationQueue2Action" method="view">
        <result name="success">/admin/registration/review.jsp</result>
        <result name="error">/admin/registration/queue.jsp</result>
    </action>
    <action name="approve" class="ca.openosp.openo.registration.web.RegistrationQueue2Action" method="approve">
        <result name="success" type="redirect">/registration/queue.do?success=approved</result>
        <result name="error">/admin/registration/review.jsp</result>
    </action>
    <action name="reject" class="ca.openosp.openo.registration.web.RegistrationQueue2Action" method="reject">
        <result name="success" type="redirect">/registration/queue.do?success=rejected</result>
        <result name="error">/admin/registration/review.jsp</result>
    </action>
    <action name="qrcode" class="ca.openosp.openo.registration.web.RegistrationQueue2Action" method="qrcode">
        <result name="success">/admin/registration/qrcode.jsp</result>
    </action>
    <action name="pendingCount" class="ca.openosp.openo.registration.web.RegistrationQueue2Action" method="getPendingCount">
        <result name="success" type="json"/>
    </action>
</package>
```

### Step 5: Exclude Public URLs from Security

Add the public registration URLs to your security exclusions in `web.xml`:

```xml
<!-- Exclude public registration endpoints from authentication -->
<filter-mapping>
    <filter-name>CasAuthenticationFilter</filter-name>
    <url-pattern>/*</url-pattern>
</filter-mapping>

<!-- Add exclusion for registration -->
<context-param>
    <param-name>excludedPaths</param-name>
    <param-value>
        /registration/start,
        /registration/form.jsp,
        /registration/success.jsp,
        /registration/expired.jsp,
        /ws/rs/registration/validate-token,
        /ws/rs/registration/submit,
        /ws/rs/registration/config
    </param-value>
</context-param>
```

### Step 6: Configure CSRF Exclusions

In `Owasp.CsrfGuard.properties`, add exclusions for public endpoints:

```properties
org.owasp.csrfguard.unprotected.Registration1=/registration/start
org.owasp.csrfguard.unprotected.Registration2=/registration/form.jsp
org.owasp.csrfguard.unprotected.Registration3=/ws/rs/registration/*
```

### Step 7: Restart Application

Restart Tomcat to apply all changes:

```bash
# Using the provided scripts
server restart

# Or manually
systemctl restart tomcat
```

---

## Configuration

### Module Configuration File

Edit `src/main/resources/registration/registration-module.properties`:

```properties
# Token Settings
registration.token.expiry.minutes=30
registration.token.length=64

# Rate Limiting
registration.rate.limit.enabled=true
registration.rate.limit.max.sessions.per.ip=5
registration.rate.limit.max.submissions.per.ip=5
registration.rate.limit.window.hours=1

# Email Notifications
registration.email.enabled=true
registration.email.from=noreply@yourclinic.com
registration.email.from.name=Your Clinic

# Default Values
registration.default.province=ON
registration.default.country=CA
registration.default.language=en
```

### Per-Clinic Configuration

Configure each clinic through the database:

```sql
INSERT INTO registration_module_config
(facility_id, enabled, clinic_name, clinic_address, clinic_phone,
 email_from, email_from_name, token_expiry_minutes, rate_limit_sessions,
 rate_limit_submissions, default_province, default_country)
VALUES
(1, 1, 'Main Street Clinic', '123 Main St, Toronto, ON M5V 1A1',
 '416-555-0100', 'noreply@mainstreetclinic.com', 'Main Street Clinic',
 30, 5, 5, 'ON', 'CA');
```

### Email Configuration

Ensure SMTP is configured in your OpenO EMR settings. The module uses the system email service.

---

## Security Setup

### Security Privileges

Add the `_registration` security object to your system:

```sql
INSERT INTO secObjPrivilege (secObjName, roleUserGroup, privilege, priority)
VALUES ('_registration', 'admin', 'rw', 0);

INSERT INTO secObjPrivilege (secObjName, roleUserGroup, privilege, priority)
VALUES ('_registration', 'receptionist', 'rw', 0);

INSERT INTO secObjPrivilege (secObjName, roleUserGroup, privilege, priority)
VALUES ('_registration', 'doctor', 'r', 0);
```

### Rate Limiting

The module includes built-in rate limiting:

| Limit Type | Default | Description |
|------------|---------|-------------|
| Sessions per IP | 5/hour | QR code scans per IP address |
| Submissions per IP | 5/hour | Form submissions per IP address |

Adjust in configuration if needed for high-traffic clinics.

### Token Security

- Tokens are 64-character cryptographically secure random strings
- Tokens expire after 30 minutes (configurable)
- Each token can only be used once
- Expired/used tokens are automatically cleaned up

---

## Usage Guide

### For Clinic Staff

#### Accessing the Queue

1. Log into OpenO EMR
2. Navigate to **Admin > Patient Registration Queue**
3. View pending registrations

#### Reviewing a Registration

1. Click **Review** on any pending registration
2. Verify patient information
3. Check for duplicate warnings
4. Select a provider to assign the patient
5. Click **Approve** or **Reject**

#### Generating QR Code

1. Navigate to **Admin > Registration QR Code**
2. Print the poster or table tent version
3. Display in your waiting area

### For Patients

1. Scan the QR code with smartphone camera
2. Complete the registration form
3. Submit the form
4. Wait for staff approval
5. Receive confirmation email

---

## API Reference

### Public Endpoints (No Authentication)

#### Start Registration Session
```
GET /oscar/registration/start
```
Redirects to form with new session token.

#### Validate Token
```
POST /oscar/ws/rs/registration/validate-token
Content-Type: application/json

{
  "token": "abc123..."
}
```

Response:
```json
{
  "valid": true,
  "expires_at": "2025-01-01T00:30:00",
  "facility_name": "Main Clinic"
}
```

#### Submit Registration
```
POST /oscar/ws/rs/registration/submit
Content-Type: application/json

{
  "token": "abc123...",
  "firstName": "John",
  "lastName": "Doe",
  ...
}
```

#### Get Form Configuration
```
GET /oscar/ws/rs/registration/config
```

### Authenticated Endpoints (Staff Only)

#### Get Queue
```
GET /oscar/ws/rs/registration/queue
Authorization: [session cookie]
```

#### Approve Registration
```
PUT /oscar/ws/rs/registration/queue/{id}/approve
Content-Type: application/json

{
  "provider_no": "999998"
}
```

#### Reject Registration
```
PUT /oscar/ws/rs/registration/queue/{id}/reject
Content-Type: application/json

{
  "reason": "Duplicate patient record exists"
}
```

---

## Troubleshooting

### Common Issues

#### QR Code Not Scanning
- Ensure sufficient lighting
- Check that the QR code is not behind reflective glass
- Verify the registration URL is accessible

#### Token Expired Error
- Patient took longer than 30 minutes
- Patient should scan QR code again
- Consider increasing `token.expiry.minutes` if common

#### Email Not Sending
- Verify SMTP configuration
- Check email service logs
- Ensure `registration.email.enabled=true`

#### Rate Limit Errors
- Multiple patients sharing same IP (e.g., clinic WiFi)
- Increase rate limits for high-traffic clinics
- Consider IP whitelist for clinic network

### Logs

Check application logs for registration-related entries:

```bash
# Filter registration logs
grep "registration" /usr/local/tomcat/logs/catalina.out
```

---

## Customization

### Adding New Languages

1. Create email templates in `src/main/resources/email-templates/registration/`:
   - `approval/{lang_code}.html`
   - `rejection/{lang_code}.html`

2. Add subject lines to `subjects.properties`:
   ```properties
   approval.subject.{lang_code}=Your subject line
   rejection.subject.{lang_code}=Your subject line
   ```

3. Update `RegistrationNotificationService.mapSpokenLangToCode()`

### Custom Validation Rules

Extend `RegistrationValidator.java` to add province-specific or custom validation rules.

### Branding

Customize the public-facing JSP pages:
- `/registration/form.jsp` - Patient form
- `/registration/success.jsp` - Success page
- `/registration/expired.jsp` - Token expired page

---

## File Structure

```
src/main/java/ca/openosp/openo/registration/
├── model/
│   ├── PatientRegistrationQueue.java
│   ├── RegistrationToken.java
│   └── RegistrationModuleConfig.java
├── dao/
│   ├── PatientRegistrationQueueDao.java
│   ├── PatientRegistrationQueueDaoImpl.java
│   ├── RegistrationTokenDao.java
│   ├── RegistrationTokenDaoImpl.java
│   ├── RegistrationModuleConfigDao.java
│   └── RegistrationModuleConfigDaoImpl.java
├── service/
│   ├── PatientRegistrationRestService.java
│   ├── RegistrationTransferService.java
│   ├── RegistrationNotificationService.java
│   ├── RateLimitService.java
│   └── RegistrationCleanupJob.java
├── web/
│   └── RegistrationQueue2Action.java
└── util/
    ├── RegistrationValidator.java
    └── QRCodeGenerator.java

src/main/webapp/
├── registration/           (public pages)
│   ├── form.jsp
│   ├── success.jsp
│   └── expired.jsp
└── admin/registration/     (staff pages)
    ├── queue.jsp
    ├── review.jsp
    └── qrcode.jsp

src/main/resources/
├── applicationContext-registration.xml
├── registration/
│   └── registration-module.properties
└── email-templates/registration/
    ├── approval/
    │   ├── en.html
    │   ├── fr.html
    │   ├── ar.html
    │   ├── hi.html
    │   └── zh.html
    ├── rejection/
    │   ├── en.html
    │   ├── fr.html
    │   ├── ar.html
    │   ├── hi.html
    │   └── zh.html
    └── subjects.properties

database/mysql/updates/
└── update-2025-12-29-patient-registration-queue.sql
```

---

## Support

For technical support or feature requests, contact your OpenO EMR vendor.

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2025-12-29 | Initial release |

---

## License

Copyright (c) 2025 OpenOSP. All Rights Reserved.

This module is licensed separately from OpenO EMR core. Contact your vendor for licensing terms.
