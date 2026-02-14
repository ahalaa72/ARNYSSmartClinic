# Patient Self-Registration Feature Implementation Plan

**Document Version:** 1.0
**Created:** December 2025
**Status:** Approved for Implementation

---

## Overview
Implement a QR code-based patient self-registration system where patients can enter their information via a public web form, which is then reviewed by clinic staff before being added to the system.

**Key Concept:** Static QR code (like restaurant menu) displayed in clinic waiting room. Each scan creates a unique 30-minute session token.

---

## Part 1: Field Classification Analysis

### PATIENT-ENTRY FIELDS (Can be filled by patient)

| Field | DB Column | Validation | Notes |
|-------|-----------|------------|-------|
| **First Name** | `first_name` | Required, max 30 chars | Uppercase on save |
| **Last Name** | `last_name` | Required, max 30 chars | Uppercase on save |
| **Middle Names** | `middleNames` | Optional, max 100 chars | |
| **Preferred Name** | `pref_name` | Optional, max 30 chars | |
| **Title** | `title` | Optional, dropdown | DR, MR, MRS, MS, etc. |
| **Date of Birth** | `year_of_birth`, `month_of_birth`, `date_of_birth` | Required, valid date 1800-present | 3 separate fields |
| **Sex** | `sex` | Required, M/F/O | Biological sex |
| **Gender** | `gender` | Optional | Self-identified |
| **Pronouns** | `pronoun` | Optional | |
| **Address** | `address` | **Required**, max 60 chars | |
| **City** | `city` | **Required**, max 50 chars | |
| **Province** | `province` | **Required**, dropdown | Canadian provinces |
| **Postal Code** | `postal` | **Required**, Canadian format A1A 1A1 | |
| **Home Phone** | `phone` | Optional, format ###-###-#### | |
| **Cell Phone** | `demo_cell` (demographicExt) | **Required** | |
| **Work Phone** | `phone2` | Optional | |
| **Email** | `email` | **Required**, max 100 chars | |
| **Health Card Number (HIN)** | `hin` | Province-specific validation | ON: 10 digits + MOD-10 |
| **Health Card Version** | `ver` | Optional, 2 chars | |
| **Health Card Province** | `hc_type` | Required if HIN provided | ON, BC, AB, etc. |
| **Health Card Expiry** | `hc_renew_date` | Optional, valid date | |
| **Official Language** | `official_lang` | Optional | English/French |
| **Spoken Language** | `spoken_lang` | Optional | For email notifications |
| **Country of Origin** | `country_of_origin` | Optional | |
| **Emergency Contact** | (custom field) | Optional | NEW - not in current schema |
| **Consent to Email** | `consentToUseEmailForCare` | Boolean | |

### CLINIC-ENTRY FIELDS (Staff only - NOT shown to patient)

| Field | DB Column | Reason |
|-------|-----------|--------|
| **Provider/Doctor** | `provider_no` | Clinic assigns |
| **Nurse** | `cust1` | Clinic assigns |
| **Midwife** | `cust4` | Clinic assigns |
| **Resident** | `cust2` | Clinic assigns |
| **Chart Number** | `chart_no` | Clinic generates |
| **Patient Status** | `patient_status` | Default: AC (Active) |
| **Roster Status** | `roster_status` | Clinic decision |
| **Roster Enrolled To** | `roster_enrolled_to` | Clinic assigns |
| **Roster Date** | `roster_date` | Clinic sets |
| **Date Joined** | `date_joined` | Auto: registration date |
| **Referral Doctor** | `family_doctor` | Clinic enters |
| **Billing Number** | various | Clinic billing |
| **OHIP Number** | `ohip_no` | Provider-specific |
| **PHU (Public Health Unit)** | custom | Clinic config |
| **Program Assignment** | admission tables | Clinic assigns |
| **Waiting List** | waiting_list tables | Clinic manages |
| **Alerts** | `cust3` | Clinic adds |
| **Notes** | `content` | Clinic adds |

---

## Part 2: System Architecture

### High-Level Flow

```
┌─────────────┐     ┌──────────────┐     ┌─────────────────┐     ┌──────────────┐
│   Patient   │────▶│  Static QR   │────▶│  Public Web     │────▶│  Staging     │
│  at Clinic  │     │  (30min tok) │     │  Form (JSP)     │     │  Table       │
└─────────────┘     └──────────────┘     └─────────────────┘     └──────┬───────┘
                                                                        │
                    ┌──────────────┐     ┌─────────────────┐            │
                    │  Demographic │◀────│  Staff Review   │◀───────────┘
                    │  Table       │     │  Interface      │
                    └──────────────┘     └─────────────────┘
```

### Components

1. **Static QR Code** - One permanent QR displayed in clinic (like restaurant menu)
2. **Session Token Generator** - Creates 30-minute tokens on each scan
3. **Public Registration Form** - Patient-facing web form (no auth required)
4. **Staging Table** - Temporary storage for pending registrations
5. **Staff Review Interface** - Admin page to approve/reject/edit
6. **Transfer Service** - Moves approved records to demographic table
7. **Notification Service** - Emails patients in their spoken language

---

## Part 3: Database Design

### New Table: `patient_registration_queue`

```sql
CREATE TABLE patient_registration_queue (
    id INT AUTO_INCREMENT PRIMARY KEY,

    -- Registration Metadata
    registration_token VARCHAR(64) UNIQUE NOT NULL,
    status ENUM('pending', 'approved', 'rejected', 'expired') DEFAULT 'pending',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME,  -- 30 minutes from creation
    submitted_at DATETIME,
    reviewed_at DATETIME,
    reviewed_by VARCHAR(6),
    rejection_reason TEXT,

    -- Patient Data (mirrors demographic table)
    first_name VARCHAR(30),
    last_name VARCHAR(30),
    middle_names VARCHAR(100),
    pref_name VARCHAR(30),
    title VARCHAR(10),
    year_of_birth VARCHAR(4),
    month_of_birth VARCHAR(2),
    date_of_birth VARCHAR(2),
    sex CHAR(1),
    gender VARCHAR(25),
    pronoun VARCHAR(25),

    -- Contact
    address VARCHAR(60),
    city VARCHAR(50),
    province VARCHAR(20),
    postal VARCHAR(9),
    phone VARCHAR(20),
    phone2 VARCHAR(20),
    cell_phone VARCHAR(20),
    email VARCHAR(100),
    consent_email TINYINT(1) DEFAULT 0,

    -- Health Card
    hin VARCHAR(20),
    ver CHAR(3),
    hc_type VARCHAR(20),
    hc_renew_date DATE,

    -- Additional
    official_lang VARCHAR(60),
    spoken_lang VARCHAR(60),
    country_of_origin CHAR(4),
    emergency_contact_name VARCHAR(60),
    emergency_contact_phone VARCHAR(20),

    -- Validation
    hin_validation_error VARCHAR(255),  -- Inline error message
    duplicate_warning TEXT,  -- JSON: potential duplicates found

    -- Final demographic_no after approval
    demographic_no INT,

    INDEX idx_status (status),
    INDEX idx_token (registration_token),
    INDEX idx_created (created_at),
    INDEX idx_hin (hin)
);
```

### New Table: `registration_tokens`

```sql
CREATE TABLE registration_tokens (
    id INT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(64) UNIQUE NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL,  -- 30 minutes from creation
    used TINYINT(1) DEFAULT 0,
    used_at DATETIME,
    ip_address VARCHAR(45),  -- For rate limiting

    INDEX idx_token (token),
    INDEX idx_expires (expires_at)
);
```

---

## Part 4: API Design

### Public Endpoints (No Authentication)

```
GET /oscar/registration/start
  Purpose: Entry point from QR code scan
  Action: Generates session token, redirects to form
  Output: 302 Redirect to /oscar/registration/form.jsp?token={new_token}

POST /oscar/ws/rs/registration/validate-token
  Input: { token: "abc123" }
  Output: { valid: true, expires_at: "2025-01-01T00:00:00", facility_name: "Main Clinic" }

POST /oscar/ws/rs/registration/submit
  Input: { token: "abc123", patient_data: { ... } }
  Output: { success: true, queue_id: 123, message: "Registration submitted" }
  Note: Includes CAPTCHA validation

GET /oscar/ws/rs/registration/config
  Output: { provinces: [...], languages: [...], titles: [...] }
```

### Authenticated Endpoints (Staff Only)

```
GET /oscar/ws/rs/registration/queue
  Output: List of pending registrations

GET /oscar/ws/rs/registration/queue/{id}
  Output: Single registration details

PUT /oscar/ws/rs/registration/queue/{id}/approve
  Input: { provider_no: "999998", additional_fields: { ... } }
  Output: { success: true, demographic_no: 12345 }

PUT /oscar/ws/rs/registration/queue/{id}/reject
  Input: { reason: "Duplicate patient" }
  Output: { success: true }

GET /oscar/ws/rs/registration/qrcode
  Output: { url: "https://clinic.com/oscar/registration/start", qr_image_base64: "..." }
  Purpose: Get static QR code for printing
```

---

## Part 5: Configuration Decisions

| Setting | Decision | Implementation |
|---------|----------|----------------|
| **QR Code Type** | Static (Restaurant-style) | ONE permanent QR displayed in clinic, each scan = new session |
| **Session Token Lifetime** | 30 minutes | Token expires 30 min after patient scans QR |
| **Duplicate Handling** | Block and notify staff | If duplicate detected, submission blocked; staff reviews |
| **Staff Notifications** | Badge + Email | Menu badge shows pending count; email sent on new submission |
| **Patient Notifications** | Email confirmation | Patient receives email when approved/rejected (in their spoken language) |
| **Default Language** | English | Form defaults to English |
| **Default Province** | Ontario (ON) | Pre-selected for Ontario clinics |
| **Default Country** | Canada | Pre-selected |

### Mandatory Fields
- First Name, Last Name
- Date of Birth (Year, Month, Day)
- Sex
- Address, City, Province, Postal Code
- Cell Phone, Email
- Health Card Province (if HIN provided)

---

## Part 6: Java Classes (12 Classes)

### Model Classes (2)
1. `ca.openosp.openo.registration.model.PatientRegistrationQueue`
2. `ca.openosp.openo.registration.model.RegistrationToken`

### DAO Classes (4)
3. `ca.openosp.openo.registration.dao.PatientRegistrationQueueDao` (Interface)
4. `ca.openosp.openo.registration.dao.PatientRegistrationQueueDaoImpl`
5. `ca.openosp.openo.registration.dao.RegistrationTokenDao` (Interface)
6. `ca.openosp.openo.registration.dao.RegistrationTokenDaoImpl`

### Service Classes (3)
7. `ca.openosp.openo.registration.service.PatientRegistrationRestService` (JAX-RS)
8. `ca.openosp.openo.registration.service.RegistrationTransferService`
9. `ca.openosp.openo.registration.service.RegistrationNotificationService`

### Web/Action Classes (1)
10. `ca.openosp.openo.registration.web.RegistrationQueue2Action` (Struts2)

### Utility Classes (2)
11. `ca.openosp.openo.registration.util.RegistrationValidator`
12. `ca.openosp.openo.registration.util.QRCodeGenerator`

---

## Part 7: JSP Pages (6 Pages)

### Public Pages (No Auth)
1. `/registration/form.jsp` - Main patient registration form (mobile-responsive)
2. `/registration/success.jsp` - Confirmation after submission
3. `/registration/expired.jsp` - Token expired error page

### Admin Pages (Auth Required)
4. `/admin/registration/queue.jsp` - List pending registrations
5. `/admin/registration/review.jsp` - Detailed view for approval/rejection
6. `/admin/registration/qrcode.jsp` - View/print static QR code

---

## Part 8: QR Code Workflow

### Restaurant-Style Static QR

```
┌─────────────────────────────────────────────────────────────────┐
│                    CLINIC WAITING ROOM                          │
│                                                                 │
│    ┌─────────────┐                                              │
│    │  [QR CODE]  │  ← Same QR always displayed                  │
│    │             │    (poster, stand, TV screen)                │
│    │  Scan to    │                                              │
│    │  Register   │                                              │
│    └─────────────┘                                              │
│                                                                 │
│    Patient A scans → Gets Token ABC (expires in 30 min)         │
│    Patient B scans → Gets Token XYZ (expires in 30 min)         │
│    Patient C scans → Gets Token 123 (expires in 30 min)         │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Technical Flow
```
Static QR Code URL: https://clinic.com/oscar/registration/start

When scanned:
1. Generates unique 64-char session token
2. Sets 30-minute expiry
3. Redirects to: /registration/form.jsp?token={generated_token}
```

---

## Part 9: Multi-Language Email Notifications

### Supported Languages
| Language | Code | Direction |
|----------|------|-----------|
| English | en | LTR |
| French | fr | LTR |
| Arabic | ar | **RTL** |
| Hindi | hi | LTR |
| Mandarin | zh | LTR |

### Email Templates Location
```
src/main/resources/email-templates/
├── approval/
│   ├── en.html, fr.html, ar.html (RTL), hi.html, zh.html
├── rejection/
│   ├── en.html, fr.html, ar.html (RTL), hi.html, zh.html
└── subjects.properties
```

---

## Part 10: Security

### Rate Limiting
| Endpoint | Limit | Window |
|----------|-------|--------|
| `/registration/start` | 5/hour | Per IP |
| `/registration/submit` | 5/hour | Per IP |
| `/registration/config` | 100/hour | Per IP |

### Protection Measures
- **30-minute session expiry** - Token invalid after 30 min
- **CAPTCHA on submit** - Prevent bot submissions
- **Rate limiting per IP** - Max 5 sessions/hour
- **CSRF protection** - Admin endpoints use OWASP CSRFGuard
- **PHI protection** - Error messages don't reveal patient data, logs sanitized

### Cleanup Jobs
| Job | Frequency | Action |
|-----|-----------|--------|
| Token Expiry | Every 15 min | Mark tokens expired after 30 min |
| Record Purge | Daily 2 AM | Delete rejected/expired records >30 days |

---

## Part 11: Dependencies (pom.xml)

```xml
<!-- QR Code Generation -->
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>core</artifactId>
    <version>3.5.2</version>
</dependency>
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>javase</artifactId>
    <version>3.5.2</version>
</dependency>
```

---

## Part 12: Files to Modify

### Existing Files
- `src/main/resources/applicationContextREST.xml` - Add new REST service
- `src/main/webapp/WEB-INF/web.xml` - Add public URL exclusions
- `src/main/webapp/provider/mainMenu.jsp` - Add queue badge
- `src/main/resources/applicationContext.xml` - Add new DAOs/services
- `pom.xml` - Add ZXing QR library

---

## Implementation Checklist

### Phase 1: Database & Backend
- [ ] Create SQL migration for `patient_registration_queue` table
- [ ] Create SQL migration for `registration_tokens` table
- [ ] Create entity classes
- [ ] Create DAO interfaces and implementations
- [ ] Register DAOs in applicationContext.xml

### Phase 2: REST API
- [ ] Create `PatientRegistrationRestService.java`
- [ ] Implement /start endpoint (token generation)
- [ ] Implement /submit endpoint (form submission)
- [ ] Add public URL exclusions in web.xml

### Phase 3: Validation & Services
- [ ] Create `RegistrationValidator.java`
- [ ] Create `RegistrationTransferService.java`
- [ ] Create `RegistrationNotificationService.java`
- [ ] Add 10 email templates (5 languages × 2 types)

### Phase 4: Staff Interface
- [ ] Create `RegistrationQueue2Action.java`
- [ ] Create queue.jsp, review.jsp, qrcode.jsp
- [ ] Add menu badge for pending count

### Phase 5: Public Form
- [ ] Create form.jsp (mobile-responsive)
- [ ] Create success.jsp, expired.jsp
- [ ] Add client-side validation JavaScript

### Phase 6: Security
- [ ] Create rate limit filter
- [ ] Create cleanup scheduled job
- [ ] Security testing

---

## Summary Workflow

1. **Clinic displays static QR** → One permanent poster in waiting room
2. **Patient scans QR** → Gets unique 30-minute session token
3. **Form opens on phone** → Defaults: ON, Canada, English
4. **Patient fills form** → Real-time inline validation
5. **If HIN invalid** → Immediate feedback with specific error
6. **If duplicate found** → Blocked, staff notified
7. **If clean** → Saved to staging, staff notified (badge + email)
8. **Staff reviews** → Approve (→ creates demographic) or Reject
9. **Patient emailed** → Confirmation in their spoken language (EN/FR/AR/HI/ZH)
