# Fix TablePlus "System Error 60" Timeout

## What's Happening

Error: `Lost connection to server at 'handshake: reading initial communication packet', system error: 60`

- **System Error 60** = Connection timeout (macOS)
- SSH tunnel connects successfully ✅
- MySQL handshake times out before completing ❌

## Solution 1: Increase TablePlus Timeout (Recommended)

TablePlus has a default connection timeout that's too short for SSH tunneling through Docker.

### Steps:
1. In TablePlus, click **"Other options:"** dropdown (in Connection tab)
2. Look for **Connection Timeout** setting
3. Increase from default (usually 10s) to **60 seconds**
4. Click **Test** again

## Solution 2: Check Remote Login is Enabled

The SSH tunnel might be slow because Remote Login isn't properly configured.

### Verify Remote Login:
1. **System Settings** → **General** → **Sharing**
2. Make sure **"Remote Login"** is **ON** (green)
3. Under "Allow full disk access for remote users" - make sure your user (AhmedE) is listed

### Test SSH works:
Open Mac Terminal and run:
```bash
ssh AhmedE@127.0.0.1
```

You should connect immediately with your Mac password. If it's slow or fails, Remote Login isn't configured properly.

## Solution 3: Use Direct Connection (Alternative)

If SSH tunnel is too slow, try connecting directly to the container IP:

### Get Container IP:
```bash
docker inspect openo-mariadb-dev | grep '"IPAddress"'
```

### In TablePlus:
1. **Disable "Over SSH"** (button should be gray)
2. Use the container IP as Host:
   ```
   Host: 172.22.0.2  (or whatever IP from command above)
   Port: 3306
   User: oscar_ext
   Password: oscar123
   Database: oscar
   ```

## Solution 4: Restart Database Container

The container might not be fully healthy yet. From Mac terminal:

```bash
cd /path/to/OpenO-project
./restart-database.sh
```

Wait for "✅ Database is healthy!" message, then try TablePlus again.

## Expected Behavior

Once working, TablePlus should:
- Connect in **less than 5 seconds**
- Show all 555 tables
- Let you browse 3,000 patient demographics

## Quick Test Query

Once connected, run:
```sql
SELECT VERSION();
-- Should return: 10.5.29-MariaDB-ubu2004-log

SELECT COUNT(*) FROM demographic;
-- Should return: 3000
```

## Still Having Issues?

The most reliable method is using the database from VS Code terminal:
```bash
# In VS Code terminal
db-connect
```

Then you have full SQL access without networking issues.
