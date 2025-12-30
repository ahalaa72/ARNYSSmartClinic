# Patient Self-Registration Module - Quick Start Guide

This guide provides the fastest path to getting the Patient Self-Registration Module running.

---

## Prerequisites Checklist

- [ ] OpenO EMR installed and running
- [ ] Database access (root or admin)
- [ ] SMTP server configured for emails
- [ ] Staff account with admin privileges

---

## 5-Minute Installation

### 1. Run Database Migration (1 min)

```bash
mysql -u root -p oscar < database/mysql/updates/update-2025-12-29-patient-registration-queue.sql
```

### 2. Configure Clinic Settings (2 min)

```sql
-- Update with your clinic details
UPDATE registration_module_config SET
  clinic_name = 'Your Clinic Name',
  clinic_address = 'Your Address',
  clinic_phone = '555-555-5555',
  email_from = 'noreply@yourclinic.com',
  email_from_name = 'Your Clinic'
WHERE facility_id = 1;
```

### 3. Add Security Privileges (1 min)

```sql
-- Allow admins and receptionists to use registration
INSERT INTO secObjPrivilege (secObjName, roleUserGroup, privilege, priority)
VALUES
  ('_registration', 'admin', 'rw', 0),
  ('_registration', 'receptionist', 'rw', 0);
```

### 4. Restart Application (1 min)

```bash
server restart
```

---

## First-Time Setup

### Access the Admin Interface

1. Log into OpenO EMR as admin
2. Go to: `https://your-server/oscar/admin/registration/queue.jsp`

### Generate Your QR Code

1. Go to: `https://your-server/oscar/admin/registration/qrcode.jsp`
2. Click **Print Poster**
3. Display in your waiting area

---

## Test the Flow

1. **Scan QR Code** with your phone
2. **Fill out the test form** (use fake data)
3. **Submit** the registration
4. **Log into admin** and see it in the queue
5. **Approve** the test registration
6. **Verify** patient record was created

---

## URLs Reference

| Purpose | URL |
|---------|-----|
| Staff Queue | `/oscar/admin/registration/queue.jsp` |
| QR Code Page | `/oscar/admin/registration/qrcode.jsp` |
| Patient Form | `/oscar/registration/form.jsp?token=...` |
| REST API | `/oscar/ws/rs/registration/*` |

---

## Common Adjustments

### Change Token Expiry Time

Edit `registration-module.properties`:
```properties
registration.token.expiry.minutes=60
```

### Increase Rate Limits

For high-traffic clinics:
```properties
registration.rate.limit.max.sessions.per.ip=20
registration.rate.limit.max.submissions.per.ip=10
```

### Disable Email Notifications

```properties
registration.email.enabled=false
```

---

## Need Help?

See the full [README.md](README.md) for detailed documentation.
