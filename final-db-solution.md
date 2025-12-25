# Database Access - Final Solution

## The Problem
Docker Desktop for Mac has networking issues that prevent TablePlus/DBeaver from connecting to MariaDB containers, regardless of method tried:
- ❌ Direct localhost connection (127.0.0.1:3306) - times out
- ❌ SSH tunnel - times out
- ❌ Container IP (172.22.0.2) - not accessible from Mac

This is a known Docker Desktop for Mac limitation.

## Working Solutions

### Solution 1: Use VS Code Terminal (Recommended - Always Works)

**From VS Code terminal (inside the DevContainer):**
```bash
db-connect
```

This gives you direct MySQL command-line access. You can run any SQL queries:

```sql
-- See all tables
SHOW TABLES;

-- Query patient data
SELECT * FROM demographic LIMIT 10;

-- Search by name
SELECT demographic_no, first_name, last_name, chart_no
FROM demographic
WHERE first_name LIKE '%John%';

-- Count records
SELECT COUNT(*) FROM demographic;
```

**Export query results to file:**
```bash
mysql -h db -u root -ppassword oscar -e "SELECT * FROM demographic;" > /workspace/demographics.csv
```

Then open `/workspace/demographics.csv` in VS Code or Excel.

---

### Solution 2: Port Forward from Container to Mac (Alternative)

If you absolutely need a GUI tool, create a port forwarding SSH tunnel:

**Step 1: From Mac Terminal**
```bash
# Forward Mac's port 3307 to container's port 3306 via SSH into the DevContainer
# This creates: Mac:3307 -> SSH -> Container -> DB:3306

ssh -L 3307:db:3306 -N root@localhost -p [devcontainer-ssh-port]
```

**Step 2: In TablePlus**
```
Host: 127.0.0.1
Port: 3307           ← Note: 3307, not 3306
User: oscar_ext
Password: oscar123
Database: oscar
Over SSH: DISABLED
```

(Note: This requires setting up SSH access to the DevContainer first)

---

### Solution 3: Use MySQL Workbench Instead of TablePlus

Some users report MySQL Workbench handles Docker Desktop networking better than other tools.

**Download:** https://dev.mysql.com/downloads/workbench/

**Settings:**
```
Connection Method: Standard (TCP/IP)
Hostname: 127.0.0.1
Port: 3306
Username: oscar_ext
Password: oscar123
Default Schema: oscar
```

---

### Solution 4: Use Web-Based Database Tool (phpMyAdmin)

Add phpMyAdmin to your docker-compose.yml to access the database via web browser.

---

## What You Can Do Right Now

**Option A: Use db-connect (5 seconds to start)**
```bash
# In VS Code terminal
db-connect

# Then run queries
USE oscar;
SELECT COUNT(*) FROM demographic;
```

**Option B: Export Data for GUI Analysis**
```bash
# Export entire tables to CSV
mysql -h db -u root -ppassword oscar -e "SELECT * FROM demographic;" > demographic.csv
mysql -h db -u root -ppassword oscar -e "SELECT * FROM appointment;" > appointments.csv

# Open CSVs in Excel, VS Code, or any spreadsheet tool
```

**Option C: Use SQLTools Extension in VS Code**

1. Install "SQLTools" extension in VS Code
2. Add connection:
   - Driver: MySQL
   - Server: db
   - Port: 3306
   - Database: oscar
   - Username: oscar_ext
   - Password: oscar123
3. Browse database visually within VS Code

---

## Why External GUI Tools Don't Work

Docker Desktop for Mac uses a Linux VM with complex networking:
- Your Mac → Docker Desktop VM → Container network → MariaDB
- MySQL protocol handshake fails somewhere in this chain
- Port is reachable (nc test works) but MySQL protocol times out
- This is a known issue with Docker Desktop's networking layer

The `db-connect` command works because it runs **inside** the container network, avoiding all the Mac → Docker translation layers.

---

## Recommendation

**For Phase 1 exploration**, use `db-connect` from VS Code terminal. It's:
- ✅ Instant (already configured)
- ✅ Full SQL capabilities
- ✅ Can export results to CSV
- ✅ No networking issues

**For production deployment**, you'll have a real server where TablePlus/DBeaver will work normally without Docker Desktop's quirks.
