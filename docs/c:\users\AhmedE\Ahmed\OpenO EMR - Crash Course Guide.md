---
tags: #openo #tutorial #crash-course #healthcare #emr #training
created: 2025-12-11
version: 1.0
audience: New Users & Healthcare Providers
---

# OpenO EMR - Crash Course Guide

**A Hands-On Tutorial for Healthcare Electronic Medical Records**

---

## Table of Contents

1. [Getting Started](#getting-started)
2. [Module 1: Patient Management (Demographics)](#module-1-patient-management-demographics)
3. [Module 2: Appointment Scheduling](#module-2-appointment-scheduling)
4. [Module 3: Clinical Encounters & Notes](#module-3-clinical-encounters--notes)
5. [Module 4: Prescriptions (e-Prescribing)](#module-4-prescriptions-e-prescribing)
6. [Module 5: Billing](#module-5-billing)
7. [Module 6: Lab Results & Documents](#module-6-lab-results--documents)
8. [Module 7: Preventive Care](#module-7-preventive-care)
9. [Quick Reference Tables](#quick-reference-tables)
10. [Sample Datasets](#sample-datasets)
11. [Best Practices](#best-practices)

---

## Getting Started

### Login Credentials (Default Development)

```
Username: openodoc
Password: openo2025
```

⚠️ **Important**: These are development credentials. Production systems will have unique credentials assigned by your administrator.

### Main Navigation Overview

After logging in, you'll see the main OpenO EMR interface:

**Top Navigation Bar:**
- **Home** - Dashboard and main menu
- **Schedule** - Appointment calendar
- **Patients** - Search and manage demographics
- **e-Chart** - Clinical documentation
- **Billing** - Invoice and claims management
- **Reports** - Clinical and administrative reports
- **Admin** - System configuration (admin only)

**Left Sidebar:**
- Quick search for patients
- Recent patients list
- Provider information
- System messages

**Main Dashboard:**
- Today's appointments
- Pending tasks/ticklers
- Recent lab results
- System notifications
- Quick action buttons

💡 **Tip**: Bookmark frequently used pages using your browser bookmarks for faster access.

---

## Module 1: Patient Management (Demographics)

### Understanding Patient Demographics

The demographic module is the foundation of OpenO EMR. Every patient record contains:
- **Personal Information**: Name, DOB, gender, contact details
- **Health Insurance**: HIN (Health Insurance Number), version code
- **Address Information**: Residential and mailing addresses
- **Emergency Contacts**: Next of kin information
- **Clinical Details**: Family doctor, rostering status, chart number

### Step 1: Search for Existing Patients

Before adding a new patient, **always search first** to avoid duplicates.

**Navigation:** Home → Search or Patients → Search

1. Click **"Search"** in the top menu
2. Enter search criteria:
   - Last Name (most common)
   - First Name
   - DOB (YYYY-MM-DD format)
   - Health Card Number
   - Chart Number
3. Click **"Search"** button
4. Review results carefully

⚠️ **Important**: Duplicate patient records violate privacy regulations and can cause billing issues.

💡 **Tip**: Use wildcards (*) for partial searches: `Smi*` finds Smith, Smit, Smither, etc.

### Step 2: Add a New Patient

**Navigation:** Home → Patients → Add Record or Demographic → Add Record

**Workflow:**

1. **Click "Add Record"** button
2. **Fill Required Fields** (marked with red asterisk *):

   **Personal Information:**
   - Last Name: `Smith`
   - First Name: `John`
   - Date of Birth: `1980-05-15` (YYYY-MM-DD)
   - Gender: `Male` (dropdown)
   - Chart Number: Auto-generated or enter custom

   **Health Insurance (HIN):**
   - Province: `BC` or `ON` (dropdown)
   - Health Card Number: `9876543210` (BC format) or `1234567890AB` (ON format with letters)
   - Version Code: `66` (BC) or leave blank (ON)
   - Health Card Type: `PHN` (BC) or `HN` (ON)

   ⚠️ **Important**: HIN validation is province-specific. BC requires 10 digits + 2-digit version. Ontario requires 10 digits + 2 letters.

   **Contact Information:**
   - Residential Address: `123 Main Street`
   - City: `Vancouver` or `Toronto`
   - Province: `BC` or `ON`
   - Postal Code: `V6B 1A1` (BC) or `M5V 3A8` (ON)
   - Home Phone: `604-555-1234` or `416-555-1234`
   - Cell Phone: `604-555-5678`
   - Email: `john.smith@email.com`

3. **Optional but Recommended Fields:**

   - **Rostering Status**: Select if patient is rostered to your practice
   - **Family Doctor**: Select from provider dropdown
   - **Language**: `English`, `French`, or other
   - **Emergency Contact**: Name and phone number
   - **Next of Kin**: Relationship and contact details

4. **Click "Save"** button at bottom

5. **Verify Success**: System displays patient master record

💡 **Tip**: Fill out as much information as possible during initial registration to avoid incomplete records.

### Step 3: Update Patient Information

**Navigation:** Search patient → Click on name → Click "Edit" or "Update"

1. **Search and open patient record**
2. **Click "Edit" or "Update" button** (top right)
3. **Modify fields** as needed
4. **Click "Save"** to commit changes
5. **Review audit trail**: System automatically logs who changed what and when

⚠️ **Important**: All changes are audited. Never modify another provider's notes without proper authorization.

### Step 4: Understanding HIN Validation

**British Columbia (BC) Format:**
- 10 digits: `9876543210`
- 2-digit version code: `66`
- Example: `9876543210 (66)`

**Ontario (ON) Format:**
- 10 digits + 2 letters: `1234567890AB`
- No version code
- Example: `1234567890AB`

**Validation Rules:**
- BC HIN must be exactly 10 digits with valid version code
- ON HIN must be 10 digits followed by 2 uppercase letters
- System validates format and displays errors if invalid
- Invalid HIN blocks billing submission

💡 **Tip**: If HIN validation fails, double-check the province selection matches the card format.

---

## Module 2: Appointment Scheduling

### Understanding the Schedule

OpenO EMR uses a time-based calendar system with:
- **Provider-specific schedules**: Each provider has their own calendar
- **Time slots**: Typically 15-minute increments (configurable)
- **Appointment types**: Different durations (15, 30, 45, 60 minutes)
- **Status tracking**: Booked, Confirmed, No-Show, Cancelled, etc.
- **Reason codes**: Clinical reason for visit
- **Billing codes**: Service code associated with visit

### Step 1: Access the Schedule

**Navigation:** Home → Schedule or Appointment → Schedule

1. **Click "Schedule"** in top menu
2. **Select Provider** from dropdown (defaults to logged-in provider)
3. **Select Date** using calendar picker or arrows
4. **View Options**: Day, Week, Month views

### Step 2: Set Up Provider Schedule Template (Admin/One-Time Setup)

**Navigation:** Admin → Schedule Template

💡 **Note**: This is typically done by administrators during initial setup.

1. **Click "Admin" → "Schedule Template"**
2. **Select Provider** from dropdown
3. **Define Time Blocks**:
   - Monday: 9:00 AM - 5:00 PM (15-min slots)
   - Tuesday: 9:00 AM - 5:00 PM (15-min slots)
   - Wednesday: 9:00 AM - 12:00 PM (morning clinic only)
   - Thursday: 1:00 PM - 7:00 PM (afternoon/evening clinic)
   - Friday: 9:00 AM - 3:00 PM (short day)
4. **Set Lunch Break**: 12:00 PM - 1:00 PM (blocked time)
5. **Click "Save Template"**

### Step 3: Book an Appointment (Step-by-Step)

**Workflow:**

1. **Navigate to Schedule** for desired date
2. **Click on Time Slot** (e.g., 10:00 AM)
3. **Search for Patient**:
   - Enter last name or chart number
   - Click "Search"
   - Select patient from results
4. **Fill Appointment Details**:

   **Required Fields:**
   - **Date**: `2025-12-15` (auto-populated from clicked slot)
   - **Start Time**: `10:00` (auto-populated)
   - **Duration**: `15` (minutes) - select from dropdown
   - **Provider**: Auto-selected, change if needed
   - **Appointment Type**: `Regular`, `Urgent`, `Follow-up`, etc.
   - **Status**: `Booked` (default)

   **Recommended Fields:**
   - **Reason**: `Annual Physical`, `Cold/Flu`, `Follow-up`, etc.
   - **Notes**: Brief note visible to provider: "Requesting form completion"
   - **Billing Code**: Select appropriate service code (e.g., `00100` - Office Visit)

5. **Click "Save" or "Book Appointment"**

6. **Verify on Calendar**: Appointment appears in time slot with patient name

💡 **Tip**: Use color-coding for appointment types to quickly identify urgent vs routine visits.

### Step 4: Search Appointments

**Navigation:** Schedule → Search Appointments

**Search Options:**
- **By Patient**: Enter patient name or chart number
- **By Date Range**: Select start and end dates
- **By Provider**: Filter by specific provider
- **By Status**: Filter by Booked, Confirmed, No-Show, etc.

**Workflow:**
1. **Click "Search Appointments"** button
2. **Enter Search Criteria**:
   - Patient Name: `Smith, John`
   - Date Range: `2025-12-01` to `2025-12-31`
3. **Click "Search"**
4. **Review Results**: List shows all matching appointments
5. **Click on Appointment** to edit or view details

### Step 5: Appointment Statuses Explained

| Status | Meaning | When to Use |
|--------|---------|-------------|
| **Booked** | Appointment scheduled | Default when booking |
| **Confirmed** | Patient confirmed attendance | After reminder call |
| **No-Show** | Patient didn't attend | Mark after 15 min wait |
| **Cancelled** | Appointment cancelled | Patient called to cancel |
| **Rescheduled** | Moved to different time | When rebooking |
| **Here** | Patient checked in | Front desk check-in |
| **Complete** | Appointment finished | After clinical encounter |
| **Billed** | Invoice created | After billing submitted |

💡 **Tip**: Update status to "Here" when patient checks in to track waiting times.

### Step 6: Manage Appointment Conflicts

**Scenario**: Double-booking or overlapping appointments

**Workflow:**
1. **System displays warning**: "Conflict detected - slot already booked"
2. **Options**:
   - **Override**: Book anyway (overbooking)
   - **Find Next Available**: Search for open slot
   - **Waitlist**: Add to waiting list
3. **If Override**: System marks slot with conflict indicator (red/yellow)

⚠️ **Important**: Overbooking can cause delays and patient dissatisfaction. Use sparingly.

---

## Module 3: Clinical Encounters & Notes

### Understanding Clinical Documentation

OpenO EMR uses **encounter-based charting** with:
- **SOAP Format**: Subjective, Objective, Assessment, Plan
- **Issue-Based Charting**: Link notes to specific medical problems
- **Integrated Workflow**: Access Rx, labs, billing from encounter window
- **Digital Signatures**: Sign notes electronically
- **Version Control**: Track note edits and changes

### Step 1: Open the e-Chart (Encounter Window)

**Navigation:** Search patient → Click "e-Chart" or "Encounter"

**Workflow:**
1. **Search for Patient**: Use patient search
2. **Click Patient Name** to open master record
3. **Click "e-Chart" button** (top of screen)
4. **Encounter Window Opens**: Left sidebar + main workspace

**Encounter Window Layout:**
- **Left Sidebar**: Measurements, allergies, medications, preventions
- **Main Area**: Clinical notes, SOAP entries
- **Right Sidebar**: Quick tools (Rx, billing, consults)
- **Bottom**: Issue list and problem management

### Step 2: Create a Clinical Encounter (SOAP Note)

**Workflow:**

1. **Click "Encounter"** or **"New Note"** button
2. **Select Encounter Template** (if prompted):
   - **Blank SOAP**: Standard template
   - **Follow-up**: For return visits
   - **Annual Physical**: Comprehensive exam
   - **Minor Ailment**: Quick visit template

3. **Fill SOAP Sections**:

   **Subjective (S):**
   ```
   CC: Cough and sore throat x 3 days
   HPI: 45yo M presents with productive cough (yellow sputum),
   sore throat, low-grade fever (38.2°C at home). No SOB.
   No chest pain. Denies recent travel.
   ```

   **Objective (O):**
   ```
   Vitals: BP 128/82, HR 78, Temp 38.1°C, RR 16, SpO2 98% RA
   PE: Alert, NAD. Throat erythematous, no exudate.
   Lungs clear bilaterally. No wheezing.
   ```

   **Assessment (A):**
   ```
   1. Acute viral upper respiratory tract infection (URTI)
   2. Pharyngitis
   ```

   **Plan (P):**
   ```
   1. Rest, fluids, acetaminophen 500mg q6h PRN fever/pain
   2. Throat lozenges
   3. Return if worsening symptoms, SOB, or fever >3 days
   4. No antibiotics indicated at this time
   ```

4. **Link to Issues** (Clinical Problems):
   - Click **"Add Issue"** or select existing issue
   - Example: Link to "Acute URTI" diagnosis
   - Issue appears in problem list with note linked

5. **Add Billing Code**:
   - Click **"Add Billing"** button
   - Select service code: `00100` (Office Visit - 15 min)
   - Diagnosis code: `465` (Acute URTI - ICD-9)
   - Click **"Add"**

6. **Click "Save" or "Sign Note"**

7. **Digital Signature**:
   - System prompts for signature
   - Review note one final time
   - Click **"Sign"** to finalize
   - Signed notes are locked (edits create amendments)

⚠️ **Important**: Once signed, notes cannot be edited directly. You must create an amendment or addendum.

💡 **Tip**: Use templates for common visit types to save time and ensure consistency.

### Step 3: Issue-Based Charting

**What is Issue-Based Charting?**
- Organize notes by medical problems/diagnoses
- Track chronic conditions over time
- Link prescriptions, labs, and notes to specific issues
- Generate problem-focused reports

**Workflow:**

1. **Open e-Chart** for patient
2. **Click "Issues" tab** (left sidebar or bottom of screen)
3. **Add New Issue**:
   - Click **"Add Issue"** button
   - **Type**: Chronic, Acute, Concern
   - **Description**: `Hypertension` or `Type 2 Diabetes Mellitus`
   - **ICD-9 Code**: `401.9` (Essential Hypertension)
   - **Onset Date**: `2020-03-15`
   - **Status**: Active, Resolved, Chronic
   - Click **"Save"**

4. **Link Note to Issue**:
   - While creating SOAP note, check box next to issue
   - Note appears in issue timeline
   - Future notes can reference this issue

5. **View Issue History**:
   - Click on issue name
   - System displays all linked notes, Rx, labs
   - Timeline view shows progression

💡 **Tip**: Maintain an accurate problem list for better continuity of care and quality reporting.

### Step 4: Linking Prescriptions to Notes

**Workflow:**

1. **Within Encounter Window**, click **"Rx"** button (right sidebar)
2. **Search Drug**: Enter drug name (e.g., `amoxicillin`)
3. **Select Strength**: `500mg capsules`
4. **Fill Prescription Details**:
   - **Sig (Directions)**: `Take 1 capsule by mouth three times daily`
   - **Quantity**: `21 capsules`
   - **Repeats**: `0`
   - **Duration**: `7 days`
5. **Link to Issue**: Select `Acute URTI` from dropdown
6. **Link to Note**: Check "Link to current encounter"
7. **Click "Add to Prescription List"**
8. **Print or eRx**: Send electronically or print for patient

⚠️ **Important**: Always check for drug interactions and allergies before prescribing.

### Step 5: Digital Signatures and Amendments

**Signing a Note:**
1. **Review note** for accuracy
2. **Click "Sign" button**
3. **System prompts**: "Are you sure you want to sign this note?"
4. **Click "Yes"**
5. **Note is locked** - timestamp and provider name recorded

**Creating an Amendment:**
1. **Open signed note**
2. **Click "Amend" or "Addendum" button**
3. **Enter amendment text**:
   ```
   AMENDMENT (2025-12-11 2:30 PM by Dr. Smith):
   Clarification - patient also reports mild headache.
   No photophobia or neck stiffness noted.
   ```
4. **Click "Save Amendment"**
5. **Original note remains visible** with amendment appended

⚠️ **Important**: Never delete signed notes. Use amendments to correct errors or add information.

---

## Module 4: Prescriptions (e-Prescribing)

### Understanding the Prescription Module

OpenO EMR's e-Prescribing system includes:
- **Drug Database**: Comprehensive Canadian medication formulary
- **ATC Codes**: Anatomical Therapeutic Chemical classification
- **Interaction Checking**: Automatic drug-drug interaction warnings
- **Allergy Alerts**: Cross-reference with patient allergies
- **Prescription History**: Complete medication timeline
- **Favorites**: Save frequently prescribed drugs
- **Templates**: Create custom Rx templates

### Step 1: Access Prescription Module

**Navigation:** e-Chart → Rx or Medications Tab

**Workflow:**
1. **Open patient e-Chart**
2. **Click "Rx" tab** or **"Medications"** in left sidebar
3. **View Current Medications**: Active prescriptions display
4. **Click "Add New Prescription"** or **"Prescribe"** button

### Step 2: Search Drug Database

**Workflow:**

1. **Click "Search Drug"** or enter drug name in search box
2. **Search Options**:
   - **By Brand Name**: `Tylenol`, `Advil`
   - **By Generic Name**: `acetaminophen`, `ibuprofen`
   - **By ATC Code**: `N02BE01` (acetaminophen)
   - **By Drug Class**: `NSAIDs`, `ACE Inhibitors`

3. **Example Search**: `lisinopril`
4. **Results Display**:
   - Lisinopril 5mg tablets
   - Lisinopril 10mg tablets
   - Lisinopril 20mg tablets
   - Generic and brand name options

5. **Select Desired Formulation**: Click on drug name

### Step 3: Create Prescription with Sample Drug

**Example: Prescribe Amoxicillin for Infection**

**Workflow:**

1. **Search Drug**: `amoxicillin`
2. **Select**: `Amoxicillin 500mg capsules`
3. **Fill Prescription Form**:

   **Drug Information:**
   - **Drug Name**: Amoxicillin 500mg capsules (auto-filled)
   - **Strength**: 500mg (confirmed)
   - **Formulation**: Capsules

   **Directions (Sig):**
   - **Method**: Select from dropdown or free-text
   - **Standard Sig**: `Take 1 capsule by mouth three times daily with food`
   - **Or Build Sig**:
     - Route: `Oral`
     - Frequency: `Three times daily (TID)`
     - Instructions: `With food`

   **Quantity and Duration:**
   - **Quantity**: `21 capsules` (7 days x 3/day)
   - **Duration**: `7 days`
   - **Repeats**: `0` (no refills for antibiotics)

   **Additional Details:**
   - **Indication**: `Acute bacterial infection` (optional but recommended)
   - **Start Date**: Auto-set to today or select custom date
   - **Long-term**: Check if chronic medication
   - **PRN (As Needed)**: Leave unchecked for scheduled medication

4. **Interaction Check**:
   - System automatically checks against:
     - Current medications
     - Patient allergies
     - Patient age/weight (if applicable)
   - **Warnings Display**: Review any alerts
   - **Example Alert**: "Amoxicillin may interact with warfarin - monitor INR"

5. **Review and Confirm**:
   - Double-check dosage, frequency, duration
   - Verify patient identity (right patient)
   - Confirm drug allergy check completed

6. **Click "Add to Prescription" or "Save"**

7. **Print or Send**:
   - **Print**: Generate paper prescription
   - **eFax**: Send to pharmacy electronically
   - **Patient Copy**: Print for patient records

💡 **Tip**: Always counsel patients on proper medication use, side effects, and when to return.

### Step 4: Prescription Templates

**Create Custom Template for Frequently Prescribed Medications:**

**Workflow:**

1. **Navigate to**: Rx → Templates or Favorites
2. **Click "Create New Template"**
3. **Template Example**: "Hypertension Starter Pack"

   **Template Details:**
   - **Template Name**: `HTN Initial Therapy`
   - **Description**: `First-line hypertension treatment`
   - **Drugs Included**:
     1. Amlodipine 5mg tablets
        - Sig: Take 1 tablet by mouth once daily
        - Quantity: 30 tablets
        - Repeats: 2
     2. Hydrochlorothiazide 12.5mg tablets
        - Sig: Take 1 tablet by mouth once daily in the morning
        - Quantity: 30 tablets
        - Repeats: 2

4. **Save Template**
5. **Use Template**:
   - Click "Apply Template"
   - Select `HTN Initial Therapy`
   - Review and modify as needed
   - Add to patient prescription list

💡 **Tip**: Create templates for chronic disease management to ensure consistency across patients.

### Step 5: Drug Interaction Warnings

**Understanding Interaction Levels:**

| Level | Severity | Action Required |
|-------|----------|-----------------|
| **Minor** | Low risk | Monitor patient |
| **Moderate** | Possible risk | Consider alternative or monitor closely |
| **Major** | Serious risk | Avoid combination or adjust dose |
| **Contraindicated** | Life-threatening | Do NOT prescribe together |

**Example Interaction:**
- **Drug Pair**: Warfarin + Amoxicillin
- **Level**: Moderate
- **Mechanism**: Increased bleeding risk
- **Action**: Monitor INR more frequently (weekly), counsel patient on bleeding signs

**Workflow When Warning Appears:**

1. **System displays alert**: "Drug Interaction Detected"
2. **Review Details**: Click "More Info"
3. **Options**:
   - **Override**: Document reason and proceed (with caution)
   - **Change Drug**: Select alternative medication
   - **Adjust Dose**: Reduce dose of one medication
   - **Cancel**: Do not prescribe
4. **Document Decision** in clinical note
5. **Counsel Patient** on risks and monitoring

⚠️ **Important**: Never ignore contraindicated combinations. Always find alternatives.

### Step 6: Refill Management

**Workflow:**

1. **Patient Requests Refill** (phone call, in-person)
2. **Open Patient e-Chart** → **Rx Tab**
3. **Locate Medication** in active list
4. **Check Refills Remaining**:
   - If refills available: Authorize refill
   - If no refills: Review patient condition
5. **If Renewal Needed**:
   - Click **"Renew Prescription"**
   - Update details if needed (dose change, quantity)
   - Add refills if appropriate
   - Click **"Save and Send"**
6. **If Patient Needs Assessment**:
   - Schedule appointment
   - Do NOT authorize refill until seen
   - Document reason in note

💡 **Tip**: Use refill limits to ensure regular follow-up for chronic medications.

---

## Module 5: Billing

### Understanding Billing in OpenO EMR

OpenO EMR supports **province-specific billing**:
- **British Columbia (BC)**: MSP (Medical Services Plan) via Teleplan
- **Ontario (ON)**: OHIP (Ontario Health Insurance Plan) via EDT
- **Generic**: For private billing or other provinces

**Key Concepts:**
- **Service Codes**: Billable procedures (e.g., 00100 = Office Visit)
- **Diagnostic Codes**: ICD-9 or ICD-10 diagnosis codes
- **Fee Schedule**: Province-specific pricing
- **Claims Submission**: Electronic batch processing
- **Remittance**: Payment reconciliation

### Step 1: Understanding Service Codes (BC vs ON)

**British Columbia (MSP) Common Codes:**
| Code | Description | Fee |
|------|-------------|-----|
| **00100** | Office Visit - 5-15 min | $31.62 |
| **00110** | Office Visit - 15+ min | $38.08 |
| **00115** | Office Visit - Extended | $75.00 |
| **03333** | Annual Health Exam | $76.00 |
| **13337** | IUD Insertion | $141.00 |

**Ontario (OHIP) Common Codes:**
| Code | Description | Fee |
|------|-------------|-----|
| **A001A** | Minor Assessment (under 5 min) | $33.70 |
| **A003A** | General Assessment (5-14 min) | $66.65 |
| **A005A** | Intermediate Assessment (15-25 min) | $77.20 |
| **A007A** | Complete Assessment (25-45 min) | $102.15 |
| **A999** | Uninsured Services | Variable |

💡 **Tip**: Memorize your 5-10 most commonly used service codes for faster billing.

### Step 2: Create an Invoice (Step-by-Step)

**Scenario**: Bill for an office visit after seeing a patient

**Navigation:** e-Chart → Billing or Billing → Create Invoice

**Workflow:**

1. **Open Patient e-Chart** or **Navigate to Billing Module**
2. **Click "Create Invoice" or "Bill"** button
3. **Verify Patient Information**:
   - Patient Name: John Smith
   - DOB: 1980-05-15
   - HIN: 9876543210 (BC) or 1234567890AB (ON)
   - Valid health card: Check green indicator

4. **Select Service Code**:
   - **BC Example**:
     - Service Code: `00110` (Office Visit 15+ min)
     - Fee: $38.08
     - Units: 1
   - **ON Example**:
     - Service Code: `A003A` (General Assessment)
     - Fee: $66.65
     - Units: 1

5. **Add Diagnostic Code** (Required):
   - **ICD-9 Code**: `465` (Acute Upper Respiratory Infection)
   - **Or Search**: Type "URI" and select from list
   - **Multiple Diagnoses**: Add up to 3 diagnosis codes (primary, secondary, tertiary)

   **Common ICD-9 Diagnosis Codes:**
   | Code | Description |
   |------|-------------|
   | **401.9** | Essential Hypertension |
   | **250.00** | Diabetes Mellitus Type 2 |
   | **465** | Acute Upper Respiratory Infection |
   | **780.60** | Fever |
   | **786.2** | Cough |

6. **Fill Additional Billing Details**:
   - **Service Date**: `2025-12-11` (date of visit)
   - **Service Location**: `Office` (dropdown)
   - **Billing Provider**: Auto-filled with logged-in provider
   - **Referring Provider**: Leave blank unless referred
   - **Time Rendered**: Optional (some codes require start/end time)

7. **Add Secondary Services** (if applicable):
   - Click **"Add Service"** to add additional billable items
   - Example: Add counseling code or procedure code
   - Each service requires diagnostic code

8. **Review Invoice Summary**:
   ```
   Patient: SMITH, John (1980-05-15)
   HIN: 9876543210 (BC)

   Service Date: 2025-12-11
   Provider: Dr. OpenO Doc

   Service Code  Description              Dx Code  Fee
   ----------------------------------------------------------
   00110         Office Visit 15+ min     465      $38.08

   Total: $38.08
   ```

9. **Click "Save" or "Submit"**

10. **Verify Confirmation**: System displays "Invoice created successfully"

💡 **Tip**: Bill immediately after each visit to avoid forgetting details and ensure timely payment.

### Step 3: Diagnostic Codes (ICD-9/10)

**ICD-9 (Used in most Canadian provinces):**
- 3-5 digit numeric codes
- Example: `401.9` = Essential Hypertension

**ICD-10 (Some jurisdictions transitioning):**
- Alphanumeric codes (letter + numbers)
- Example: `I10` = Essential Hypertension

**Common Diagnosis Code Categories:**

| Category | ICD-9 Range | Examples |
|----------|-------------|----------|
| **Infectious Diseases** | 001-139 | 465 (URTI), 487 (Influenza) |
| **Circulatory** | 390-459 | 401.9 (HTN), 414.00 (CAD) |
| **Respiratory** | 460-519 | 493.90 (Asthma), 786.2 (Cough) |
| **Endocrine** | 240-279 | 250.00 (DM Type 2), 244.9 (Hypothyroid) |
| **Musculoskeletal** | 710-739 | 724.2 (Low back pain), 715.9 (OA) |
| **Mental Health** | 290-319 | 311 (Depression), 300.00 (Anxiety) |

**Search Tips:**
1. **Use Description**: Type symptom/condition name
2. **Use Category**: Browse by body system
3. **Use Recent**: Select from recently used codes

⚠️ **Important**: Diagnosis code must match clinical documentation. Fraudulent coding is illegal.

### Step 4: Batch Billing Submission

**Batch Billing Process** (Electronic Submission to Province):

**Navigation:** Billing → Batch Billing or Reports → Billing Reports

**Workflow:**

1. **Review Unbilled Services**:
   - Click **"Unbilled Services Report"**
   - Date Range: `2025-12-01` to `2025-12-11`
   - Provider: Select your name
   - Click **"Generate Report"**

2. **Verify Claims**:
   - Review each service line for accuracy
   - Check HIN validation (green = valid, red = invalid)
   - Fix any errors or rejected claims

3. **Create Batch**:
   - Click **"Create Batch"** button
   - **Batch Name**: `Batch_2025-12-11_DrSmith`
   - **Provider**: Select provider
   - **Billing Period**: `2025-12-01` to `2025-12-11`
   - **Include Services**: Select all verified claims
   - Click **"Create Batch"**

4. **Generate Billing File**:
   - **BC (Teleplan)**:
     - Format: MSP Teleplan format
     - File extension: `.msp`
     - Click **"Export Teleplan File"**
   - **ON (EDT)**:
     - Format: OHIP EDT format
     - File extension: `.edt`
     - Click **"Export EDT File"**

5. **Submit to Province**:
   - **BC**: Upload to Teleplan website (www.teleplan.net)
   - **ON**: Submit via EDT portal or clearinghouse
   - Keep copy of submission for records

6. **Track Submission**:
   - System logs submission date/time
   - Status: Submitted, Pending, Paid, Rejected
   - Monitor remittance reports

⚠️ **Important**: Submit batches regularly (weekly recommended) to ensure timely payment.

💡 **Tip**: Run batch billing reports weekly to catch billing errors early.

---

## Module 6: Lab Results & Documents

### Understanding Document Management

OpenO EMR handles various document types:
- **Lab Results**: Blood work, urinalysis, imaging reports
- **Consultation Reports**: Specialist letters
- **Imaging**: X-rays, CT, MRI reports (not images themselves typically)
- **Hospital Reports**: Discharge summaries, ER visits
- **Forms**: Completed medical forms, consents
- **Correspondence**: Letters, faxes, emails

### Step 1: Upload a Document

**Navigation:** e-Chart → Documents or Document Management

**Workflow:**

1. **Click "Documents" tab** in patient e-Chart
2. **Click "Upload Document" or "Add Document"** button
3. **Select File**:
   - Click **"Choose File"** or **"Browse"**
   - Navigate to file location
   - Supported formats: PDF, JPG, PNG, TIF, DOC
   - Select file: `lab_results_2025-12-11.pdf`
   - Click **"Open"**

4. **Fill Document Details**:
   - **Document Type**: Select from dropdown
     - Lab Results
     - Consultation Report
     - Imaging Report
     - Hospital Report
     - Correspondence
     - Other
   - **Document Description**: `CBC and electrolytes - 2025-12-11`
   - **Document Date**: `2025-12-11` (date of lab draw, not upload date)
   - **Source**: `LifeLabs` or lab facility name
   - **Responsible Provider**: Select provider to review

5. **Categorize** (Optional but Recommended):
   - **Category**: `Laboratory`
   - **Subcategory**: `Blood Work`

6. **Click "Upload" or "Save"**

7. **Verify Success**: Document appears in document list

💡 **Tip**: Use consistent naming conventions for easier searching: `[Type]_[Description]_[Date]`

### Step 2: Manual Lab Entry

**Scenario**: Enter lab results manually (if not electronic)

**Navigation:** e-Chart → Measurements → Lab Entry or Labs

**Workflow:**

1. **Open Patient e-Chart**
2. **Click "Measurements" or "Labs" tab**
3. **Click "Add Lab Result" or "Manual Entry"**
4. **Select Lab Test Type**:
   - **Complete Blood Count (CBC)**
   - **Basic Metabolic Panel (BMP)**
   - **Lipid Panel**
   - **Thyroid Function (TSH, Free T4)**
   - **Custom** (for unlisted tests)

5. **Enter Lab Values - Example: CBC**

   **Test Date**: `2025-12-11`
   **Ordering Provider**: Auto-filled

   | Test | Result | Units | Reference Range | Flag |
   |------|--------|-------|-----------------|------|
   | **WBC** | 8.5 | x10^9/L | 4.0-11.0 | Normal |
   | **RBC** | 4.8 | x10^12/L | 4.2-5.4 | Normal |
   | **Hemoglobin** | 145 | g/L | 135-175 | Normal |
   | **Hematocrit** | 0.42 | L/L | 0.40-0.50 | Normal |
   | **Platelets** | 250 | x10^9/L | 150-400 | Normal |

   **Abnormal Flags:**
   - System auto-flags values outside reference range
   - **High**: Red up arrow ↑
   - **Low**: Blue down arrow ↓
   - **Critical**: Flashing red

6. **Add Comments** (if applicable):
   ```
   Slight hemolysis noted. Repeat if clinically indicated.
   ```

7. **Click "Save Lab Results"**

8. **Review in Flowsheet**:
   - Navigate to **"Flowsheet"** view
   - Results appear in chronological order
   - Compare with previous values
   - Identify trends (improving, worsening, stable)

💡 **Tip**: Use flowsheet view to track chronic disease markers (HbA1c, creatinine, etc.) over time.

### Step 3: Link Labs to Patient

**Auto-Linking (Electronic Lab Results):**

1. **Lab Results Arrive** electronically (HL7 interface)
2. **System Auto-Matches** based on:
   - Health Card Number (HIN)
   - Date of Birth
   - Patient Name
3. **If Match Found**: Lab auto-attached to patient chart
4. **If No Match**: Lab appears in **"Unmatched Queue"**

**Manual Linking from Queue:**

**Navigation:** Labs → Unmatched Lab Queue

**Workflow:**

1. **Click "Unmatched Labs" or "Inbox"**
2. **Review Lab**:
   - Patient Name: `SMITH, JOHN`
   - DOB: `1980-05-15`
   - HIN: `9876543210`
   - Test: `CBC`
3. **Search for Patient**:
   - Click **"Search Patient"**
   - Enter search criteria
   - Select correct patient from results
4. **Click "Link to Patient"**
5. **Verify**: Lab moves from queue to patient chart
6. **Acknowledge**: Click **"Acknowledge"** to mark as reviewed

⚠️ **Important**: Always verify patient identity before linking labs. Wrong patient = privacy breach.

### Step 4: Document Categories

**Category Structure:**

| Category | Subcategories | Purpose |
|----------|---------------|---------|
| **Laboratory** | Blood Work, Urine, Microbiology | Organize lab results |
| **Imaging** | X-Ray, CT, MRI, Ultrasound | Radiology reports |
| **Consultation** | Cardiology, Orthopedics, Psychiatry | Specialist letters |
| **Hospital** | Discharge Summary, ER Report, Operative | Hospital records |
| **Legal** | Consents, Advance Directives, POA | Legal documents |
| **Correspondence** | Letters, Faxes, Referrals | Communications |

**Best Practices:**
- **Consistent Categorization**: Use same categories across all patients
- **Descriptive Names**: Include date and type in document name
- **Regular Review**: Set reminders to review unacknowledged documents
- **Secure Storage**: Never store documents outside EMR system

💡 **Tip**: Create custom document categories for specialty-specific needs (e.g., "Prenatal" for OB).

---

## Module 7: Preventive Care

### Understanding Prevention Module

OpenO EMR's prevention module tracks:
- **Immunizations**: Vaccines and boosters
- **Screening**: Cancer screening, health maintenance
- **Prevention Schedules**: Age-based and condition-based recommendations
- **Rourke Charts**: Pediatric preventive care guidelines
- **Reminders**: Automated patient reminders for due screenings

### Step 1: Record Immunizations

**Navigation:** e-Chart → Preventions or Immunizations Tab

**Workflow:**

1. **Open Patient e-Chart**
2. **Click "Preventions" or "Immunizations" tab**
3. **Click "Add Prevention" or "New Immunization"**
4. **Select Immunization Type**:
   - Influenza (Flu)
   - COVID-19
   - Pneumococcal (Pneumovax)
   - Tetanus-Diphtheria (Td)
   - MMR (Measles-Mumps-Rubella)
   - Hepatitis A/B
   - HPV (Human Papillomavirus)
   - Shingles (Zoster)

5. **Fill Immunization Details - Example: Flu Vaccine**

   **Required Fields:**
   - **Prevention Type**: `Influenza`
   - **Date Given**: `2025-12-11`
   - **Provider**: Auto-filled with logged-in provider
   - **Result**: `Completed` (or Refused, Contraindicated)

   **Recommended Fields:**
   - **Lot Number**: `FLU2025-12345` (from vaccine vial)
   - **Manufacturer**: `Sanofi Pasteur`, `GlaxoSmithKline`, `Seqirus`
   - **Route**: `Intramuscular (IM)`, `Subcutaneous (SC)`, `Oral`, `Intranasal`
   - **Site**: `Left Deltoid`, `Right Deltoid`, `Left Thigh`, `Right Thigh`
   - **Dose**: `0.5 mL` (standard flu dose for adults)
   - **Expiry Date**: `2026-06-30` (from vaccine vial)

   **Optional Fields:**
   - **Next Due Date**: `2026-12-11` (annual flu shot)
   - **Comments**: `No adverse reactions reported. Patient tolerated well.`

6. **Click "Save Prevention"**

7. **Generate Immunization Record**:
   - Click **"Print Immunization Record"**
   - System generates official document
   - Provide copy to patient

⚠️ **Important**: Always record lot numbers for vaccines. Required for adverse event reporting.

💡 **Tip**: Set calendar reminders for seasonal vaccines (flu in October-November each year).

### Step 2: Prevention Schedules

**Understanding Age-Based Schedules:**

**Adult Prevention Schedule (Sample):**

| Age | Prevention | Frequency |
|-----|------------|-----------|
| **18-64** | Influenza vaccine | Annual |
| **18-64** | Td booster | Every 10 years |
| **50+** | Colorectal screening | Every 2-10 years |
| **50-74** | Mammography (women) | Every 2 years |
| **65+** | Pneumococcal vaccine | Once (Pneumovax 23) |
| **65+** | Shingles vaccine | Once (Shingrix x2 doses) |

**Pediatric Prevention Schedule (Sample - Canadian):**

| Age | Immunizations |
|-----|---------------|
| **2 months** | DTaP-IPV-Hib, Pneumococcal, Rotavirus |
| **4 months** | DTaP-IPV-Hib, Pneumococcal, Rotavirus |
| **6 months** | DTaP-IPV-Hib, Pneumococcal, Rotavirus |
| **12 months** | MMR, Pneumococcal, Meningococcal C |
| **18 months** | DTaP-IPV-Hib, Varicella |
| **4-6 years** | DTaP-IPV, MMR, Varicella |

**Workflow to Check Due Preventions:**

1. **Open Patient e-Chart** → **Preventions Tab**
2. **Click "Check Prevention Schedule"**
3. **System Displays**:
   - **Up to Date**: Green checkmark ✓
   - **Due Soon**: Yellow warning △
   - **Overdue**: Red alert ✗
4. **Example Output**:
   ```
   ✓ Influenza (2025-12-11) - Up to date
   △ Colorectal Screening (Last: 2015) - Due soon
   ✗ Td Booster (Last: 2010) - Overdue by 5 years
   ```
5. **Click on Prevention** to schedule or mark as completed

### Step 3: Rourke Charts (Pediatric)

**What is a Rourke Chart?**
- Evidence-based pediatric preventive care guideline
- Age-specific checklists (birth to 5 years)
- Covers: growth, development, immunizations, anticipatory guidance
- Multiple versions: 2006, 2009, 2017, 2020

**Workflow:**

**Navigation:** Forms → Rourke Chart or Pediatric Forms

1. **Open Pediatric Patient e-Chart**
2. **Click "Forms" → "Rourke Chart"**
3. **Select Version**: `Rourke 2020` (most current)
4. **Select Age Visit**:
   - 1 week
   - 2 months
   - 4 months
   - 6 months
   - 9 months
   - 12 months
   - 18 months
   - 2 years
   - 3 years
   - 4 years
   - 5 years

5. **Complete Form Sections - Example: 2 Month Visit**

   **Section 1: Nutrition**
   - ☑ Breastfeeding exclusively or formula feeding?
   - ☑ Vitamin D supplementation discussed
   - ☑ Introduction to solids timing discussed (4-6 months)

   **Section 2: Education & Advice**
   - ☑ Safe sleep practices (back to sleep, no pillows)
   - ☑ Car seat safety (rear-facing until age 2)
   - ☑ Crying and soothing techniques
   - ☑ Immunization schedule explained

   **Section 3: Physical Examination**
   - Weight: `5.2 kg` (plot on growth chart)
   - Length: `58 cm` (plot on growth chart)
   - Head Circumference: `39 cm` (plot on growth chart)
   - Heart: Regular rate and rhythm, no murmur
   - Hips: Negative Barlow/Ortolani (no DDH)

   **Section 4: Developmental Screening**
   - ☑ Follows objects with eyes
   - ☑ Social smile present
   - ☑ Coos and makes sounds
   - ☑ Lifts head when on tummy

   **Section 5: Immunizations Due**
   - ☑ DTaP-IPV-Hib given
   - ☑ Pneumococcal given
   - ☑ Rotavirus given

6. **Click "Save Form"**
7. **Print Copy** for parent

💡 **Tip**: Use Rourke charts to ensure comprehensive well-child visits and improve quality metrics.

### Step 4: Sample Workflow - Record Flu Vaccination

**Complete Scenario:**

**Patient**: Mary Jones, age 68, coming for annual flu shot

**Workflow:**

1. **Patient Check-In**: Front desk marks appointment as "Here"
2. **Open e-Chart**: Search for Mary Jones, DOB 1957-03-22
3. **Verify Identity**: Check photo ID and health card
4. **Check Allergies**:
   - Review allergy list
   - Confirm no egg allergy (for flu vaccine)
   - Confirm no previous reaction to flu vaccine
5. **Obtain Consent**:
   - Explain risks and benefits
   - Patient verbally consents
   - Document in note
6. **Prepare Vaccine**:
   - Retrieve vaccine from fridge
   - Check expiry date: `2026-06-30` ✓
   - Record lot number: `FLU2025-67890`
   - Manufacturer: `Sanofi Pasteur`
7. **Administer Vaccine**:
   - Site: Right deltoid
   - Route: Intramuscular (IM)
   - Dose: 0.5 mL
   - No immediate reaction
8. **Document in EMR**:
   - **Navigate**: e-Chart → Preventions → Add Prevention
   - **Type**: Influenza
   - **Date**: 2025-12-11
   - **Lot Number**: FLU2025-67890
   - **Manufacturer**: Sanofi Pasteur
   - **Route**: IM
   - **Site**: Right Deltoid
   - **Dose**: 0.5 mL
   - **Expiry**: 2026-06-30
   - **Result**: Completed
   - **Comments**: `No immediate adverse reactions. Patient instructed to monitor for fever, soreness. Return if signs of anaphylaxis.`
   - **Next Due**: 2026-12-11
   - **Click "Save"**
9. **Post-Vaccination Instructions**:
   - Monitor patient for 15 minutes
   - Provide handout on expected side effects
   - Schedule next annual flu shot
10. **Bill Service** (if applicable):
    - BC Code: `00173` (Influenza immunization)
    - ON Code: `G372A` (Influenza immunization)
    - Dx Code: `V04.81` (Need for prophylactic vaccination)

⚠️ **Important**: Always observe patients for 15 minutes post-vaccination for allergic reactions.

---

## Quick Reference Tables

### Common Keyboard Shortcuts

| Shortcut | Action | Context |
|----------|--------|---------|
| **Ctrl + S** | Save current form/note | Forms, Notes |
| **Ctrl + P** | Print current page | All pages |
| **Ctrl + F** | Find/Search on page | All pages |
| **Alt + N** | New patient | Demographics |
| **Alt + S** | Search patient | Demographics |
| **Alt + A** | New appointment | Schedule |
| **Alt + E** | Open e-Chart | Patient selected |
| **F1** | Help documentation | All pages |
| **Esc** | Close popup/modal | Popups |

💡 **Tip**: Most shortcuts work browser-wide. EMR-specific shortcuts may vary by installation.

### Navigation Paths to Key Features

| Feature | Navigation Path |
|---------|-----------------|
| **Add Patient** | Home → Patients → Add Record |
| **Search Patient** | Home → Search (top menu) |
| **Book Appointment** | Schedule → Click time slot |
| **Open e-Chart** | Search patient → Click name → e-Chart |
| **Prescribe Medication** | e-Chart → Rx tab → Add Prescription |
| **Create SOAP Note** | e-Chart → Encounter → New Note |
| **Bill Service** | e-Chart → Billing tab → Create Invoice |
| **Upload Document** | e-Chart → Documents → Upload |
| **Record Immunization** | e-Chart → Preventions → Add Prevention |
| **Run Report** | Reports → Select report type |
| **Admin Settings** | Admin → Select configuration area |

### Province-Specific Settings

**British Columbia (BC) Configuration:**

| Setting | Value | Location |
|---------|-------|----------|
| **Billing Type** | MSP (Medical Services Plan) | Admin → Billing Settings |
| **HIN Format** | 10 digits + 2-digit version | Demographics form |
| **Service Code Prefix** | 5-digit numeric | Billing forms |
| **Submission Method** | Teleplan (www.teleplan.net) | Billing module |
| **Fee Schedule** | BC MSP Fee Schedule | Admin → Fee Schedule |

**Ontario (ON) Configuration:**

| Setting | Value | Location |
|---------|-------|----------|
| **Billing Type** | OHIP (Ontario Health Insurance Plan) | Admin → Billing Settings |
| **HIN Format** | 10 digits + 2 letters | Demographics form |
| **Service Code Prefix** | Alphanumeric (e.g., A001A) | Billing forms |
| **Submission Method** | EDT (Electronic Data Transfer) | Billing module |
| **Fee Schedule** | OHIP Schedule of Benefits | Admin → Fee Schedule |

### Common Service Codes Quick Reference

**British Columbia (MSP):**
- `00100` - Office Visit 5-15 min ($31.62)
- `00110` - Office Visit 15+ min ($38.08)
- `03333` - Annual Health Exam ($76.00)
- `00173` - Influenza immunization ($14.10)
- `13260` - Minor surgery ($61.40)

**Ontario (OHIP):**
- `A001A` - Minor Assessment under 5 min ($33.70)
- `A003A` - General Assessment 5-14 min ($66.65)
- `A005A` - Intermediate Assessment 15-25 min ($77.20)
- `A007A` - Complete Assessment 25-45 min ($102.15)
- `G372A` - Influenza immunization ($12.10)

### Troubleshooting Tips

| Problem | Possible Cause | Solution |
|---------|----------------|----------|
| **Can't log in** | Wrong credentials | Verify username/password with admin |
| **HIN validation fails** | Wrong province format | Check province matches HIN format |
| **Appointment won't save** | Time slot conflict | Choose different time or override |
| **Can't find patient** | Misspelled name | Try DOB or HIN search instead |
| **Prescription interaction warning** | Drug-drug interaction | Review warning, consider alternative |
| **Billing rejected** | Invalid HIN or code | Verify HIN valid, check service code |
| **Document won't upload** | File too large | Compress PDF or reduce image size |
| **Lab not linking** | Wrong patient details | Manually search and link |
| **e-Chart slow to load** | Large patient chart | Be patient, optimize browser cache |
| **Form won't print** | Popup blocker | Disable popup blocker for EMR site |

---

## Sample Datasets

### Patient 1: Adult Male with Hypertension (BC)

**Demographics:**
```
Last Name: Thompson
First Name: Robert
Gender: Male
Date of Birth: 1965-08-22 (Age 60)
Chart Number: T12345

Health Insurance:
Province: BC
PHN: 9123456789
Version Code: 66

Contact Information:
Residential Address: 1234 Oak Street
City: Vancouver
Province: BC
Postal Code: V6H 3M5
Home Phone: 604-555-1111
Cell Phone: 604-555-2222
Email: robert.thompson@email.com

Emergency Contact: Sarah Thompson (Wife)
Emergency Phone: 604-555-2222
```

**Medical History:**
- **Active Issues**:
  - Essential Hypertension (ICD-9: 401.9) - Since 2010
  - Hyperlipidemia (ICD-9: 272.0) - Since 2015
  - Type 2 Diabetes Mellitus (ICD-9: 250.00) - Since 2018

**Current Medications:**
1. Ramipril 10mg tablets
   - Sig: Take 1 tablet by mouth once daily
   - Quantity: 30 tablets
   - Repeats: 5
   - Indication: Hypertension

2. Rosuvastatin 20mg tablets
   - Sig: Take 1 tablet by mouth at bedtime
   - Quantity: 30 tablets
   - Repeats: 5
   - Indication: Hyperlipidemia

3. Metformin 500mg tablets
   - Sig: Take 1 tablet by mouth twice daily with meals
   - Quantity: 60 tablets
   - Repeats: 5
   - Indication: Type 2 Diabetes

**Allergies:**
- Penicillin - Rash (Moderate severity)
- ASA (Aspirin) - GI upset (Mild severity)

**Recent Appointments:**
- 2025-12-11: Follow-up for HTN/DM management
  - BP: 132/78 mmHg
  - Service Code: 00110 (Office Visit 15+ min)
  - Billed: $38.08

**Recent Labs (2025-11-15):**
- HbA1c: 6.8% (Target <7%)
- Fasting Glucose: 6.2 mmol/L
- Creatinine: 85 µmol/L (Normal)
- eGFR: 78 mL/min/1.73m² (Mildly reduced)
- Total Cholesterol: 4.2 mmol/L
- LDL: 2.1 mmol/L (Target <2.0)
- HDL: 1.3 mmol/L
- Triglycerides: 1.5 mmol/L

**Preventions:**
- Influenza vaccine: 2025-10-15 (Annual)
- Pneumococcal vaccine: Not yet due (age <65)
- Td booster: 2020-03-10 (Next due: 2030)

**Sample Clinical Note (2025-12-11):**
```
S: 60yo M here for 3-month f/u HTN, DM, HLD. Generally feeling well.
   Checking BG qAM, range 5.5-7.0. Diet improved, walking 30 min/day.
   No chest pain, SOB, or visual changes.

O: BP 132/78, HR 68, Wt 92kg (down 2kg from last visit).
   Cardiovascular: RRR, no murmur.
   Lungs: Clear bilaterally.
   Feet: No ulcers, pulses palpable.

A: 1. Type 2 DM - Good control, HbA1c 6.8%
   2. HTN - At target, continue current meds
   3. HLD - LDL slightly above target

P: 1. Continue current medications as prescribed
   2. Recheck HbA1c, lipids, Cr in 3 months
   3. Referral to dietitian for further dietary counseling
   4. Return in 3 months or PRN

Counseled on diet, exercise, medication adherence. Patient agrees with plan.
```

---

### Patient 2: Adult Female with Diabetes (ON)

**Demographics:**
```
Last Name: Patel
First Name: Priya
Gender: Female
Date of Birth: 1978-11-05 (Age 47)
Chart Number: P67890

Health Insurance:
Province: ON
Health Number: 6543210987AB

Contact Information:
Residential Address: 567 Maple Avenue, Apt 12
City: Toronto
Province: ON
Postal Code: M4S 2N8
Home Phone: 416-555-3333
Cell Phone: 416-555-4444
Email: priya.patel@email.com

Emergency Contact: Raj Patel (Husband)
Emergency Phone: 416-555-5555
```

**Medical History:**
- **Active Issues**:
  - Type 2 Diabetes Mellitus (ICD-9: 250.00) - Since 2016
  - Obesity (ICD-9: 278.00) - BMI 32
  - Anxiety Disorder (ICD-9: 300.00) - Since 2019
  - Hypothyroidism (ICD-9: 244.9) - Since 2020

**Current Medications:**
1. Metformin 1000mg tablets
   - Sig: Take 1 tablet by mouth twice daily with breakfast and dinner
   - Quantity: 60 tablets
   - Repeats: 5
   - Indication: Type 2 Diabetes

2. Levothyroxine 75mcg tablets
   - Sig: Take 1 tablet by mouth once daily on empty stomach
   - Quantity: 30 tablets
   - Repeats: 5
   - Indication: Hypothyroidism

3. Escitalopram 10mg tablets
   - Sig: Take 1 tablet by mouth once daily
   - Quantity: 30 tablets
   - Repeats: 5
   - Indication: Anxiety

**Allergies:**
- Sulfa drugs - Severe rash (Severe severity)
- Shellfish - Anaphylaxis (Severe severity)

**Recent Appointments:**
- 2025-12-11: Diabetes management and routine follow-up
  - Weight: 82kg, BMI: 32
  - Service Code: A005A (Intermediate Assessment 15-25 min)
  - Billed: $77.20

**Recent Labs (2025-11-20):**
- HbA1c: 7.8% (Above target, was 8.2% 3 months ago - improving)
- Fasting Glucose: 8.5 mmol/L
- TSH: 2.1 mIU/L (Normal, on levothyroxine)
- Free T4: 15 pmol/L (Normal)
- Creatinine: 68 µmol/L (Normal)
- eGFR: >90 mL/min/1.73m² (Normal)
- Urine microalbumin: Negative (Good - no diabetic nephropathy)

**Preventions:**
- Influenza vaccine: 2025-11-01 (Annual)
- COVID-19 vaccine: 2025-09-15 (Bivalent booster)
- Td booster: 2018-06-12 (Next due: 2028)
- Pap smear: 2024-03-10 (Next due: 2027)
- Mammography: 2024-09-22 (Next due: 2026)

**Sample Clinical Note (2025-12-11):**
```
S: 47yo F presenting for diabetes f/u. Reports improved BG control since
   increasing metformin. Checking BG 2x/day, range 7-10 mmol/L.
   Anxiety well-controlled on escitalopram, no panic attacks.
   Thyroid: No palpitations, energy level good. Attempting weight loss
   with exercise 3x/week.

O: BP 124/76, HR 72, Wt 82kg (BMI 32, down 3kg since last visit).
   Thyroid: Non-tender, no nodules.
   Cardiovascular: RRR, no murmur.
   Feet: Intact sensation, no lesions.

A: 1. Type 2 DM - Improving control (HbA1c down from 8.2% to 7.8%)
   2. Hypothyroidism - Well controlled on levothyroxine
   3. Anxiety - Stable on escitalopram
   4. Obesity - Gradual weight loss in progress

P: 1. Continue metformin 1000mg BID
   2. Continue levothyroxine 75mcg daily
   3. Continue escitalopram 10mg daily
   4. Recheck HbA1c, TSH in 3 months
   5. Consider referral to diabetes education program
   6. Encouraged re: weight loss progress, continue diet/exercise
   7. Return in 3 months

Patient motivated and adherent. Good progress overall.
```

---

### Patient 3: Pediatric Patient for Immunizations (Generic)

**Demographics:**
```
Last Name: Martinez
First Name: Sofia
Gender: Female
Date of Birth: 2025-06-15 (Age 6 months)
Chart Number: M11223

Health Insurance:
Province: Generic (or BC/ON - adapt as needed)
PHN: 8765432109 (BC format)
Version Code: 66

Contact Information:
Parent/Guardian: Maria Martinez (Mother)
Residential Address: 890 Pine Road
City: Anytown
Province: AB
Postal Code: T5K 2P7
Home Phone: 780-555-6666
Parent Cell: 780-555-7777
Email: maria.martinez@email.com

Emergency Contact: Carlos Martinez (Father)
Emergency Phone: 780-555-8888
```

**Medical History:**
- **Birth History**:
  - Full-term delivery at 39 weeks
  - Birth weight: 3.4 kg
  - Uncomplicated vaginal delivery
  - No NICU admission

- **Active Issues**:
  - None (healthy infant)

- **Feeding**:
  - Breastfeeding exclusively
  - Started vitamin D supplementation (400 IU daily)

**Current Medications:**
- Vitamin D drops 400 IU daily (over-the-counter)

**Allergies:**
- No known drug allergies (NKDA)
- No known food allergies

**Recent Appointments:**
- 2025-12-15: 6-month well-child visit and immunizations

**Growth Parameters (2025-12-15):**
- Weight: 7.8 kg (50th percentile)
- Length: 67 cm (60th percentile)
- Head Circumference: 43 cm (55th percentile)
- WHO Growth Chart: Normal growth trajectory

**Developmental Milestones (6 months):**
- ✓ Sits with support
- ✓ Rolls both ways
- ✓ Reaches for objects
- ✓ Babbles and makes sounds
- ✓ Recognizes familiar faces
- ✓ Brings hands to mouth

**Immunization Record:**

| Date | Age | Vaccines Given | Lot Number | Site | Adverse Events |
|------|-----|----------------|------------|------|----------------|
| 2025-08-15 | 2 mo | DTaP-IPV-Hib | DTAP202508A | L Thigh | None |
| | | Pneumococcal-13 | PNEU202508B | R Thigh | None |
| | | Rotavirus (oral) | ROTA202508C | Oral | None |
| 2025-10-15 | 4 mo | DTaP-IPV-Hib | DTAP202510A | L Thigh | None |
| | | Pneumococcal-13 | PNEU202510B | R Thigh | None |
| | | Rotavirus (oral) | ROTA202510C | Oral | None |
| 2025-12-15 | 6 mo | DTaP-IPV-Hib | DTAP202512A | L Thigh | None |
| | | Pneumococcal-13 | PNEU202512B | R Thigh | None |
| | | Rotavirus (oral) | ROTA202512C | Oral | None |

**Next Immunizations Due:**
- 12 months: MMR, Pneumococcal-13, Meningococcal C
- 18 months: DTaP-IPV-Hib, Varicella

**Sample Clinical Note - Rourke Chart 6-Month Visit (2025-12-15):**
```
NUTRITION:
☑ Breastfeeding exclusively, considering introduction of solids next month
☑ Vitamin D 400 IU daily
☑ No cow's milk until 9-12 months discussed

EDUCATION & ADVICE:
☑ Safe sleep practices reviewed (back to sleep, crib safety)
☑ Car seat rear-facing until age 2
☑ Introduction of solids at 6 months - iron-fortified cereals, pureed foods
☑ Choking hazards discussed
☑ Injury prevention: baby-proofing home

PHYSICAL EXAMINATION:
General: Alert, interactive, well-appearing infant
Growth: Weight 7.8kg (50th %), Length 67cm (60th %), HC 43cm (55th %)
        Normal growth trajectory
Eyes: Red reflex present bilaterally, tracks objects
Ears: TMs clear, bilateral
Heart: RRR, no murmur
Lungs: Clear to auscultation bilaterally
Abdomen: Soft, non-tender, no masses
Hips: Negative Barlow/Ortolani, full ROM
Neuro: Age-appropriate tone and reflexes

DEVELOPMENTAL SCREENING:
☑ Sits with support
☑ Rolls over both ways
☑ Reaches for and grasps toys
☑ Transfers objects hand to hand
☑ Babbles ("ba-ba", "da-da")
☑ Responds to name
☑ Shows excitement when sees familiar people

IMMUNIZATIONS:
✓ DTaP-IPV-Hib given (Lot: DTAP202512A, Site: L Thigh)
✓ Pneumococcal-13 given (Lot: PNEU202512B, Site: R Thigh)
✓ Rotavirus given (Lot: ROTA202512C, Route: Oral)

Post-immunization: Infant tolerated well, no immediate reactions.
Parents instructed on expected side effects (fussiness, low-grade fever).
Advised to give acetaminophen if fever >38.5°C. Return if inconsolable crying,
high fever >40°C, or other concerns.

PLAN:
- Continue breastfeeding, start solid foods (iron-fortified cereal, pureed
  vegetables, fruits)
- Continue vitamin D supplementation
- Next well-child visit at 9 months
- Next immunizations at 12 months (MMR, Pneumococcal, Meningococcal C)

Parents had no questions. Infant developing appropriately.
```

---

## Best Practices

### 1. Data Entry Tips

**Be Consistent:**
- Use standard abbreviations (e.g., HTN for hypertension, DM for diabetes)
- Follow institutional naming conventions
- Use same date format throughout (YYYY-MM-DD recommended)

**Complete Required Fields:**
- Never skip mandatory fields (HIN, DOB, name)
- Fill optional fields when information available
- Update missing information at every visit

**Use Templates and Favorites:**
- Create templates for common visits (annual physicals, prenatal visits)
- Save frequently prescribed medications as favorites
- Use macros for repetitive text (e.g., normal physical exam)

**Document Thoroughly:**
- Record all clinical reasoning and decision-making
- Note patient counseling and consent
- Document medication adherence and side effects
- Include all relevant positive and negative findings

**Proofread Before Saving:**
- Review notes for spelling and grammar
- Verify medications doses and frequencies
- Check diagnostic codes match clinical note
- Confirm patient identity on all documents

💡 **Tip**: Use voice-to-text software (Dragon Medical, built-in dictation) to speed up documentation.

### 2. Security Considerations

**Protect Patient Privacy (PIPEDA/HIPAA Compliance):**
- Never share login credentials with colleagues
- Always log out when leaving workstation
- Lock screen if stepping away briefly (Windows: Win+L, Mac: Cmd+Ctrl+Q)
- Close patient charts when not actively viewing
- Never discuss patients in public areas

**Password Security:**
- Use strong passwords (12+ characters, mix of letters/numbers/symbols)
- Change passwords regularly (every 90 days recommended)
- Don't write passwords on sticky notes
- Use password manager if allowed by institution

**Access Control:**
- Only access charts for patients under your care
- Don't browse charts out of curiosity (audited and violates privacy law)
- Report unauthorized access attempts
- Review your access audit logs periodically

**Physical Security:**
- Keep computers in secure areas
- Don't leave printed materials unattended
- Shred printed PHI when no longer needed
- Use privacy screens on monitors in open areas

**Mobile Device Security:**
- Use device encryption
- Enable remote wipe capability
- Don't store patient data on personal devices
- Use VPN when accessing remotely

⚠️ **Important**: Privacy breaches can result in fines, job loss, and legal action. Take security seriously.

### 3. Workflow Optimization

**Morning Routine:**
1. Log in and check system messages
2. Review today's appointment schedule
3. Check inbox for lab results and documents
4. Acknowledge urgent labs/reports
5. Prepare charts for scheduled appointments

**During Clinic:**
1. Mark patients as "Here" when checked in
2. Document encounters in real-time or immediately after
3. Bill services same day as visit
4. Order follow-up labs/tests before patient leaves
5. Update problem list and medications at every visit

**End of Day:**
1. Complete all unsigned notes
2. Review and acknowledge all labs/documents
3. Return phone messages and patient portal messages
4. Check billing for unbilled services
5. Schedule follow-ups for next day

**Weekly Tasks:**
1. Review unmatched lab queue
2. Run billing reports and fix rejections
3. Check prevention reminders and outreach due
4. Review inbox for missed documents
5. Clean up old tasks/ticklers

**Monthly Tasks:**
1. Submit billing batch to province
2. Reconcile remittance reports
3. Review quality metrics and screening rates
4. Update medication lists for chronic patients
5. Archive/purge old documents per retention policy

💡 **Tip**: Set calendar reminders for recurring tasks to ensure nothing falls through the cracks.

### 4. Common Mistakes to Avoid

**❌ DON'T:**

1. **Skip Searching Before Adding Patient**
   - Creates duplicate records
   - Violates privacy regulations
   - Causes billing issues
   - Always search by DOB and HIN first

2. **Document in Wrong Patient Chart**
   - Serious privacy breach
   - Medical error risk
   - Always verify patient name and DOB at top of screen

3. **Ignore Drug Interaction Warnings**
   - Can harm patients
   - May be liable for adverse events
   - Always review warnings and document decision

4. **Bill Without Documentation**
   - Fraudulent billing
   - Can't defend in audit
   - Note must support service code billed

5. **Leave Notes Unsigned**
   - Notes not legally valid until signed
   - Can delay care if covering provider can't see
   - Sign notes same day as visit

6. **Delay Lab Review**
   - Critical results may be missed
   - Patient harm from delayed diagnosis
   - Check inbox multiple times daily

7. **Use Copy-Paste Excessively**
   - Creates inaccurate notes
   - Auditors flag repetitive notes
   - Update template for each patient

8. **Share Login Credentials**
   - Violates privacy law
   - Can't track who did what
   - Your responsibility for all actions under your login

9. **Ignore Software Updates**
   - Security vulnerabilities
   - Missing new features
   - Compatibility issues
   - Update EMR when prompted

10. **Forget to Back Up Data**
    - Risk of data loss
    - System handles automatic backups typically
    - Verify backup processes with IT

**✓ DO:**

1. **Double-Check Patient Identity**
   - Verify name, DOB, HIN before every action
   - Use two patient identifiers minimum
   - Check photo if available

2. **Keep Problem List Updated**
   - Add new diagnoses as identified
   - Resolve issues when no longer active
   - Improves continuity of care

3. **Reconcile Medications Regularly**
   - Ask about adherence at every visit
   - Update discontinued medications
   - Check for duplicates or interactions

4. **Use Consistent Terminology**
   - Follow institutional standards
   - Use medical abbreviations correctly
   - Avoid ambiguous terms

5. **Document Patient Education**
   - Record what was explained
   - Note patient understanding
   - Provide written materials

6. **Track Follow-Up**
   - Use ticklers for pending results
   - Schedule return visits before patient leaves
   - Set reminders for overdue patients

7. **Review Notes Before Signing**
   - Catch errors and typos
   - Ensure completeness
   - Verify accuracy

8. **Stay Current with Training**
   - Attend EMR training sessions
   - Learn new features
   - Ask questions when unsure

9. **Communicate with Team**
   - Use internal messaging for non-urgent issues
   - Document verbal orders in EMR
   - Share workflow improvements

10. **Audit Your Own Work**
    - Review your billing periodically
    - Check for patterns in documentation
    - Identify areas for improvement

### 5. Getting Help

**Technical Support:**
- **IT Help Desk**: Contact for login issues, system errors, hardware problems
- **Email**: support@youremr.com (example)
- **Phone**: 1-800-EMR-HELP (example)
- **Hours**: Monday-Friday 8am-6pm, On-call after hours

**Clinical Support:**
- **Physician Champion**: Colleague experienced with EMR for clinical workflow questions
- **Practice Manager**: Administrative and billing questions
- **Privacy Officer**: Privacy and security concerns

**Training Resources:**
- **Online Tutorials**: Built-in help system (F1 key)
- **Video Library**: Screen recordings of common tasks
- **User Manuals**: Comprehensive documentation
- **Live Training**: Scheduled group or one-on-one sessions

**Community Resources:**
- **User Forums**: Connect with other OpenO EMR users
- **GitHub Repository**: https://github.com/open-osp/Open-O
- **OpenOSP Website**: https://openosp.ca
- **Contact OpenOSP**: https://openosp.ca/contact

💡 **Tip**: Keep a notebook of frequently used codes, shortcuts, and workflow tips for quick reference.

---

## Conclusion

**Congratulations!** You've completed the OpenO EMR Crash Course. You should now be able to:

✓ Navigate the OpenO EMR interface
✓ Manage patient demographics and search for patients
✓ Schedule and manage appointments
✓ Document clinical encounters using SOAP format
✓ Prescribe medications and check for interactions
✓ Create and submit billing claims
✓ Upload documents and manage lab results
✓ Record immunizations and preventive care

### Next Steps:

1. **Practice with Test Patients**: Use the sample datasets to practice workflows
2. **Explore Advanced Features**: Reporting, e-forms, consults, referrals
3. **Customize Your Workflow**: Create templates, favorites, and shortcuts
4. **Stay Updated**: Attend training sessions and review release notes
5. **Provide Feedback**: Share workflow improvements with your team

### Additional Training Modules (Not Covered):

- **E-Forms**: Custom electronic forms
- **Consultation Module**: Referral management
- **Reporting**: Clinical and administrative reports
- **Program Management**: Multi-program clinics (CAISI)
- **Advanced Billing**: Complex billing scenarios
- **HL7 Integration**: Lab and hospital interfaces
- **FHIR API**: Third-party integrations
- **Admin Functions**: System configuration

**Remember**: This guide covers core functionality. Every clinic customizes OpenO EMR differently. Consult your local policies and procedures for institution-specific workflows.

---

**Document Version**: 1.0
**Last Updated**: 2025-12-11
**Author**: OpenO EMR Training Team
**License**: GPL v2 (same as OpenO EMR software)

**Feedback**: Please report errors or suggestions to improve this guide.

---

*OpenO EMR is open-source healthcare software licensed under GPL v2. OSCAR is a registered trademark of McMaster University. This guide has no affiliation with McMaster University.*
