---
title: OpenO EMR - Database Access Guide
tags: [database, mysql, openo, guide, mariadb, healthcare]
created: 2025-12-11
updated: 2025-12-11
---

# OpenO EMR - Database Access Guide

## Overview

OpenO EMR uses MariaDB (MySQL-compatible) as its primary database system. The database contains 555 tables covering all aspects of healthcare operations including patient demographics, clinical records, appointments, billing, prescriptions, and more.

**Database Statistics:**
- **Total Tables:** 555
- **Database Name:** oscar
- **Default Port:** 3306
- **Character Set:** UTF-8 (utf8mb4)
- **Development Credentials:** root/password (development only - NEVER use in production)

---

## 1. Database Connection Methods

### Method 1: MySQL Command Line (Inside Container) - RECOMMENDED

This is the fastest and most direct method when working inside the DevContainer.

**Quick Connect:**
```bash
# Using the pre-configured alias
db-connect

# Or the full command
mysql -h db -u root -ppassword oscar
```

**Execute Single Query:**
```bash
# Using the alias with -e flag
mysql -h db -u root -ppassword oscar -e "SELECT COUNT(*) FROM demographic;"

# Or multi-line for readability
mysql -h db -u root -ppassword oscar -e "
  SELECT
    demographic_no,
    first_name,
    last_name,
    hin
  FROM demographic
  LIMIT 5;
"
```

**Connection Parameters:**
- **Host:** `db` (container name on the docker network)
- **Port:** 3306 (default, implicit)
- **Username:** `root`
- **Password:** `password`
- **Database:** `oscar`

---

### Method 2: DBeaver (External Tool) - BEST FOR BROWSING

DBeaver is a free, powerful database management tool with excellent MySQL/MariaDB support.

**Download:** https://dbeaver.io/download/

**Connection Setup:**
1. Click **New Database Connection**
2. Select **MySQL** or **MariaDB**
3. Enter connection details:
   ```
   Host:     localhost
   Port:     3306
   Database: oscar
   Username: root
   Password: password
   ```
4. Click **Test Connection**
5. Click **Finish**

**Features:**
- Visual table browser with search
- ER diagram generation
- SQL editor with autocomplete
- Export to CSV, JSON, XML
- Data comparison and migration tools

**Connection String:**
```
jdbc:mysql://localhost:3306/oscar?zeroDateTimeBehavior=round&useOldAliasMetadataBehavior=true&jdbcCompliantTruncation=false
```

---

### Method 3: MySQL Workbench (External Tool)

Official MySQL GUI tool from Oracle.

**Download:** https://dev.mysql.com/downloads/workbench/

**Connection Setup:**
1. Click **+** next to **MySQL Connections**
2. Enter connection details:
   ```
   Connection Name: OpenO EMR Dev
   Hostname:        localhost
   Port:            3306
   Username:        root
   ```
3. Click **Store in Vault** for password: `password`
4. Click **Test Connection**
5. Click **OK**

**Features:**
- Visual database design
- Performance monitoring
- Server administration
- SQL development
- Data modeling

---

### Method 4: Connection from Host Machine (External Apps)

When connecting from your host machine (Windows) to the containerized database:

**Connection Parameters:**
```
Host:     localhost  (or 127.0.0.1)
Port:     3306
Database: oscar
Username: root
Password: password
```

**Docker Port Mapping:**
The DevContainer docker-compose.yml maps port 3306:
```yaml
ports:
  - "3306:3306"
```

**Firewall Note:** If you cannot connect from host machine:
- Check Windows Firewall settings
- Ensure Docker Desktop is running
- Verify port 3306 is not blocked

---

## 2. Database Navigation Commands

### Show All Tables

```sql
-- List all tables in the database
SHOW TABLES;

-- List tables with pattern matching
SHOW TABLES LIKE 'demographic%';

-- Count total tables
SELECT COUNT(*) as total_tables
FROM information_schema.tables
WHERE table_schema = 'oscar';
```

**Output:**
```
total_tables
555
```

---

### Describe Table Structure

```sql
-- Show all columns and their types
DESCRIBE demographic;

-- Alternative syntax
SHOW COLUMNS FROM demographic;

-- Get detailed column information
SELECT
  COLUMN_NAME,
  DATA_TYPE,
  CHARACTER_MAXIMUM_LENGTH,
  IS_NULLABLE,
  COLUMN_KEY,
  COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'oscar'
  AND TABLE_NAME = 'demographic';
```

---

### View Table Relationships

```sql
-- Show foreign keys for a table
SELECT
  CONSTRAINT_NAME,
  TABLE_NAME,
  COLUMN_NAME,
  REFERENCED_TABLE_NAME,
  REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = 'oscar'
  AND REFERENCED_TABLE_NAME = 'demographic';

-- Show all indexes on a table
SHOW INDEX FROM demographic;
```

---

### Table Statistics

```sql
-- Get row counts for all tables
SELECT
  table_name,
  table_rows,
  ROUND((data_length + index_length) / 1024 / 1024, 2) AS size_mb
FROM information_schema.tables
WHERE table_schema = 'oscar'
ORDER BY table_rows DESC
LIMIT 20;

-- Get table size only
SELECT
  ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) AS total_size_mb
FROM information_schema.tables
WHERE table_schema = 'oscar';
```

---

## 3. Core Healthcare Tables Overview

### Table: `demographic` (Patient Data)

**Purpose:** Stores patient demographic information, health insurance numbers, contact details, and rostering status.

**Key Columns:**
```sql
demographic_no          INT(10)       Primary Key, Auto Increment
first_name             VARCHAR(30)   Patient's first name
last_name              VARCHAR(30)   Patient's last name
date_of_birth          VARCHAR(2)    Day component of DOB
month_of_birth         VARCHAR(2)    Month component of DOB
year_of_birth          VARCHAR(4)    Year component of DOB
hin                    VARCHAR(20)   Health Insurance Number
ver                    CHAR(3)       HIN version code
address                VARCHAR(60)   Street address
city                   VARCHAR(50)   City
province               VARCHAR(20)   Province code (e.g., ON, BC)
postal                 VARCHAR(9)    Postal code
phone                  VARCHAR(20)   Primary phone number
email                  VARCHAR(100)  Email address
roster_status          VARCHAR(20)   Rostering status (RO, NR, etc.)
patient_status         VARCHAR(20)   Active, Inactive, etc.
provider_no            VARCHAR(11)   Most Responsible Physician (MRP)
chart_no               VARCHAR(10)   Chart number
```

**Row Count:** Typically 1000s to 100,000s depending on clinic size

---

### Table: `appointment` (Scheduling)

**Purpose:** Stores all appointment data including scheduled, completed, canceled, and no-show appointments.

**Key Columns:**
```sql
appointment_no         INT(12)       Primary Key, Auto Increment
provider_no            VARCHAR(6)    Provider seeing the patient
demographic_no         INT(10)       Patient ID (FK to demographic)
appointment_date       DATE          Date of appointment
start_time             TIME          Start time
end_time               TIME          End time
name                   VARCHAR(50)   Patient name (cached)
reason                 VARCHAR(80)   Appointment reason
reasonCode             INT(11)       Coded reason
status                 CHAR(2)       Status: 't'=To Do, 'H'=Here, etc.
type                   VARCHAR(50)   Appointment type
billing                VARCHAR(10)   Billing code
location               VARCHAR(30)   Location/room
notes                  VARCHAR(255)  Appointment notes
```

**Status Codes:**
- `t` = To Do (scheduled)
- `H` = Here (checked in)
- `P` = Billed (completed)
- `C` = Canceled
- `N` = No Show

---

### Table: `provider` (Doctors/Staff)

**Purpose:** Stores healthcare provider information including doctors, nurses, administrators.

**Key Columns:**
```sql
provider_no            VARCHAR(6)    Primary Key, Provider number
first_name             VARCHAR(30)   First name
last_name              VARCHAR(30)   Last name
provider_type          VARCHAR(15)   doctor, nurse, admin, etc.
specialty              VARCHAR(40)   Medical specialty
ohip_no                VARCHAR(20)   OHIP billing number (Ontario)
billing_no             VARCHAR(20)   Billing number
email                  VARCHAR(60)   Email address
phone                  VARCHAR(20)   Phone number
status                 CHAR(1)       1=Active, 0=Inactive
practitionerNo         VARCHAR(20)   License/registration number
title                  VARCHAR(20)   Dr., RN, etc.
sex                    CHAR(1)       M, F, O
lastUpdateUser         VARCHAR(6)    Last user to update record
lastUpdateDate         DATETIME      Last update timestamp
```

---

### Table: `casemgmt_note` (Clinical Notes)

**Purpose:** Stores clinical encounter notes, SOAP notes, and case management documentation.

**Key Columns:**
```sql
note_id                BIGINT(20)    Primary Key, Auto Increment
demographic_no         INT(10)       Patient ID
provider_no            VARCHAR(6)    Provider who wrote note
update_date            DATETIME      Note creation/update date
observation_date       DATE          Date of observation
note                   TEXT          Clinical note content
signed                 TINYINT(1)    Is note signed? (1=Yes, 0=No)
locked                 TINYINT(1)    Is note locked? (1=Yes, 0=No)
archived               TINYINT(1)    Is note archived?
encounter_type         VARCHAR(10)   face to face, telephone, etc.
billing_code           VARCHAR(20)   Associated billing code
program_no             INT(11)       Program ID
```

**Important:** Notes may contain encrypted PHI (Protected Health Information).

---

### Table: `drugs` (Prescriptions)

**Purpose:** Stores prescription and medication data including active, discontinued, and historical prescriptions.

**Key Columns:**
```sql
drugid                 INT(10)       Primary Key, Auto Increment
demographic_no         INT(10)       Patient ID
provider_no            VARCHAR(6)    Prescribing provider
rx_date                DATE          Prescription date
end_date               DATE          End date
BN                     VARCHAR(255)  Brand Name
customName             VARCHAR(60)   Custom drug name
GCN_SEQNO              DECIMAL(10,0) Generic Code Number
ATC                    VARCHAR(8)    Anatomical Therapeutic Chemical code
quantity               VARCHAR(20)   Quantity prescribed
repeat                 TINYINT(4)    Number of repeats
duration               VARCHAR(4)    Duration
durunit                CHAR(1)       Duration unit (D=Days, W=Weeks)
freqcode               VARCHAR(6)    Frequency code
dosage                 VARCHAR(255)  Dosage instructions
archived               CHAR(1)       0=Active, 1=Archived
long_term              CHAR(1)       Is long-term medication?
```

---

### Table: `allergies`

**Purpose:** Stores patient allergy and adverse reaction information.

**Key Columns:**
```sql
allergyid              INT(11)       Primary Key, Auto Increment
demographic_no         INT(10)       Patient ID
entry_date             DATE          Date allergy was recorded
description            VARCHAR(255)  Allergy description
severity               TINYINT(1)    Severity: 1=Mild, 2=Moderate, 3=Severe, 4=Life-threatening
reaction               VARCHAR(255)  Reaction description
type_code              TINYINT(1)    0=Drug, 1=Non-drug
life_stage             CHAR(1)       A=Adult, C=Child, etc.
regional_identifier    VARCHAR(255)  Regional allergy code
archived               TINYINT(1)    0=Active, 1=Archived
```

---

### Table: `billing` (Billing Records)

**Purpose:** Stores billing transactions for fee-for-service and alternative payment plans.

**Key Columns:**
```sql
billing_no             INT(11)       Primary Key, Auto Increment
demographic_no         INT(10)       Patient ID
provider_no            VARCHAR(6)    Billing provider
appointment_no         INT(12)       Related appointment (if applicable)
billing_date           DATE          Date of service
billing_time           TIME          Time of service
service_code           VARCHAR(10)   Service/billing code
service_location       VARCHAR(3)    Service location code
dx_code1               VARCHAR(10)   Primary diagnosis code
dx_code2               VARCHAR(10)   Secondary diagnosis code
dx_code3               VARCHAR(10)   Tertiary diagnosis code
billable               CHAR(1)       B=Billable, O=Not Billable
clinic_ref_code        VARCHAR(20)   Clinic reference
status                 CHAR(1)       O=Outstanding, B=Billed, S=Settled
total                  DECIMAL(10,2) Total amount
```

---

### Table: `prevention` (Immunizations)

**Purpose:** Stores immunization records and preventive care tracking.

**Key Columns:**
```sql
id                     INT(11)       Primary Key, Auto Increment
demographic_no         INT(10)       Patient ID
provider_no            VARCHAR(6)    Administering provider
creation_date          DATETIME      Record creation date
prevention_date        DATE          Date vaccine/prevention given
prevention_type        VARCHAR(60)   Type (e.g., Flu, COVID-19, Mammogram)
deleted                TINYINT(1)    0=Active, 1=Deleted
refused                TINYINT(1)    0=Given, 1=Refused by patient
next_date              DATE          Next due date
lot_no                 VARCHAR(50)   Vaccine lot number
manufacture            VARCHAR(100)  Manufacturer
location               VARCHAR(50)   Administration site
route                  VARCHAR(50)   Route of administration
dose                   VARCHAR(50)   Dose amount
```

**Common Prevention Types:**
- Flu (Influenza vaccine)
- COVID19 (COVID-19 vaccine)
- Mammo (Mammogram)
- PAP (Pap smear)
- Pneumovax (Pneumococcal vaccine)
- Tetanus
- Shingles

---

## 4. Sample Queries by Module

### Demographic Queries

**Find all patients:**
```sql
SELECT
  demographic_no,
  CONCAT(last_name, ', ', first_name) AS patient_name,
  DATE_FORMAT(CONCAT(year_of_birth, '-', month_of_birth, '-', date_of_birth), '%Y-%m-%d') AS date_of_birth,
  hin,
  patient_status
FROM demographic
ORDER BY last_name, first_name
LIMIT 20;
```

**Search patients by name:**
```sql
SELECT
  demographic_no,
  first_name,
  last_name,
  phone,
  email,
  chart_no
FROM demographic
WHERE last_name LIKE 'Smith%'
  AND patient_status = 'AC'
ORDER BY last_name, first_name;
```

**Get patient count by status:**
```sql
SELECT
  patient_status,
  COUNT(*) as patient_count
FROM demographic
GROUP BY patient_status
ORDER BY patient_count DESC;
```

**Find patients rostered to a specific provider:**
```sql
SELECT
  d.demographic_no,
  d.first_name,
  d.last_name,
  d.roster_status,
  d.roster_date,
  p.first_name AS provider_first,
  p.last_name AS provider_last
FROM demographic d
LEFT JOIN provider p ON d.provider_no = p.provider_no
WHERE d.provider_no = '000001'
  AND d.roster_status = 'RO'
ORDER BY d.last_name;
```

---

### Appointment Queries

**View appointments for today:**
```sql
SELECT
  a.appointment_no,
  a.start_time,
  a.end_time,
  CONCAT(d.last_name, ', ', d.first_name) AS patient_name,
  a.reason,
  a.status,
  CONCAT(p.last_name, ', ', p.first_name) AS provider_name
FROM appointment a
LEFT JOIN demographic d ON a.demographic_no = d.demographic_no
LEFT JOIN provider p ON a.provider_no = p.provider_no
WHERE a.appointment_date = CURDATE()
ORDER BY a.start_time;
```

**Upcoming appointments for a patient:**
```sql
SELECT
  a.appointment_date,
  a.start_time,
  a.reason,
  a.status,
  CONCAT(p.first_name, ' ', p.last_name) AS provider_name
FROM appointment a
LEFT JOIN provider p ON a.provider_no = p.provider_no
WHERE a.demographic_no = 1
  AND a.appointment_date >= CURDATE()
ORDER BY a.appointment_date, a.start_time;
```

**Provider schedule for a specific date:**
```sql
SELECT
  start_time,
  end_time,
  CONCAT(d.last_name, ', ', d.first_name) AS patient_name,
  reason,
  status,
  type
FROM appointment a
LEFT JOIN demographic d ON a.demographic_no = d.demographic_no
WHERE provider_no = '000001'
  AND appointment_date = '2025-12-11'
ORDER BY start_time;
```

**Appointment statistics by status:**
```sql
SELECT
  status,
  CASE
    WHEN status = 't' THEN 'Scheduled'
    WHEN status = 'H' THEN 'Checked In'
    WHEN status = 'P' THEN 'Completed'
    WHEN status = 'C' THEN 'Canceled'
    WHEN status = 'N' THEN 'No Show'
    ELSE status
  END AS status_description,
  COUNT(*) as appointment_count
FROM appointment
WHERE appointment_date >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
GROUP BY status
ORDER BY appointment_count DESC;
```

---

### Clinical Notes Queries

**Check clinical notes for a patient:**
```sql
SELECT
  n.note_id,
  n.observation_date,
  n.update_date,
  CONCAT(p.first_name, ' ', p.last_name) AS provider_name,
  n.encounter_type,
  SUBSTRING(n.note, 1, 100) AS note_preview,
  n.signed,
  n.locked
FROM casemgmt_note n
LEFT JOIN provider p ON n.provider_no = p.provider_no
WHERE n.demographic_no = 1
  AND n.archived = 0
ORDER BY n.observation_date DESC
LIMIT 10;
```

**Find unsigned notes:**
```sql
SELECT
  n.note_id,
  CONCAT(d.last_name, ', ', d.first_name) AS patient_name,
  CONCAT(p.last_name, ', ', p.first_name) AS provider_name,
  n.observation_date,
  n.update_date
FROM casemgmt_note n
LEFT JOIN demographic d ON n.demographic_no = d.demographic_no
LEFT JOIN provider p ON n.provider_no = p.provider_no
WHERE n.signed = 0
  AND n.archived = 0
ORDER BY n.update_date DESC;
```

**Notes by encounter type:**
```sql
SELECT
  encounter_type,
  COUNT(*) as note_count
FROM casemgmt_note
WHERE observation_date >= DATE_SUB(CURDATE(), INTERVAL 90 DAY)
  AND archived = 0
GROUP BY encounter_type
ORDER BY note_count DESC;
```

---

### Prescription Queries

**List active prescriptions for a patient:**
```sql
SELECT
  d.drugid,
  d.rx_date,
  d.BN AS brand_name,
  d.customName,
  d.quantity,
  d.dosage,
  d.freqcode,
  d.duration,
  d.repeat,
  d.end_date,
  CONCAT(p.first_name, ' ', p.last_name) AS prescriber
FROM drugs d
LEFT JOIN provider p ON d.provider_no = p.provider_no
WHERE d.demographic_no = 1
  AND d.archived = '0'
ORDER BY d.rx_date DESC;
```

**Recently prescribed medications:**
```sql
SELECT
  CONCAT(dem.last_name, ', ', dem.first_name) AS patient_name,
  d.BN AS medication,
  d.rx_date,
  d.quantity,
  CONCAT(p.last_name, ', ', p.first_name) AS prescriber
FROM drugs d
LEFT JOIN demographic dem ON d.demographic_no = dem.demographic_no
LEFT JOIN provider p ON d.provider_no = p.provider_no
WHERE d.rx_date >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
ORDER BY d.rx_date DESC
LIMIT 50;
```

**Most commonly prescribed medications:**
```sql
SELECT
  COALESCE(BN, customName) AS medication_name,
  COUNT(*) as prescription_count
FROM drugs
WHERE rx_date >= DATE_SUB(CURDATE(), INTERVAL 365 DAY)
  AND archived = '0'
GROUP BY COALESCE(BN, customName)
ORDER BY prescription_count DESC
LIMIT 20;
```

---

### Allergy Queries

**Patient allergies:**
```sql
SELECT
  a.allergyid,
  a.description,
  a.severity,
  CASE a.severity
    WHEN 1 THEN 'Mild'
    WHEN 2 THEN 'Moderate'
    WHEN 3 THEN 'Severe'
    WHEN 4 THEN 'Life-threatening'
    ELSE 'Unknown'
  END AS severity_description,
  a.reaction,
  CASE a.type_code
    WHEN 0 THEN 'Drug Allergy'
    WHEN 1 THEN 'Non-drug Allergy'
    ELSE 'Unknown'
  END AS allergy_type,
  a.entry_date
FROM allergies a
WHERE a.demographic_no = 1
  AND a.archived = 0
ORDER BY a.severity DESC, a.entry_date DESC;
```

**Patients with severe allergies:**
```sql
SELECT
  CONCAT(d.last_name, ', ', d.first_name) AS patient_name,
  a.description,
  a.reaction,
  a.entry_date
FROM allergies a
JOIN demographic d ON a.demographic_no = d.demographic_no
WHERE a.severity >= 3
  AND a.archived = 0
ORDER BY a.severity DESC, d.last_name;
```

---

### Billing Queries

**View billing records for a patient:**
```sql
SELECT
  b.billing_no,
  b.billing_date,
  b.service_code,
  b.dx_code1,
  CONCAT(p.first_name, ' ', p.last_name) AS provider_name,
  b.total,
  b.status
FROM billing b
LEFT JOIN provider p ON b.provider_no = p.provider_no
WHERE b.demographic_no = 1
ORDER BY b.billing_date DESC
LIMIT 20;
```

**Billing summary by provider:**
```sql
SELECT
  p.provider_no,
  CONCAT(p.first_name, ' ', p.last_name) AS provider_name,
  COUNT(*) as billing_count,
  SUM(b.total) as total_billed
FROM billing b
JOIN provider p ON b.provider_no = p.provider_no
WHERE b.billing_date >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
GROUP BY p.provider_no, provider_name
ORDER BY total_billed DESC;
```

**Outstanding bills:**
```sql
SELECT
  b.billing_no,
  CONCAT(d.last_name, ', ', d.first_name) AS patient_name,
  b.billing_date,
  b.service_code,
  b.total,
  DATEDIFF(CURDATE(), b.billing_date) as days_outstanding
FROM billing b
JOIN demographic d ON b.demographic_no = d.demographic_no
WHERE b.status = 'O'
ORDER BY b.billing_date;
```

---

### Prevention/Immunization Queries

**Patient immunization history:**
```sql
SELECT
  p.prevention_type,
  p.prevention_date,
  p.lot_no,
  p.manufacture,
  p.dose,
  p.next_date,
  CONCAT(prov.first_name, ' ', prov.last_name) AS administered_by
FROM prevention p
LEFT JOIN provider prov ON p.provider_no = prov.provider_no
WHERE p.demographic_no = 1
  AND p.deleted = 0
ORDER BY p.prevention_date DESC;
```

**Flu vaccinations this season:**
```sql
SELECT
  CONCAT(d.last_name, ', ', d.first_name) AS patient_name,
  p.prevention_date,
  p.lot_no,
  CONCAT(prov.last_name, ', ', prov.first_name) AS provider
FROM prevention p
JOIN demographic d ON p.demographic_no = d.demographic_no
LEFT JOIN provider prov ON p.provider_no = prov.provider_no
WHERE p.prevention_type LIKE '%Flu%'
  AND p.prevention_date >= '2024-09-01'
  AND p.deleted = 0
ORDER BY p.prevention_date DESC;
```

**Due vaccinations:**
```sql
SELECT
  CONCAT(d.last_name, ', ', d.first_name) AS patient_name,
  p.prevention_type,
  p.next_date,
  DATEDIFF(p.next_date, CURDATE()) as days_until_due
FROM prevention p
JOIN demographic d ON p.demographic_no = d.demographic_no
WHERE p.next_date IS NOT NULL
  AND p.next_date <= DATE_ADD(CURDATE(), INTERVAL 30 DAY)
  AND p.deleted = 0
ORDER BY p.next_date;
```

---

### Provider Queries

**List all active providers:**
```sql
SELECT
  provider_no,
  CONCAT(title, ' ', first_name, ' ', last_name) AS full_name,
  provider_type,
  specialty,
  email,
  work_phone
FROM provider
WHERE status = '1'
ORDER BY last_name, first_name;
```

**Provider workload (appointments in next 7 days):**
```sql
SELECT
  p.provider_no,
  CONCAT(p.first_name, ' ', p.last_name) AS provider_name,
  COUNT(a.appointment_no) as upcoming_appointments
FROM provider p
LEFT JOIN appointment a ON p.provider_no = a.provider_no
  AND a.appointment_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 7 DAY)
  AND a.status != 'C'
WHERE p.status = '1'
GROUP BY p.provider_no, provider_name
ORDER BY upcoming_appointments DESC;
```

---

## 5. Advanced Queries

### Patient Master Index

**Complete patient profile with related data counts:**
```sql
SELECT
  d.demographic_no,
  CONCAT(d.last_name, ', ', d.first_name) AS patient_name,
  DATE_FORMAT(CONCAT(d.year_of_birth, '-', d.month_of_birth, '-', d.date_of_birth), '%Y-%m-%d') AS dob,
  d.hin,
  d.chart_no,
  CONCAT(p.first_name, ' ', p.last_name) AS mrp,
  (SELECT COUNT(*) FROM appointment WHERE demographic_no = d.demographic_no) as total_appointments,
  (SELECT COUNT(*) FROM casemgmt_note WHERE demographic_no = d.demographic_no AND archived = 0) as total_notes,
  (SELECT COUNT(*) FROM drugs WHERE demographic_no = d.demographic_no AND archived = '0') as active_prescriptions,
  (SELECT COUNT(*) FROM allergies WHERE demographic_no = d.demographic_no AND archived = 0) as active_allergies,
  (SELECT COUNT(*) FROM prevention WHERE demographic_no = d.demographic_no AND deleted = 0) as immunizations
FROM demographic d
LEFT JOIN provider p ON d.provider_no = p.provider_no
WHERE d.demographic_no = 1;
```

---

### Data Quality Checks

**Find patients with missing critical data:**
```sql
SELECT
  demographic_no,
  first_name,
  last_name,
  CASE
    WHEN hin IS NULL OR hin = '' THEN 'Missing HIN'
    WHEN phone IS NULL OR phone = '' THEN 'Missing Phone'
    WHEN year_of_birth IS NULL THEN 'Missing DOB'
    WHEN provider_no IS NULL OR provider_no = '' THEN 'No MRP'
    ELSE 'Unknown Issue'
  END AS data_issue
FROM demographic
WHERE (hin IS NULL OR hin = '')
   OR (phone IS NULL OR phone = '')
   OR (year_of_birth IS NULL)
   OR (provider_no IS NULL OR provider_no = '')
LIMIT 50;
```

---

### Healthcare Analytics

**Appointment no-show rate by provider:**
```sql
SELECT
  p.provider_no,
  CONCAT(p.first_name, ' ', p.last_name) AS provider_name,
  COUNT(CASE WHEN a.status = 'N' THEN 1 END) as no_shows,
  COUNT(*) as total_appointments,
  ROUND(COUNT(CASE WHEN a.status = 'N' THEN 1 END) * 100.0 / COUNT(*), 2) as no_show_percentage
FROM provider p
LEFT JOIN appointment a ON p.provider_no = a.provider_no
  AND a.appointment_date >= DATE_SUB(CURDATE(), INTERVAL 90 DAY)
WHERE p.status = '1'
GROUP BY p.provider_no, provider_name
HAVING total_appointments > 0
ORDER BY no_show_percentage DESC;
```

---

## 6. Database Maintenance Commands

### Backup Database

```bash
# From inside container
mysqldump -h db -u root -ppassword oscar > /workspace/backup_$(date +%Y%m%d).sql

# Compress backup
mysqldump -h db -u root -ppassword oscar | gzip > /workspace/backup_$(date +%Y%m%d).sql.gz
```

### Restore Database

```bash
# Restore from backup
mysql -h db -u root -ppassword oscar < /workspace/backup_20251211.sql

# Restore from compressed backup
gunzip < /workspace/backup_20251211.sql.gz | mysql -h db -u root -ppassword oscar
```

### Check Database Size

```sql
SELECT
  table_schema AS 'Database',
  ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) AS 'Size (MB)'
FROM information_schema.tables
WHERE table_schema = 'oscar'
GROUP BY table_schema;
```

### Optimize Tables

```sql
-- Optimize a specific table
OPTIMIZE TABLE demographic;

-- Optimize all tables (use with caution, can take time)
-- First, get list of all tables
SELECT CONCAT('OPTIMIZE TABLE ', table_name, ';')
FROM information_schema.tables
WHERE table_schema = 'oscar';
```

---

## 7. Important Notes and Best Practices

### Security Warnings

⚠️ **NEVER** use `root/password` credentials in production
⚠️ **ALWAYS** use parameterized queries in application code
⚠️ **NEVER** log Patient Health Information (PHI)
⚠️ **ALWAYS** comply with HIPAA/PIPEDA regulations

### Query Performance

- **Use LIMIT** on large tables to avoid timeouts
- **Create indexes** on frequently queried columns
- **Use EXPLAIN** to analyze query performance:
  ```sql
  EXPLAIN SELECT * FROM demographic WHERE last_name = 'Smith';
  ```

### Date Handling

OpenO EMR stores dates in multiple formats:
- Separate fields: `year_of_birth`, `month_of_birth`, `date_of_birth`
- DATE type: `appointment_date`
- DATETIME type: `lastUpdateDate`

**Convert to standard date format:**
```sql
DATE_FORMAT(CONCAT(year_of_birth, '-', month_of_birth, '-', date_of_birth), '%Y-%m-%d')
```

### Audit Trail

Most tables include audit fields:
- `lastUpdateUser` - User who last modified the record
- `lastUpdateDate` - Timestamp of last modification

**Example:**
```sql
SELECT * FROM demographic
WHERE lastUpdateDate >= DATE_SUB(NOW(), INTERVAL 24 HOUR);
```

---

## 8. Quick Reference Table

| **Task** | **Command** |
|----------|-------------|
| Connect to database | `db-connect` |
| Show all tables | `SHOW TABLES;` |
| Describe table | `DESCRIBE table_name;` |
| Count rows | `SELECT COUNT(*) FROM table_name;` |
| Export to CSV | `SELECT ... INTO OUTFILE '/tmp/export.csv' FIELDS TERMINATED BY ',';` |
| Current database | `SELECT DATABASE();` |
| Database version | `SELECT VERSION();` |
| Current user | `SELECT CURRENT_USER();` |
| Show processes | `SHOW PROCESSLIST;` |
| Kill query | `KILL query_id;` |

---

## 9. Common Table Prefixes

OpenO EMR uses prefixes to organize related tables:

| **Prefix** | **Purpose** | **Examples** |
|------------|-------------|--------------|
| `form*` | Medical forms | `formBPMH`, `formintakea` |
| `Eform*` | Electronic forms | `EFormDocs`, `EFormReportTool` |
| `HRM*` | Hospital Report Manager | `HRMDocument`, `HRMCategory` |
| `BORN*` | Ontario perinatal reporting | `BORNPathwayMapping` |
| `HL7*` | HL7 integration | `HL7HandlerMSHMapping` |
| `CVCImmunization` | COVID vaccination | `CVCImmunization`, `CVCMapping` |
| `Drug*` | Drug database | `DrugProduct`, `DrugDispensing` |

---

## 10. Additional Resources

### Documentation
- **OpenO EMR Docs:** https://support.openosp.ca/
- **MySQL Documentation:** https://dev.mysql.com/doc/
- **MariaDB Documentation:** https://mariadb.com/kb/en/

### Database Schema
- Schema files located at: `/workspace/database/mysql/`
- Current schema: `oscarinit_2025.sql`
- Provincial schemas: `oscarinit_bc.sql`, `oscarinit_on.sql`

### Development Tools
- **DBeaver:** https://dbeaver.io/
- **MySQL Workbench:** https://www.mysql.com/products/workbench/
- **phpMyAdmin:** Available as Docker container if needed

---

## Appendix: Full Table List (Sample)

```
AppDefinition, AppUser, BORNPathwayMapping, CVCImmunization,
Consent, Contact, DemographicContact, DigitalSignature,
DrugDispensing, DrugProduct, EFormDocs, Eyeform, Facility,
FaxClientLog, Flowsheet, HRMDocument, HL7HandlerMSHMapping,
LookupCodeValue, OLISQueryLog, PHRConsentLog, QuickList,
Measurements, TicklerComment, UserProperty, ValidDateFmt,
accessServiceNotify, admission, allergies, appointmentType,
appointment, billactivity, billing, billingmaster,
casemgmt_issue, casemgmt_note, clinic, consultationServices,
demographic, desannualreviewplan, diagnosticcode,
drugs, dxresearch, encounterForm, faxConfig, formONAREnhanced,
formRourke2006, log, measurementGroup, measurementType,
oscarLog, prevention, preventions, professionalSpecialists,
program, provider, providerArchive, providersite,
secUserRole, security, securityArchive, site, tickler
... (555 total tables)
```

---

**Document Version:** 1.0
**Last Updated:** 2025-12-11
**Maintained By:** OpenO EMR Community

---

For questions or corrections, visit: https://support.openosp.ca/
