# Docker Volume Migration Guide (11GB Volume)

## Overview
This guide helps you migrate Docker volumes between laptops. For an 11GB volume, expect the transfer to take 10-30 minutes depending on compression and transfer method.

---

## Method 1: Docker Volume Export/Import (Recommended)

### Step 1: Identify Your Volume (OLD Laptop)

```bash
# List all volumes
docker volume ls

# For OpenO EMR, common volume names:
# - <project>_mysql_data (database data)
# - <project>_maven_cache (build cache)
# - <project>_tomcat_logs (application logs)

# Inspect a specific volume
docker volume inspect <volume_name>
```

### Step 2: Export Volume (OLD Laptop)

```bash
# Copy the export script to your old laptop
# Then run:
./export-docker-volume.sh <volume_name> mysql-backup.tar.gz

# Example for OpenO EMR database:
./export-docker-volume.sh openo_mysql_data openo-mysql-data.tar.gz

# For large volumes (11GB), this will take several minutes
# Progress will show in terminal
```

**What this does:**
- Creates a temporary Alpine Linux container
- Mounts your volume as read-only
- Compresses all data to a `.tar.gz` file
- Saves to your current directory

### Step 3: Transfer File to NEW Laptop

Choose one of these methods:

**Option A: External Drive/USB**
```bash
# Copy to USB drive
cp openo-mysql-data.tar.gz /media/usb-drive/

# On new laptop, copy from USB
cp /media/usb-drive/openo-mysql-data.tar.gz ~/
```

**Option B: Cloud Storage (Dropbox, Google Drive, OneDrive)**
```bash
# Upload the .tar.gz file via web interface or CLI
# Download on new laptop
```

**Option C: Direct Network Transfer (if both on same network)**
```bash
# On NEW laptop, start receiving:
nc -l 9999 > openo-mysql-data.tar.gz

# On OLD laptop, send file:
cat openo-mysql-data.tar.gz | nc <new-laptop-ip> 9999
```

**Option D: SCP (if SSH enabled)**
```bash
# From OLD laptop:
scp openo-mysql-data.tar.gz user@new-laptop-ip:~/
```

### Step 4: Import Volume (NEW Laptop)

```bash
# Copy the import script to your new laptop
# Then run:
./import-docker-volume.sh <volume_name> openo-mysql-data.tar.gz

# Example:
./import-docker-volume.sh openo_mysql_data openo-mysql-data.tar.gz
```

**What this does:**
- Creates the volume if it doesn't exist
- Extracts the compressed data into the volume
- Preserves all file permissions and structure

### Step 5: Verify (NEW Laptop)

```bash
# Check volume exists
docker volume inspect openo_mysql_data

# Test with a container
docker run --rm -v openo_mysql_data:/data alpine ls -lah /data
```

---

## Method 2: Manual Backup/Restore (Database-Specific)

If your 11GB volume is the **MySQL/MariaDB database**, you can use database dumps:

### Export (OLD Laptop)

```bash
# Start your database container
docker-compose up -d db

# Create database dump
docker exec <mysql-container-name> mysqldump \
    -u root -ppassword \
    --all-databases \
    --single-transaction \
    --quick \
    --lock-tables=false \
    > openo-database-dump.sql

# Compress it
gzip openo-database-dump.sql
# Creates: openo-database-dump.sql.gz
```

### Import (NEW Laptop)

```bash
# Start your database container
docker-compose up -d db

# Uncompress
gunzip openo-database-dump.sql.gz

# Import database
docker exec -i <mysql-container-name> mysql \
    -u root -ppassword \
    < openo-database-dump.sql
```

**Advantages:**
- Smaller file size (SQL dump compresses better)
- More portable between different MySQL versions
- Can verify/edit SQL before importing

**Disadvantages:**
- Slower for large databases
- Doesn't preserve exact binary data

---

## Method 3: Docker Compose Volume Backup Plugin

If using Docker Compose (which OpenO EMR does):

### Install Backup Plugin (OLD Laptop)

```bash
docker volume create --name temp_backup
```

### Backup (OLD Laptop)

```bash
docker run --rm \
    -v openo_mysql_data:/source:ro \
    -v temp_backup:/backup \
    alpine \
    tar czf /backup/mysql-data.tar.gz -C /source .

# Export the backup volume
docker run --rm \
    -v temp_backup:/backup \
    -v $(pwd):/output \
    alpine \
    cp /backup/mysql-data.tar.gz /output/
```

---

## Method 4: Full DevContainer Migration

If you want to migrate your **entire OpenO EMR development environment**:

### Export Entire Project (OLD Laptop)

```bash
# Stop all containers
docker-compose down

# Backup volumes
./export-docker-volume.sh openo_mysql_data mysql-data.tar.gz
./export-docker-volume.sh openo_maven_cache maven-cache.tar.gz

# Copy your project directory
tar czf openo-project.tar.gz /path/to/workspace/
```

### Import Project (NEW Laptop)

```bash
# Clone or copy project
git clone <repository-url>
cd openo-emr

# Restore volumes
./import-docker-volume.sh openo_mysql_data mysql-data.tar.gz
./import-docker-volume.sh openo_maven_cache maven-cache.tar.gz

# Rebuild containers
docker-compose up -d
```

---

## Performance Tips for 11GB Transfer

1. **Compression Level**: The scripts use default `gzip` compression. For faster transfer:
   ```bash
   # Modify export script to use less compression (faster):
   tar cf - -C /source . | gzip -1 > /backup/file.tar.gz

   # Or more compression (smaller file):
   tar cf - -C /source . | gzip -9 > /backup/file.tar.gz
   ```

2. **Parallel Compression** (if you have `pigz`):
   ```bash
   tar cf - -C /source . | pigz > /backup/file.tar.gz
   ```

3. **Split Large Files** (for cloud uploads with size limits):
   ```bash
   # Split into 2GB chunks
   split -b 2G mysql-data.tar.gz mysql-data.tar.gz.part

   # Reassemble on new laptop
   cat mysql-data.tar.gz.part* > mysql-data.tar.gz
   ```

4. **Expected Times** (11GB volume):
   - Export (compression): 5-15 minutes
   - Transfer (USB 3.0): 2-5 minutes
   - Transfer (WiFi): 10-30 minutes
   - Transfer (Cloud): 20-60 minutes (depends on upload speed)
   - Import (extraction): 5-10 minutes

---

## Troubleshooting

### "No space left on device"
```bash
# Check available space
df -h

# Clean up old Docker data
docker system prune -a
docker volume prune
```

### "Permission denied"
```bash
# Make scripts executable
chmod +x export-docker-volume.sh import-docker-volume.sh

# Run with sudo if needed
sudo ./export-docker-volume.sh <volume_name>
```

### "Volume not found"
```bash
# List all volumes
docker volume ls

# Check exact name (case-sensitive)
docker volume inspect <volume_name>
```

### Import seems stuck
```bash
# Monitor progress (open another terminal)
docker ps
docker stats
```

---

## OpenO EMR Specific Volumes

Based on the devcontainer configuration, you likely have:

1. **`<project>_mysql_data`** (~11GB) - Main database
   - **Priority**: HIGH - Contains all patient records
   - **Migration**: Use export/import script or database dump

2. **`<project>_maven_cache`** (~1-2GB) - Maven dependencies
   - **Priority**: LOW - Can be rebuilt automatically
   - **Migration**: Optional (saves rebuild time)

3. **`<project>_tomcat_logs`** (~100MB) - Application logs
   - **Priority**: LOW - Usually not needed on new machine
   - **Migration**: Skip unless debugging

**Recommendation**: Only migrate the `mysql_data` volume. Maven cache and logs can be regenerated.

---

## Quick Start Commands

**On OLD laptop:**
```bash
# 1. List volumes to find the exact name
docker volume ls

# 2. Export the database volume
./export-docker-volume.sh <your_mysql_volume_name> mysql-backup.tar.gz

# 3. Copy mysql-backup.tar.gz to new laptop
```

**On NEW laptop:**
```bash
# 1. Setup OpenO EMR project
git clone <repo-url>
cd openo-emr

# 2. Import the database volume
./import-docker-volume.sh <your_mysql_volume_name> mysql-backup.tar.gz

# 3. Start containers
docker-compose up -d
```

---

## Security Notes

- Database backups contain **PHI (Protected Health Information)**
- Encrypt the backup file if transferring over internet:
  ```bash
  # Encrypt
  gpg -c mysql-backup.tar.gz

  # Decrypt on new laptop
  gpg mysql-backup.tar.gz.gpg
  ```
- Delete backup files securely after migration:
  ```bash
  shred -vfz -n 10 mysql-backup.tar.gz
  ```
