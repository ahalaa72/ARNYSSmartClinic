# OpenO EMR Crash Course
## Training Material for Developers (Current Version)

**Version:** OpenO EMR (Development Build)
**Base URL:** http://localhost:8080/oscar/
**Last Updated:** December 2025

---

## Table of Contents

1. [System Overview](#1-system-overview)
2. [Logging In](#2-logging-in)
3. [Main Dashboard Navigation](#3-main-dashboard-navigation)
4. [Patient Management (Demographics)](#4-patient-management-demographics)
5. [Appointment Scheduling](#5-appointment-scheduling)
6. [Clinical Encounter (E-Chart)](#6-clinical-encounter-e-chart)
7. [Prescriptions (Rx)](#7-prescriptions-rx)
8. [Lab Results](#8-lab-results)
9. [Messaging & Ticklers](#9-messaging--ticklers)
10. [Billing](#10-billing)
11. [Administration](#11-administration)
12. [Key Database Tables](#12-key-database-tables)
13. [REST API Endpoints](#13-rest-api-endpoints)
14. [AI Integration Points](#14-ai-integration-points)

---

## 1. System Overview

### What is OpenO EMR?

OpenO EMR is a **Canadian Electronic Medical Records (EMR) system** designed for primary care clinics. It's a fork/evolution of OSCAR EMR, rebranded and modernized.

### Key Concepts

| Term | Description |
|------|-------------|
| **Provider** | Healthcare professional (doctor, nurse, admin) who uses the system |
| **Demographic** | Patient record (the term "demographic" is used instead of "patient" in code) |
| **Encounter** | A clinical visit/appointment with documentation |
| **Tickler** | A reminder/task system for follow-ups |
| **E-Chart** | Electronic patient chart containing all clinical notes |

### Technology Stack

| Component | Technology |
|-----------|------------|
| Backend | Java 21, Spring 5.3.39, Hibernate 5.6 |
| Frontend | JSP, JavaScript, jQuery, Bootstrap 5 |
| Database | MariaDB/MySQL |
| Web Server | Apache Tomcat 9.0.97 |
| Build | Maven 3 |

---

## 2. Logging In

### Login Page
**URL:** `http://localhost:8080/oscar/`

### Default Development Credentials
```
Username: openodoc
Password: openo2025
PIN: 2025
```

### Login Fields
- **Username:** Provider's login name (alphanumeric, max 10 chars)
- **Password:** Provider's password (BCrypt hashed in database)
- **PIN:** 4-digit numeric PIN (required for Ontario, optional for other provinces)

### After Login
You are redirected to: `provider/providercontrol.jsp` - the main schedule view.

---

## 3. Main Dashboard Navigation

After login, you see the **Provider Schedule** page with a top navigation menu.

### Top Menu Bar (Left to Right)

| Menu Item | Function | URL/Action |
|-----------|----------|------------|
| **Schedule View** | View all providers' schedules | `providercontrol.jsp?viewall=1` |
| **Caseload** | View your assigned patients | `providercontrol.jsp?caseload=1` |
| **Search** | Find patients | `demographic/search.jsp` (popup) |
| **Report** | Generate reports | `report/reportindex.jsp` (popup) |
| **Billing** | Access billing functions | `billing/CA/{PROVINCE}/billingReportCenter.jsp` |
| **Lab** | View lab results inbox | `documentManager/inboxManage.do` (popup) |
| **Msg** | Internal messaging | `messenger/DisplayMessages.do` (popup) |
| **Con** | Consultation requests | `oscarEncounter/IncomingConsultation.do` |
| **eDoc** | Electronic documents | `documentManager/documentReport.jsp` |
| **Tickler** | Reminders/tasks | `tickler/ticklerMain.jsp` (popup) |
| **Administration** | System settings | `administration/` |
| **Dashboard** | Analytics dashboards | Dropdown menu |
| **Help** | Help resources | External link |

### User Menu (Right Side)
- **Scratch Pad** - Quick notes
- **Provider Name** - Click for preferences
- **Logout** - End session

---

## 4. Patient Management (Demographics)

### Searching for Patients
**URL:** `demographic/search.jsp`

Search options:
- By name (Last, First)
- By HIN (Health Insurance Number)
- By DOB (Date of Birth)
- By Phone
- By Chart Number

### Adding a New Patient
**URL:** `demographic/demographicaddarecord.jsp`

Required fields:
- Last Name
- First Name
- Sex
- Date of Birth
- Health Card Type (Province)
- HIN (if Ontario, must be 10 digits passing MOD-10)

### Viewing/Editing Patient
**URL:** `demographic/demographiccontrol.jsp?demographic_no={ID}&displaymode=edit`

Key tabs/sections:
- **Master Record** - Basic demographics
- **Appointment History** - Past/future appointments
- **Waiting List** - Queue management

### Patient Data Model

```
Database Table: demographic
Primary Key: demographic_no (auto-increment integer)

Key Fields:
- demographic_no     INT        Patient ID
- first_name         VARCHAR
- last_name          VARCHAR
- hin                VARCHAR    Health Insurance Number
- hc_type            VARCHAR    Province code (ON, BC, AB...)
- sex                CHAR(1)    M/F
- year_of_birth      VARCHAR(4)
- patient_status     VARCHAR    AC=Active, IN=Inactive, DE=Deceased
- provider_no        VARCHAR    Assigned provider
```

---

## 5. Appointment Scheduling

### Schedule Views

| View | URL Parameter | Description |
|------|---------------|-------------|
| Day View | `displaymode=day` | Default daily schedule |
| Week View | `displaymode=week` | Weekly overview |
| Month View | `displaymode=month` | Monthly calendar |

### Schedule URL Format
```
provider/providercontrol.jsp?
  year={YYYY}&
  month={MM}&
  day={DD}&
  view=0&
  displaymode=day&
  dboperation=searchappointmentday
```

### Booking an Appointment
1. Click on empty time slot in schedule
2. Search for patient or create new
3. Select appointment type/reason
4. Confirm booking

### Appointment Data Model

```
Database Table: appointment
Primary Key: appointment_no

Key Fields:
- appointment_no     INT        Appointment ID
- demographic_no     INT        Patient ID
- provider_no        VARCHAR    Provider ID
- appointment_date   DATE
- start_time         TIME
- end_time           TIME
- status             CHAR(2)    t=Confirmed, H=Here, P=Picked
- reason             VARCHAR    Visit reason
- type               VARCHAR    Appointment type
```

---

## 6. Clinical Encounter (E-Chart)

### Opening an Encounter
From the schedule, click on a patient's appointment to open their E-Chart.

**URL:** `casemgmt/forward.jsp?demographic_no={ID}&providerNo={PROV}&appointmentNo={APPT}`

### E-Chart Sections

| Section | Description | Location |
|---------|-------------|----------|
| **CPP** | Cumulative Patient Profile | Left panel |
| **Ongoing Concerns** | Active medical issues | Left panel |
| **Medical History** | Past conditions | Left panel |
| **Social History** | Lifestyle factors | Left panel |
| **Family History** | Hereditary conditions | Left panel |
| **Reminders** | Preventive care alerts | Left panel |
| **Encounter Notes** | Clinical documentation | Main area |
| **Rx** | Prescriptions | Right panel |
| **Labs** | Lab results | Right panel |
| **Measurements** | Vitals, BMI, etc. | Right panel |

### Clinical Notes Data Model

```
Database Table: casemgmt_note
Primary Key: note_id

Key Fields:
- note_id            INT        Note ID
- demographic_no     INT        Patient ID
- provider_no        VARCHAR    Author
- note               TEXT       Clinical note content
- observation_date   DATETIME   When observed
- signed             BOOLEAN    Is signed/locked
```

---

## 7. Prescriptions (Rx)

### Accessing Rx Module
**URL:** `oscarRx/choosePatient.do?providerNo={PROV}`

Or from within E-Chart, click "Rx" button.

### Prescription Workflow
1. Search for drug (by brand or generic name)
2. Select strength/formulation
3. Set dosage, frequency, duration
4. Add special instructions
5. Print or e-prescribe

### Prescription Data Model

```
Database Table: drugs
Primary Key: drugid

Key Fields:
- drugid             INT        Prescription ID
- demographic_no     INT        Patient ID
- provider_no        VARCHAR    Prescriber
- BN                 VARCHAR    Brand Name
- GN                 VARCHAR    Generic Name
- ATC                VARCHAR    ATC Code (classification)
- dosage             VARCHAR
- frequency          VARCHAR
- rx_date            DATE       Prescription date
- end_date           DATE       End date
- archived           BOOLEAN    Is discontinued
```

---

## 8. Lab Results

### Lab Inbox
**URL:** `documentManager/inboxManage.do?method=prepareForIndexPage&providerNo={PROV}`

### Lab Types Supported
- HL7 (standard lab format)
- OLIS (Ontario Lab Information System)
- PDF documents
- Scanned images

### Lab Data Model

```
Database Table: hl7TextMessage
Primary Key: lab_id

Database Table: patientLabRouting
- Links labs to patients and providers
- Tracks acknowledgment status
```

---

## 9. Messaging & Ticklers

### Internal Messaging
**URL:** `messenger/DisplayMessages.do?providerNo={PROV}`

Features:
- Send messages between providers
- Attach to patient records
- Message forwarding

### Ticklers (Reminders)
**URL:** `tickler/ticklerMain.jsp`

Purpose: Task reminders for patient follow-ups

### Tickler Data Model

```
Database Table: tickler
Primary Key: tickler_no

Key Fields:
- tickler_no         INT        Tickler ID
- demographic_no     INT        Patient ID
- creator            VARCHAR    Creator provider
- task_assigned_to   VARCHAR    Assigned provider
- service_date       DATE       Due date
- message            VARCHAR    Reminder text
- status             CHAR(1)    A=Active, C=Complete, D=Deleted
- priority           VARCHAR    High/Normal/Low
```

---

## 10. Billing

### Billing Center
**URL:** `billing/CA/{PROVINCE}/billingReportCenter.jsp`

Province codes: ON (Ontario), BC (British Columbia), AB (Alberta), etc.

### Billing Workflow
1. Open patient encounter
2. Click "Bill" button
3. Select billing codes (fee codes)
4. Add diagnostic codes (ICD-9/ICD-10)
5. Submit claim

### Billing Data Model

```
Database Tables (vary by province):
- billing_on_cheader1   (Ontario header)
- billing_on_item       (Ontario line items)
- billingmaster         (Generic billing)
```

---

## 11. Administration

### Admin Panel
**URL:** `administration/`

### Key Admin Functions

| Section | Purpose |
|---------|---------|
| **User Management** | Add/edit providers and users |
| **Schedule Management** | Configure appointment templates |
| **System Properties** | Application settings |
| **Security** | Roles and permissions |
| **Lookup Tables** | Drug lists, billing codes |

---

## 12. Key Database Tables

### Core Tables Reference

| Table | Purpose |
|-------|---------|
| `demographic` | Patient records |
| `provider` | Healthcare providers |
| `appointment` | Scheduled appointments |
| `casemgmt_note` | Clinical encounter notes |
| `drugs` | Prescriptions |
| `tickler` | Reminders/tasks |
| `measurements` | Vitals and clinical measurements |
| `preventions` | Immunizations and preventive care |
| `allergies` | Patient allergies |
| `dxresearch` | Diagnoses |
| `security` | User login credentials |
| `secUserRole` | User role assignments |
| `secRole` | Role definitions |

---

## 13. REST API Endpoints

### Base URL
```
http://localhost:8080/oscar/ws/rs/
```

### Key Endpoints

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/demographic/{id}` | GET | Get patient info |
| `/schedule/day` | GET | Get daily schedule |
| `/notes/{demographicNo}` | GET | Get patient notes |
| `/tickler` | GET/POST | Manage ticklers |
| `/prescription/{demographicNo}` | GET | Get prescriptions |

### Authentication
- OAuth 1.0a (being migrated to ScribeJava)
- Session-based for JSP pages

---

## 14. AI Integration Points

### Recommended Integration Areas

#### 1. Clinical Decision Support
**Location:** Encounter notes / E-Chart
**Data Source:** `casemgmt_note`, `dxresearch`, `drugs`
**Use Case:** Suggest diagnoses, drug interactions, clinical guidelines

#### 2. Appointment Scheduling Optimization
**Location:** Schedule view
**Data Source:** `appointment`, `demographic`
**Use Case:** Predict no-shows, optimize scheduling

#### 3. Lab Result Interpretation
**Location:** Lab inbox
**Data Source:** `hl7TextMessage`, `measurements`
**Use Case:** Highlight abnormal values, trend analysis

#### 4. Clinical Documentation
**Location:** Encounter notes
**Data Source:** `casemgmt_note`
**Use Case:** Voice-to-text, note summarization, coding assistance

#### 5. Patient Risk Stratification
**Location:** Dashboard / Caseload
**Data Source:** Multiple tables
**Use Case:** Identify high-risk patients, preventive care gaps

### Integration Architecture Options

```
Option A: REST API Integration
[AI Service] <--REST--> [OpenO REST API] <--> [Database]

Option B: Direct Database Integration
[AI Service] <--JDBC--> [MariaDB] (read-only recommended)

Option C: Message Queue
[OpenO] --events--> [Message Queue] --> [AI Service]
```

### Security Considerations
- All patient data is PHI (Protected Health Information)
- HIPAA/PIPEDA compliance required
- Use encrypted connections
- Implement audit logging
- Never log PHI

---

## Quick Reference URLs

| Function | URL |
|----------|-----|
| Login | `/oscar/` |
| Schedule | `/oscar/provider/providercontrol.jsp` |
| Patient Search | `/oscar/demographic/search.jsp` |
| Add Patient | `/oscar/demographic/demographicaddarecord.jsp` |
| E-Chart | `/oscar/casemgmt/forward.jsp?demographic_no={ID}` |
| Rx Module | `/oscar/oscarRx/choosePatient.do` |
| Ticklers | `/oscar/tickler/ticklerMain.jsp` |
| Lab Inbox | `/oscar/documentManager/inboxManage.do` |
| Admin | `/oscar/administration/` |

---

## Development Environment

### Starting the Application
```bash
# Build and deploy
make install

# Start/stop Tomcat
server start
server stop
server restart

# View logs
server log

# Connect to database
db-connect
```

### Key Configuration Files
- `oscar.properties` - Main application config
- `applicationContext.xml` - Spring configuration
- `struts.xml` - Action mappings

---

*This crash course is based on the current OpenO EMR codebase as of December 2025.*
