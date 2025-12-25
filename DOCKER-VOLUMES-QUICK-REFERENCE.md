# Docker Volumes Quick Reference

## Your OpenO EMR Setup

Based on your [docker-compose.yml](.devcontainer/docker-compose.yml:54-59):

```
┌─────────────────────────────────────────────────────────────┐
│                   OPENO EMR DOCKER SETUP                     │
└─────────────────────────────────────────────────────────────┘

Container: openo-tomcat-dev
├─ Image: Built from .devcontainer/development/Dockerfile
├─ Volumes:
│  ├─ openo-emr_m2-volume → /root/.m2 (Maven cache)
│  ├─ ../workspace → /workspace (bind mount - your code)
│  └─ db_data/ → /db-data (bind mount - seed data)
└─ Purpose: Tomcat server running OpenO EMR

Container: openo-mariadb-dev
├─ Image: Built from .devcontainer/db/Dockerfile
├─ Volumes:
│  └─ openo-emr_mariadb-files → /var/lib/mysql ⭐ 11GB
└─ Purpose: MariaDB database
```

---

## Essential Commands (Run on Your Laptop)

### 1. Find All Volumes

```bash
docker volume ls
```

**Expected Output:**
```
DRIVER    VOLUME NAME
local     openo-emr_mariadb-files    ← This is your 11GB database!
local     openo-emr_m2-volume        ← Maven cache (optional to migrate)
```

### 2. See Which Container Uses Which Volume

```bash
docker ps --format 'table {{.Names}}\t{{.Mounts}}'
```

**Expected Output:**
```
NAMES                   MOUNTS
openo-mariadb-dev       openo-emr_mariadb-files
openo-tomcat-dev        openo-emr_m2-volume,../workspace
```

### 3. Get Detailed Volume Information

```bash
docker volume inspect openo-emr_mariadb-files
```

**Output Shows:**
- Mountpoint: Where it's stored on your laptop
- Size: Actual disk usage
- Created date

### 4. Check Volume Sizes

```bash
docker system df -v
```

**Shows all volumes with their sizes:**
```
VOLUME NAME                     SIZE
openo-emr_mariadb-files         11.2GB    ← Database
openo-emr_m2-volume             1.8GB     ← Maven cache
```

### 5. See Volume Usage for Running Containers

```bash
docker ps -a --format '{{.Names}}' | while read container; do
    echo "Container: $container"
    docker inspect $container --format '{{range .Mounts}}  {{.Type}}: {{.Source}} -> {{.Destination}}{{println}}{{end}}'
    echo ""
done
```

**Shows:**
- Volume mounts (named volumes)
- Bind mounts (directory mounts)
- Source and destination paths

### 6. Find Volumes by Container Name

```bash
# For MariaDB container
docker inspect openo-mariadb-dev --format '{{range .Mounts}}{{.Name}}{{println}}{{end}}'

# Output: openo-emr_mariadb-files
```

### 7. Check Volume Location on Disk

```bash
docker volume inspect openo-emr_mariadb-files --format '{{.Mountpoint}}'
```

**Example Output:**
```
/var/lib/docker/volumes/openo-emr_mariadb-files/_data
```

---

## Quick Identification by Container

### Method 1: Docker Compose (Recommended)

```bash
cd /path/to/openo-emr/.devcontainer
docker-compose config | grep -A 5 volumes:
```

**Shows volumes from your docker-compose.yml**

### Method 2: Inspect Running Container

```bash
# Step 1: List running containers
docker ps

# Step 2: Inspect specific container
docker inspect openo-mariadb-dev | grep -A 20 '"Mounts"'
```

### Method 3: By Image Name

```bash
# Find containers using a specific image
docker ps --filter ancestor=mariadb --format '{{.Names}}'

# Then inspect those containers
docker inspect <container_name> --format '{{range .Mounts}}{{.Name}}{{println}}{{end}}'
```

---

## Understanding Volume Types

### Named Volumes (What You Have)
```yaml
volumes:
  - mariadb-files:/var/lib/mysql
```
- **Name**: `openo-emr_mariadb-files` (project_volumename)
- **Managed by Docker**: Located in `/var/lib/docker/volumes/`
- **Persists**: Even when container is removed
- **For Migration**: Use export/import scripts ⭐

### Bind Mounts
```yaml
volumes:
  - ../workspace:/workspace:cached
```
- **Source**: Your laptop's filesystem (`../workspace`)
- **Not a Docker volume**: Just a directory mount
- **For Migration**: Copy directory normally (no Docker export needed)

---

## Find Volume Name Pattern

Docker Compose creates volumes with this naming pattern:
```
<project-name>_<volume-name>
```

**Your project:**
- Project name: `openo-emr` (from devcontainer.json:7)
- Volume names: `mariadb-files`, `m2-volume` (from docker-compose.yml:54-59)

**Full volume names:**
- `openo-emr_mariadb-files` ← Database (11GB)
- `openo-emr_m2-volume` ← Maven cache

---

## Specific Commands for Your Setup

### Export the Database Volume (11GB)

```bash
./export-docker-volume.sh openo-emr_mariadb-files mysql-backup.tar.gz
```

### Export Maven Cache (Optional - Saves Build Time)

```bash
./export-docker-volume.sh openo-emr_m2-volume maven-cache.tar.gz
```

### What NOT to Export

**Bind Mounts** (not Docker volumes):
- `../workspace` → Just copy your Git repository normally
- `db_data/` → Seed data, already in your repository

---

## Troubleshooting: Volume Not Found?

If you can't find `openo-emr_mariadb-files`:

1. **Check actual project name:**
   ```bash
   docker volume ls | grep mariadb
   ```

2. **Containers might be stopped:**
   ```bash
   docker ps -a  # Shows all containers (including stopped)
   ```

3. **Check without project prefix:**
   ```bash
   docker volume ls | grep mariadb
   # Might show: mariadb-files (without openo-emr_ prefix)
   ```

4. **Use Docker Compose to check:**
   ```bash
   cd .devcontainer
   docker-compose ps
   docker-compose config --volumes
   ```

---

## Complete Migration Checklist

**To Migrate from OLD to NEW Laptop:**

### OLD Laptop:
- [ ] `docker volume ls` → Confirm volume name
- [ ] `docker system df -v` → Verify it's ~11GB
- [ ] `./export-docker-volume.sh openo-emr_mariadb-files mysql-backup.tar.gz`
- [ ] Transfer `mysql-backup.tar.gz` to new laptop

### NEW Laptop:
- [ ] Clone OpenO EMR repository
- [ ] Copy export/import scripts
- [ ] `./import-docker-volume.sh openo-emr_mariadb-files mysql-backup.tar.gz`
- [ ] `cd .devcontainer && docker-compose up -d`
- [ ] Verify: Connect to database and check data

---

## Pro Tips

1. **Find volumes consuming most space:**
   ```bash
   docker system df -v | grep VOLUME -A 100 | sort -k3 -h
   ```

2. **Remove unused volumes (⚠️ CAREFUL!):**
   ```bash
   docker volume prune  # Only removes unused volumes
   ```

3. **Backup before cleanup:**
   ```bash
   # Always export important volumes before pruning
   ./export-docker-volume.sh openo-emr_mariadb-files backup-$(date +%Y%m%d).tar.gz
   ```

4. **Check if volume is in use:**
   ```bash
   docker ps -a --filter volume=openo-emr_mariadb-files
   ```

---

## Visual Summary

```
YOUR LAPTOP
│
├─ /var/lib/docker/volumes/
│  ├─ openo-emr_mariadb-files/_data/  ← 11GB DATABASE ⭐
│  └─ openo-emr_m2-volume/_data/      ← 1-2GB Maven cache
│
└─ /path/to/workspace/
   ├─ src/                             ← Git repository (not a volume)
   └─ .devcontainer/
      ├─ docker-compose.yml            ← Defines volume mappings
      └─ db/db_data/                   ← Seed data (not a volume)

CONTAINERS USE THESE VOLUMES:
openo-mariadb-dev  →  openo-emr_mariadb-files  (11GB)
openo-tomcat-dev   →  openo-emr_m2-volume      (1-2GB)
```

---

## Need More Help?

Run the inspection script:
```bash
./docker-volume-inspection-guide.sh
```

Or check the comprehensive migration guide:
```bash
cat DOCKER-VOLUME-MIGRATION-GUIDE.md
```
