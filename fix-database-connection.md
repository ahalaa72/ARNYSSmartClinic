# Fix MariaDB Connection from Mac to Docker Container

## Problem
Docker Desktop for Mac uses a Linux VM which can cause MySQL protocol connection issues even when port 3306 is exposed and reachable via `nc`.

## What We've Verified ✅
- Port 3306 is exposed: `0.0.0.0:3306 -> container:3306`
- MariaDB is configured correctly (bind_address not set)
- User `oscar_ext@%` exists with correct privileges
- Connections work perfectly **inside** Docker network
- Configuration file updated with better timeouts

## Solution: Restart Database Container

The updated configuration needs the MariaDB container to be restarted. **Run these commands on your Mac terminal** (not inside VS Code):

### Step 1: Restart the Database Container
```bash
cd /path/to/your/project  # Navigate to your OpenO EMR project directory
docker compose -f .devcontainer/docker-compose.yml restart db
```

### Step 2: Wait for Health Check (30 seconds)
```bash
# Check when database is healthy
docker ps
# Look for openo-mariadb-dev with status "healthy"
```

### Step 3: Try TablePlus Connection
```
Host: 127.0.0.1
Port: 3306
User: oscar_ext
Password: oscar123
Database: oscar
SSL Mode: PREFERRED
Over SSH: DISABLED (gray button, not blue)
```

## Alternative Solution: Connect via Docker Internal Network

If the above doesn't work, Docker Desktop for Mac has networking quirks. Try this instead:

### Option A: Use host.docker.internal (Mac/Windows only)
```
Host: host.docker.internal
Port: 3306
User: oscar_ext
Password: oscar123
Database: oscar
```

### Option B: Find Container IP and Use It
```bash
# On your Mac terminal:
docker inspect openo-mariadb-dev | grep IPAddress
```
Then use that IP address (e.g., `172.22.0.2`) in TablePlus:
```
Host: 172.22.0.2  # Use the IP from above command
Port: 3306
User: oscar_ext
Password: oscar123
Database: oscar
```

## Option C: SSH Tunnel (Most Reliable)

### 1. Set up SSH access to your Mac:
```bash
# On your Mac, enable Remote Login:
System Settings → General → Sharing → Remote Login → ON
```

### 2. In TablePlus, configure:
```
Connection Settings:
  Host: 127.0.0.1
  Port: 3306
  User: oscar_ext
  Password: oscar123
  Database: oscar

SSH Settings (Over SSH button should be BLUE/enabled):
  SSH Host: 127.0.0.1
  SSH Port: 22
  SSH User: AhmedE  # Your Mac username
  SSH Password: [your Mac login password]
```

This creates an SSH tunnel: `Mac → SSH → Mac → Docker → MariaDB`

## Verify It Works

Once connected, run this query to verify:
```sql
SELECT VERSION();
SELECT COUNT(*) FROM demographic;
```

You should see:
- MariaDB version 10.5.29
- Number of patient records in database

## If Still Not Working

The issue is Docker Desktop for Mac's networking layer. The most reliable fallback is to use the database from inside VS Code:

```bash
# In VS Code terminal (inside container):
db-connect

# Then you can run any SQL queries:
USE oscar;
SELECT * FROM demographic LIMIT 10;
```

While not a GUI, this gives you full database access for queries and exploration.
