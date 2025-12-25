# TablePlus SSH Tunnel Setup for Mac

## Why SSH Tunnel?

Docker Desktop for Mac has a networking limitation where MySQL protocol connections through port forwarding can fail, even when the port is reachable. SSH tunneling bypasses this issue completely.

## Prerequisites

1. **Enable Remote Login on your Mac:**
   - Go to **System Settings** → **General** → **Sharing**
   - Turn ON **Remote Login**
   - This allows SSH connections to your own Mac

## TablePlus Configuration

### Step 1: Create New Connection
Click **Create a new connection** → Choose **MariaDB**

### Step 2: Connection Tab
```
Name: OpenO EMR Database
Host: 127.0.0.1
Port: 3306
User: oscar_ext
Password: oscar123
Database: oscar
```

### Step 3: Enable SSH Tunnel
Click the **"Over SSH"** button at the bottom - it should turn **BLUE**

### Step 4: SSH Tab (appears after enabling Over SSH)
```
SSH Host: 127.0.0.1
SSH Port: 22
SSH User: AhmedE
SSH Password: [Your Mac login password]
```

**Important:** Use your actual Mac login password for SSH Password

### Step 5: SSL/TLS Tab (optional)
```
SSL Mode: Preferred
```

### Step 6: Test Connection
Click **Test** button at the bottom

You should see: ✅ **Connection successful**

## How It Works

```
TablePlus
    ↓
SSH to 127.0.0.1:22 (your Mac)
    ↓
Forward to 127.0.0.1:3306 (Docker port)
    ↓
Docker forwards to container:3306
    ↓
MariaDB database
```

The SSH tunnel creates a secure connection that Docker Desktop handles properly, unlike direct MySQL protocol connections.

## Troubleshooting

### "Connection refused" on SSH
- Remote Login is not enabled
- Go to System Settings → Sharing → Enable Remote Login

### "Authentication failed" on SSH
- Wrong Mac password
- Use your Mac login password, not oscar123

### Still can't connect
- Restart database container:
  ```bash
  docker compose -f .devcontainer/docker-compose.yml restart db
  ```
- Wait 30 seconds for health check
- Try again

## Verify It's Working

Once connected, run this query:
```sql
SELECT VERSION();
```

You should see: `10.5.29-MariaDB-ubu2004-log`

Then check your data:
```sql
SELECT COUNT(*) as total_patients FROM demographic;
SELECT COUNT(*) as total_appointments FROM appointment;
```

You should see:
- 3,000 patient demographics
- 11 appointments
