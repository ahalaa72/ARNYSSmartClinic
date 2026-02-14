---
title: OpenO EMR - Database Entity Relationship Diagram
tags: [erd, database, openo, schema, healthcare, emr]
created: 2025-12-11
version: 2025.1
description: Comprehensive ERD showing core entities and relationships in OpenO EMR healthcare system
---

# OpenO EMR - Database Entity Relationship Diagram

## Overview

This document provides a comprehensive Entity Relationship Diagram (ERD) for the OpenO EMR (Electronic Medical Records) system. The database schema supports a complete healthcare management system including patient demographics, clinical encounters, prescriptions, billing, and administrative functions.

**Database Engine**: MariaDB/MySQL
**Schema Name**: oscar
**Key Features**: Multi-jurisdictional Canadian healthcare support (BC, ON), HIPAA/PIPEDA compliance, PHI protection

---

## Core Entity Relationship Diagram

```mermaid
erDiagram
    %% =====================================================
    %% PATIENT MANAGEMENT DOMAIN
    %% =====================================================

    demographic {
        int demographic_no PK "Auto-increment patient ID"
        varchar last_name "Patient last name"
        varchar first_name "Patient first name"
        varchar hin "Health Insurance Number"
        char sex "Gender (M/F)"
        varchar year_of_birth "Birth year"
        varchar month_of_birth "Birth month"
        varchar date_of_birth "Birth day"
        varchar address "Street address"
        varchar city "City"
        varchar province "Province"
        varchar postal "Postal code"
        varchar phone "Primary phone"
        varchar email "Email address"
        varchar provider_no FK "Primary care provider"
        varchar roster_status "Rostering status"
        date roster_date "Date rostered"
        varchar patient_status "Active/Inactive status"
        int genderId FK "Gender identity"
        int pronounId FK "Preferred pronoun"
        varchar lastUpdateUser "Audit: last user"
        datetime lastUpdateDate "Audit: last update"
    }

    allergies {
        int allergyid PK "Auto-increment allergy ID"
        int demographic_no FK "Patient reference"
        varchar DESCRIPTION "Allergy description"
        text reaction "Reaction details"
        date entry_date "Date recorded"
        date start_date "Onset date"
        char severity_of_reaction "Severity code"
        char onset_of_reaction "Onset type code"
        varchar regional_identifier "Provincial ID"
        tinyint archived "Soft delete flag"
        varchar providerNo FK "Recording provider"
        varchar atc "ATC code for drugs"
        varchar reaction_type "Type of reaction"
        tinyint nonDrug "Non-drug allergy flag"
        datetime lastUpdateDate "Audit: last update"
    }

    dxresearch {
        int dxresearch_no PK "Auto-increment diagnosis ID"
        int demographic_no FK "Patient reference"
        varchar dxresearch_code "Diagnosis code"
        varchar coding_system "ICD-9/ICD-10/SNOMED"
        date start_date "Diagnosis date"
        datetime update_date "Last updated"
        char status "A=Active, D=Deleted"
        tinyint association "Association flag"
        varchar providerNo FK "Diagnosing provider"
    }

    %% =====================================================
    %% PROVIDER & SECURITY DOMAIN
    %% =====================================================

    provider {
        varchar provider_no PK "Provider number (6 chars)"
        varchar last_name "Provider last name"
        varchar first_name "Provider first name"
        varchar provider_type "doctor/nurse/admin"
        varchar specialty "Medical specialty"
        varchar ohip_no "Provincial billing number"
        varchar billing_no "Billing identifier"
        char sex "Gender"
        date dob "Date of birth"
        varchar email "Email address"
        varchar phone "Phone number"
        char status "1=Active, 0=Inactive"
        varchar supervisor FK "Supervising provider"
        varchar lastUpdateUser "Audit: last user"
        datetime lastUpdateDate "Audit: last update"
    }

    security {
        int security_no PK "Auto-increment security ID"
        varchar user_name UK "Unique login username"
        varchar password "BCrypt hashed password"
        varchar provider_no FK "Associated provider"
        varchar pin "Encrypted PIN"
        tinyint forcePasswordReset "Password reset flag"
        datetime passwordUpdateDate "Last password change"
        tinyint usingMfa "Multi-factor auth enabled"
        varchar mfaSecret "MFA secret key"
        varchar oneIdKey "OneID integration"
        datetime lastUpdateDate "Audit: last update"
    }

    secRole {
        int role_no PK "Auto-increment role ID"
        varchar role_name UK "Unique role name"
        varchar description "Role description"
    }

    secUserRole {
        int id PK "Auto-increment assignment ID"
        varchar provider_no FK "Provider reference"
        varchar role_name FK "Role reference"
        int activeyn "Active flag"
        datetime lastUpdateDate "Audit: last update"
    }

    %% =====================================================
    %% SCHEDULING DOMAIN
    %% =====================================================

    appointment {
        int appointment_no PK "Auto-increment appointment ID"
        varchar provider_no FK "Provider reference"
        int demographic_no FK "Patient reference"
        date appointment_date "Appointment date"
        time start_time "Start time"
        time end_time "End time"
        varchar name "Appointment name"
        int program_id FK "Program reference"
        varchar reason "Appointment reason"
        int reasonCode FK "Coded reason"
        varchar location "Location/room"
        varchar type "Appointment type"     
        char status "Status code"
        datetime createdatetime "Creation timestamp"
        datetime updatedatetime "Last update timestamp"
        varchar creator "Creating user"
        varchar lastupdateuser "Last updating user"
    }

    scheduletemplate {
        varchar provider_no PK "Provider reference"
        varchar name PK "Template name"
        varchar summary "Template summary"
        text timecode "Time slot configuration"
    }

    %% =====================================================
    %% CLINICAL ENCOUNTERS DOMAIN
    %% =====================================================

    casemgmt_note {
        int note_id PK "Auto-increment note ID"
        int demographic_no FK "Patient reference"
        varchar provider_no FK "Provider reference"
        datetime observation_date "Clinical date"
        datetime update_date "Last modified"
        mediumtext note "Clinical note content"
        mediumtext history "Note history"
        tinyint signed "Digitally signed flag"
        varchar signing_provider_no FK "Signing provider"
        varchar encounter_type "Encounter type"
        varchar billing_code "Billing code"
        varchar program_no FK "Program reference"
        tinyint archived "Archive flag"
        char locked "Lock status"
        varchar password "Encryption password"
        char uuid UK "Universal unique ID"
        int appointmentNo FK "Related appointment"
    }

    casemgmt_issue {
        int id PK "Auto-increment issue ID"
        int demographic_no FK "Patient reference"
        int issue_id FK "Issue definition"
        tinyint acute "Acute problem flag"
        tinyint certain "Certainty flag"
        tinyint major "Major issue flag"
        tinyint resolved "Resolution status"
        int program_id FK "Program reference"
        varchar type "Issue type"
        datetime update_date "Last update"
    }

    issue {
        int issue_id PK "Auto-increment issue ID"
        varchar code UK "Issue code"
        varchar description UK "Issue description"
        varchar role "Issue role"
        varchar priority "Priority level"
        varchar type "Issue type"
        int sortOrderId "Display order"
        datetime update_date "Last update"
    }

    measurements {
        int id PK "Auto-increment measurement ID"
        varchar type "Measurement type (BP, WT, HT)"
        int demographicNo FK "Patient reference"
        varchar providerNo FK "Recording provider"
        varchar dataField "Measurement value"
        varchar measuringInstruction "Instructions/units"
        varchar comments "Comments"
        datetime dateObserved "Observation date/time"
        datetime dateEntered "Entry date/time"
        int appointmentNo FK "Related appointment"
    }

    encounterForm {
        varchar form_value PK "Form identifier"
        varchar form_name "Form display name"
        varchar form_table "Database table name"
        int hidden "Hidden flag"
    }

    %% =====================================================
    %% PRESCRIPTION & DRUG DOMAIN
    %% =====================================================

    drugs {
        int drugid PK "Auto-increment drug ID"
        varchar provider_no FK "Prescribing provider"
        int demographic_no FK "Patient reference"
        date rx_date "Prescription date"
        date end_date "End date"
        date written_date "Written date"
        varchar BN "Brand name"
        varchar GN "Generic name"
        varchar ATC "ATC code"
        varchar customName "Custom drug name"
        float takemin "Minimum dose"
        float takemax "Maximum dose"
        varchar freqcode "Frequency code"
        varchar duration "Duration value"
        char durunit "Duration unit"
        varchar quantity "Quantity"
        tinyint repeat "Refills"
        date last_refill_date "Last refill"
        tinyint archived "Archive status"
        tinyint prn "PRN flag"
        tinyint nosubs "No substitution"
        text special "Special instructions"
        varchar regional_identifier "Provincial ID"
        varchar route "Route of administration"
        varchar drug_form "Drug form"
        datetime create_date "Creation date"
        tinyint long_term "Long-term medication"
        tinyint past_med "Past medication"
        varchar rxStatus "Prescription status"
        datetime lastUpdateDate "Audit: last update"
    }

    drugReason {
        int id PK "Auto-increment reason ID"
        int archived FK "Drug reference"
        int demographic_no FK "Patient reference"
        varchar primaryReason "Primary indication"
        text comments "Additional comments"
    }

    %% =====================================================
    %% BILLING DOMAIN
    %% =====================================================

    billing {
        int billing_no PK "Auto-increment billing ID"
        int demographic_no FK "Patient reference"
        varchar provider_no FK "Billing provider"
        int appointment_no FK "Related appointment"
        int clinic_no "Clinic identifier"
        varchar hin "Patient HIN"
        date billing_date "Service date"
        time billing_time "Service time"
        date update_date "Last update date"
        time update_time "Last update time"
        text content "Billing content/codes"
        varchar total "Total amount"
        char status "O=Outstanding, B=Billed, P=Paid"
        varchar provider_ohip_no FK "Provider OHIP number"
        varchar apptProvider_no FK "Appointment provider"
        varchar creator FK "Creating user"
        varchar billingtype "Billing type (MSP/WCB)"
    }

    billingservice {
        varchar service_code PK "Service code"
        varchar description "Service description"
        varchar value "Billing value/fee"
        date billingservice_date "Effective date"
        varchar specialty "Provider specialty"
        char billingservice_premium "Premium flag"
        varchar gstFlag "GST applicable flag"
        varchar region "Province/region"
    }

    %% =====================================================
    %% DOCUMENT MANAGEMENT DOMAIN
    %% =====================================================

    document {
        int document_no PK "Auto-increment document ID"
        varchar doctype "Document type"
        varchar docClass "Document class"
        varchar docSubClass "Document subclass"
        varchar docdesc "Description"
        text docxml "XML metadata"
        varchar docfilename "File name"
        varchar doccreator FK "Creator provider"
        varchar responsible FK "Responsible provider"
        varchar source "Document source"
        int program_id FK "Program reference"
        datetime updatedatetime "Last update"
        char status "Status code"
        varchar contenttype "MIME type"
        datetime contentdatetime "Content date"
        int public1 "Public flag"
        date observationdate "Observation date"
        varchar reviewer FK "Reviewer provider"
        datetime reviewdatetime "Review date"
        int appointment_no FK "Related appointment"
        tinyint abnormal "Abnormal flag"
        date receivedDate "Received date"
    }

    ctl_document {
        varchar module PK "Module name"
        int module_id PK "Module entity ID"
        int document_no PK "Document reference"
        char status "Link status"
    }

    %% =====================================================
    %% PROGRAM MANAGEMENT DOMAIN
    %% =====================================================

    program {
        int id PK "Auto-increment program ID"
        int facilityId FK "Facility reference"
        varchar name "Program name"
        varchar description "Program description"
        varchar type "Program type"
        varchar location "Physical location"
        int maxAllowed "Maximum capacity"
        varchar programStatus "Status"
        varchar email "Contact email"
        varchar phone "Contact phone"
        int ageMin "Minimum age"
        int ageMax "Maximum age"
        tinyint enableEncounterTime "Encounter time tracking"
        datetime lastUpdateDate "Audit: last update"
    }

    program_provider {
        bigint id PK "Auto-increment assignment ID"
        bigint program_id FK "Program reference"
        varchar provider_no FK "Provider reference"
        bigint role_id FK "Role reference"
        bigint team_id FK "Team reference"
    }

    Facility {
        int id PK "Auto-increment facility ID"
        varchar name UK "Unique facility name"
        varchar contactName "Contact person"
        varchar contactEmail "Contact email"
        varchar contactPhone "Contact phone"
        tinyint disabled "Disabled flag"
        tinyint integratorEnabled "Integrator enabled"
        tinyint enableDigitalSignatures "Digital signatures"
        datetime lastUpdated "Last update"
    }

    site {
        int site_id PK "Auto-increment site ID"
        varchar name UK "Unique site name"
        varchar short_name UK "Short name"
        varchar phone "Phone number"
        varchar fax "Fax number"
        varchar address "Street address"
        varchar city "City"
        varchar province "Province"
        varchar postal "Postal code"
        tinyint status "Active status"
        varchar siteUrl "Site URL"
    }

    %% =====================================================
    %% SYSTEM & AUDIT DOMAIN
    %% =====================================================

    log {
        bigint id PK "Auto-increment log ID"
        datetime dateTime "Log timestamp"
        varchar provider_no FK "Provider performing action"
        varchar action "Action type"
        varchar content "Content type"
        varchar contentId "Content ID"
        varchar ip "IP address"
        int demographic_no FK "Related patient"
        text data "Additional data"
        int securityId FK "Security ID"
    }

    tickler {
        int tickler_no PK "Auto-increment tickler ID"
        int demographic_no FK "Patient reference"
        int program_id FK "Program reference"
        text message "Tickler message"
        char status "A=Active, C=Completed, D=Deleted"
        datetime update_date "Last update"
        datetime service_date "Service date"
        varchar creator FK "Creating provider"
        varchar priority "Priority (High/Normal/Low)"
        varchar task_assigned_to "Assigned provider(s)"
        int category_id FK "Category reference"
        timestamp creation_date "Creation timestamp"
    }

    eform {
        int fid PK "Auto-increment eform ID"
        varchar form_name "Form name"
        varchar file_name "File name"
        varchar subject "Form subject"
        date form_date "Form date"
        time form_time "Form time"
        varchar form_creator FK "Creator provider"
        tinyint status "Status flag"
        mediumtext form_html "HTML content"
        tinyint showLatestFormOnly "Latest only flag"
        tinyint patient_independent "Patient independent"
        varchar roleType "Role type"
        int programNo FK "Program reference"
    }

    %% =====================================================
    %% RELATIONSHIPS
    %% =====================================================

    %% Patient Management Relationships
    demographic ||--o{ allergies : "has allergies"
    demographic ||--o{ dxresearch : "has diagnoses"
    demographic ||--o{ appointment : "has appointments"
    demographic ||--o{ casemgmt_note : "has clinical notes"
    demographic ||--o{ casemgmt_issue : "has issues"
    demographic ||--o{ measurements : "has measurements"
    demographic ||--o{ drugs : "has prescriptions"
    demographic ||--o{ billing : "has billings"
    demographic ||--o{ document : "has documents"
    demographic ||--o{ tickler : "has ticklers"
    demographic ||--o{ log : "activity logged"

    %% Provider Relationships
    provider ||--o{ demographic : "primary care for"
    provider ||--o{ appointment : "schedules"
    provider ||--o{ casemgmt_note : "creates notes"
    provider ||--o{ drugs : "prescribes"
    provider ||--o{ allergies : "records"
    provider ||--o{ dxresearch : "diagnoses"
    provider ||--o{ billing : "bills services"
    provider ||--o{ measurements : "records"
    provider ||--o{ document : "creates documents"
    provider ||--o{ tickler : "creates ticklers"
    provider ||--o{ log : "actions logged"
    provider ||--o{ scheduletemplate : "has templates"
    provider ||--o{ program_provider : "assigned to programs"
    provider ||--|| security : "has login"
    provider ||--o{ secUserRole : "has roles"

    %% Security Relationships
    secRole ||--o{ secUserRole : "assigned to users"

    %% Issue Relationships
    issue ||--o{ casemgmt_issue : "defines"

    %% Appointment Relationships
    appointment ||--o{ casemgmt_note : "documented in"
    appointment ||--o{ billing : "generates"
    appointment ||--o{ measurements : "includes"
    appointment ||--o{ document : "related to"

    %% Program Relationships
    program ||--o{ appointment : "schedules"
    program ||--o{ casemgmt_note : "notes for"
    program ||--o{ casemgmt_issue : "manages issues"
    program ||--o{ document : "manages documents"
    program ||--o{ tickler : "has ticklers"
    program ||--o{ eform : "uses eforms"
    program ||--o{ program_provider : "has providers"
    Facility ||--o{ program : "hosts"

    %% Document Relationships
    document ||--o{ ctl_document : "linked to modules"

    %% Drug Relationships
    drugs ||--o{ drugReason : "has indication"
```

---

## Entity Descriptions by Domain

### Patient Management Domain

#### **demographic**
Core patient registry table containing all patient demographics, contact information, and health insurance details. Includes rostering information for primary care physicians and supports multiple address types (mailing and residential). Tracks patient status and supports gender identity fields.

**Key Features:**
- Health Insurance Number (HIN) management with version
- Rostering status and dates for primary care
- Multi-address support (mailing and residential)
- Gender identity and pronoun fields
- Audit trail (lastUpdateUser, lastUpdateDate)

#### **allergies**
Patient allergy and adverse reaction tracking with support for both drug and non-drug allergies. Includes severity, onset timing, and provincial identifiers for interoperability.

**Key Features:**
- Drug allergies linked via ATC codes
- Non-drug allergy flag
- Severity and onset classification
- Regional identifiers for provincial systems
- Soft delete via archived flag

#### **dxresearch**
Patient diagnosis research and problem list. Supports multiple coding systems (ICD-9, ICD-10, SNOMED CT) for diagnosis coding.

**Key Features:**
- Multi-coding system support
- Active/deleted status tracking
- Diagnosis start dates
- Association flags for related diagnoses

### Provider & Security Domain

#### **provider**
Healthcare provider registry including physicians, nurses, and administrative staff. Contains credentialing information and provincial billing numbers.

**Key Features:**
- Provider type classification (doctor/nurse/admin)
- Specialty tracking
- Provincial billing numbers (OHIP, RMA)
- Supervisor relationships for training programs
- Status tracking (active/inactive)

#### **security**
User authentication and security credentials. Supports password and PIN authentication, MFA, and OneID integration.

**Key Features:**
- BCrypt password hashing
- Multi-factor authentication (MFA) support
- Password expiry and force reset
- OneID single sign-on integration
- PIN for quick access

#### **secRole / secUserRole**
Role-based access control (RBAC) system. Defines security roles and assigns them to providers for granular permission management.

### Scheduling Domain

#### **appointment**
Appointment scheduling with support for multiple appointment types, statuses, and billing integration.

**Key Features:**
- Time slot management (start/end times)
- Reason codes for appointments
- Billing code association
- Status tracking (booked/confirmed/cancelled)
- Program-specific appointments
- Audit trail (creator, timestamps)

#### **scheduletemplate**
Provider schedule templates for recurring availability patterns.

### Clinical Encounters Domain

#### **casemgmt_note**
Clinical encounter notes using case management approach. Supports encrypted notes, digital signatures, and issue-based organization.

**Key Features:**
- Mediumtext for large clinical notes
- Digital signature support
- Password encryption for sensitive notes
- Issue-based note organization
- Encounter type classification
- Billing code integration
- Encounter time tracking
- History tracking for edits

#### **casemgmt_issue**
Patient issue/problem list linked to clinical notes. Supports issue classification (acute, chronic, major, resolved).

**Key Features:**
- Issue severity flags (acute, major)
- Certainty tracking
- Resolution status
- Program-specific issues

#### **issue**
Master issue definition table with standardized codes and descriptions.

#### **measurements**
Vital signs and clinical measurements (blood pressure, weight, height, etc.). Supports flowsheet integration.

**Key Features:**
- Flexible measurement type system
- Measuring instructions (units)
- Observation vs entry date tracking
- Appointment association

#### **encounterForm**
Encounter form definitions linking to specialized form tables (e.g., Rourke growth charts, BCAR prenatal forms).

### Prescription & Drug Domain

#### **drugs**
Prescription and medication management. Supports full prescription lifecycle from writing through refills.

**Key Features:**
- ATC code classification
- Brand/Generic name tracking
- Dosage and frequency
- Refill management (repeat count, last refill date)
- PRN (as needed) flag
- Long-term vs short-term classification
- Past medication tracking
- Regional identifiers for e-prescribing
- Route and form tracking
- Audit trail

#### **drugReason**
Indications and reasons for prescriptions, linking drugs to diagnoses or symptoms.

### Billing Domain

#### **billing**
Provincial healthcare billing records. Supports multiple billing types (MSP, WCB, private) and tracks submission/payment status.

**Key Features:**
- Appointment association
- Provider billing number tracking
- Service date/time
- Status tracking (Outstanding/Billed/Paid)
- Billing content (diagnostic codes, service codes)
- Update audit trail

#### **billingservice**
Fee schedule and service code definitions. Province-specific billing codes with effective dates.

**Key Features:**
- Service code and description
- Fee values
- Specialty-specific codes
- Premium flags
- Regional variations (BC, ON)

### Document Management Domain

#### **document**
Clinical document repository for PDFs, images, lab reports, and other medical documents.

**Key Features:**
- Document classification (type, class, subclass)
- Creator and responsible provider tracking
- Review workflow (reviewer, review date)
- Program restrictions
- Abnormal flag for lab results
- Public/private access control
- MIME type support
- Observation date for clinical correlation

#### **ctl_document**
Document linking table connecting documents to various modules (demographics, appointments, etc.).

### Program Management Domain

#### **program**
Clinical program definitions (e.g., diabetes program, prenatal care, mental health). Supports capacity management and age restrictions.

**Key Features:**
- Facility association
- Capacity limits
- Age restrictions (min/max)
- Encounter time tracking
- Contact information
- Program status

#### **program_provider**
Provider assignments to programs with roles and team associations.

#### **Facility**
Healthcare facility registry for multi-site EMR deployments.

**Key Features:**
- Integrator support for inter-facility communication
- Digital signature enablement
- Contact information

#### **site**
Physical site/location management for clinics with multiple locations.

### System & Audit Domain

#### **log**
Comprehensive audit log for all system actions. Tracks user activity for compliance and security.

**Key Features:**
- Action type tracking
- Content and content ID
- IP address logging
- Demographic association for PHI access
- Security ID tracking

#### **tickler**
Task and reminder system for follow-ups, recalls, and provider tasks.

**Key Features:**
- Patient association (optional)
- Program association
- Priority levels (High/Normal/Low)
- Status tracking (Active/Completed/Deleted)
- Assignment to multiple providers
- Category classification
- Service date for scheduling

#### **eform**
Electronic form system for custom clinical forms and questionnaires.

**Key Features:**
- HTML form content storage
- Role-based access
- Program-specific forms
- Patient-independent forms support
- Show latest only option

---

## Key Relationships and Cardinality

### One-to-Many Relationships

1. **demographic → allergies, dxresearch, appointments, notes, drugs, billing**
   - Each patient has multiple clinical records

2. **provider → appointments, notes, prescriptions, billings**
   - Each provider creates multiple clinical activities

3. **appointment → casemgmt_note, billing, measurements**
   - Each appointment can generate multiple clinical outputs

4. **issue → casemgmt_issue**
   - Master issue definitions used in multiple patient problem lists

5. **program → appointments, notes, documents**
   - Programs manage multiple clinical activities

### Many-to-Many Relationships

1. **provider ↔ program** (via program_provider)
   - Providers work in multiple programs, programs have multiple providers

2. **provider ↔ secRole** (via secUserRole)
   - Providers have multiple roles, roles assigned to multiple providers

3. **document ↔ modules** (via ctl_document)
   - Documents linked to multiple module entities

### One-to-One Relationships

1. **provider ↔ security**
   - Each provider has one security record for authentication

---

## Database Design Patterns

### Audit Trail Pattern
Most tables include:
- `lastUpdateUser` - varchar(6) - Provider who last modified
- `lastUpdateDate` - datetime - Timestamp of last modification

Examples: demographic, provider, drugs, allergies, program

### Soft Delete Pattern
Tables use archived or status flags instead of hard deletes:
- `archived` - tinyint(1) - Flag for soft deletion
- `status` - char(1) - Multi-state status (A=Active, D=Deleted, C=Completed)

Examples: allergies, drugs, casemgmt_note, tickler, dxresearch

### Multi-Jurisdictional Support
Tables include regional/provincial identifiers:
- `regional_identifier` - varchar(100) - Provincial system identifiers
- `hin` + `ver` - Health Insurance Number with version code
- Province-specific billing tables (billing_on_* for Ontario)

### Security & Privacy
- Password encryption (BCrypt in security table)
- Note encryption (password field in casemgmt_note)
- Public/private flags (public1 in document)
- Comprehensive audit logging (log table)

### Clinical Workflow Support
- Encounter-based note organization (casemgmt_note)
- Issue-based problem lists (casemgmt_issue)
- Appointment-driven workflows (appointment → note → billing)
- Digital signatures (signed, signing_provider_no)

---

## Technical Notes

### Primary Keys
- Most tables use auto-increment integer primary keys (demographic_no, provider_no, appointment_no)
- Composite primary keys used for linking tables (ctl_document, scheduletemplate)
- provider_no uses varchar(6) for legacy compatibility

### Foreign Key Conventions
- `demographic_no` - int(10) - Patient references
- `provider_no` - varchar(6) - Provider references
- `program_id` - int(11) - Program references
- `appointment_no` - int(12) - Appointment references

### Indexing Strategy
- Primary keys on all tables
- Foreign key indexes for join performance
- Date-based indexes on frequently queried date fields (appointment_date, billing_date, observation_date)
- Status indexes for filtering (status, archived)
- Unique indexes on business keys (security.user_name, site.name, Facility.name)

### Data Types
- **int(10)** - Standard ID fields with display width
- **varchar** - Variable length strings with appropriate limits
- **text/mediumtext** - Large content (clinical notes, comments)
- **datetime** - Timestamp fields with date and time
- **date** - Date-only fields
- **time** - Time-only fields
- **tinyint(1)** - Boolean flags (0/1)
- **char** - Fixed-length codes (status codes, gender)

---

## Common Queries and Use Cases

### Patient Chart View
```sql
SELECT d.*, a.*, cn.*, dr.*
FROM demographic d
LEFT JOIN allergies a ON d.demographic_no = a.demographic_no
LEFT JOIN casemgmt_note cn ON d.demographic_no = cn.demographic_no
LEFT JOIN drugs dr ON d.demographic_no = dr.demographic_no
WHERE d.demographic_no = ?
```

### Daily Appointment Schedule
```sql
SELECT a.*, d.first_name, d.last_name, p.first_name AS provider_first, p.last_name AS provider_last
FROM appointment a
JOIN demographic d ON a.demographic_no = d.demographic_no
JOIN provider p ON a.provider_no = p.provider_no
WHERE a.appointment_date = CURDATE()
  AND a.provider_no = ?
ORDER BY a.start_time
```

### Active Medications
```sql
SELECT *
FROM drugs
WHERE demographic_no = ?
  AND archived = 0
  AND (end_date >= CURDATE() OR end_date IS NULL OR end_date = '0001-01-01')
ORDER BY rx_date DESC
```

### Billing Claims by Status
```sql
SELECT b.*, d.first_name, d.last_name, p.first_name AS provider_first, p.last_name AS provider_last
FROM billing b
JOIN demographic d ON b.demographic_no = d.demographic_no
JOIN provider p ON b.provider_no = p.provider_no
WHERE b.status = 'O'
  AND b.billing_date BETWEEN ? AND ?
ORDER BY b.billing_date DESC
```

---

## Schema Evolution and Migration

### Migration Pattern
- Date-based migration scripts: `update-YYYY-MM-DD-description.sql`
- Initial schema: `oscarinit_2025.sql`
- Province-specific schemas: `oscarinit_bc.sql`, `oscarinit_on.sql`
- Reference data: `oscardata.sql`

### Recent Schema Changes (2025)
- Removed MyDrugRef integration tables
- Removed BORN (Better Outcomes Registry & Network) tables
- Removed HealthSafety module
- Added gender identity fields (genderId, pronounId, gender)
- Enhanced MFA support in security table
- Added encounter time tracking fields

### Healthcare Standard Support
- ICD-9/ICD-10 diagnosis codes (icd9.sql, icd10.sql)
- SNOMED CT clinical terminology (SnomedCore/)
- ATC drug classification codes
- HL7 lab integration (OLIS for Ontario)

---

## Compliance and Security

### HIPAA/PIPEDA Requirements Met
1. **Access Control** - Role-based security (secRole, secUserRole)
2. **Audit Logging** - Comprehensive log table tracking all PHI access
3. **Encryption** - Password encryption in casemgmt_note
4. **Data Integrity** - Foreign key relationships and constraints
5. **Accountability** - Audit trail fields (lastUpdateUser, lastUpdateDate)

### PHI Protection
- Patient data isolated in demographic table
- Access logged in log table
- Encryption support for sensitive notes
- Role-based access control
- Multi-factor authentication support

---

## References

- **Database Location**: MariaDB/MySQL on port 3306
- **Schema Name**: oscar
- **Hibernate Configuration**: `OscarDatabaseBase.xml`
- **Migration Scripts**: `/database/mysql/updates/`
- **Initial Schema**: `/database/mysql/oscarinit_2025.sql`

---

**Document Version**: 2025.1
**Last Updated**: 2025-12-11
**Maintained By**: OpenO EMR Development Team
