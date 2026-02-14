---
title: OpenO EMR - DevContainer Architecture
tags:
  - devcontainer
  - docker
  - openo
  - development
  - java
  - healthcare
date: 2025-12-11
project: OpenO EMR
---

# OpenO EMR - DevContainer Architecture

## What is a DevContainer?

**DevContainer = Development Container**

A DevContainer is a fully-configured development environment running inside Docker containers that VS Code connects to directly. It provides:
- ✅ All development tools pre-installed (Java, Maven, Node.js, etc.)
- ✅ All dependencies and libraries
- ✅ Exact environment configuration
- ✅ Everything ready to code immediately

### Benefits for Our Team
1. **Consistency**: All 3 developers work in identical environments
2. **No "works on my machine" issues**: Same Docker = same results
3. **Quick onboarding**: New developers just open VS Code, everything auto-configures
4. **Isolated**: Doesn't interfere with host machine installations

---

## Two-Container Architecture

The OpenO EMR development environment uses **TWO Docker containers** working together:

### Container 1: `openo-tomcat-dev` (Application Server)

**Purpose**: Main development environment where OpenO EMR runs

**Includes:**
- ☕ **Java 21 (JDK)** - Modern LTS Java version
- 🐱 **Tomcat 9.0.97** - Web application server
- 📦 **Maven** - Build and dependency management
- 🔧 **Node.js** - Frontend tooling
- 🤖 **AI Tools**: Claude Code CLI, GitHub CLI, Aider, Gemini CLI
- 📝 **Development Tools**: Git, vim, nano, MariaDB client
- 📧 **Postfix** - Local SMTP server for testing emails
- 🐍 **Python 3** - For automation scripts
- 📚 **Documentation Tools**: Docusaurus, javadoc2md

**Exposed Ports:**
- **8080** → OpenO EMR web application (`http://localhost:8080/oscar`)
- **18000** → Java debugger port (for remote debugging)

**File Locations:**
- `/workspace` → Your source code (mounted from host)
- `/usr/local/tomcat/webapps/oscar/` → Deployed application
- `/root/.m2` → Maven repository (cached dependencies)
- `/scripts` → Custom helper scripts (make, server, db-connect, etc.)

---

### Container 2: `openo-mariadb-dev` (Database Server)

**Purpose**: Healthcare database storing all patient and clinical data

**Includes:**
- 🗄️ **MariaDB** (MySQL-compatible database)
- 📊 **Healthcare Schema**: 200+ tables for patient data, appointments, billing, etc.
- 📋 **Reference Data**: ICD-9/10 codes, SNOMED, drug databases, provincial billing codes

**Exposed Ports:**
- **3306** → MariaDB database port

**Resource Limits:**
- **Memory**: 2GB maximum
- **CPUs**: 2 cores

**Data Storage:**
- Volume `mariadb-files` → Persistent database files (survives container restarts)
- Database name: `oscar` (default)
- Root password: `password` (development only)

**Health Check:**
- Runs every 10 seconds to ensure database is ready
- Container 1 waits for Container 2 to be healthy before starting

---

## Container Communication Flow

```
┌────────────────────────────────────────────────────────────────┐
│                    HOST MACHINE (Developer PC)                  │
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │              VS Code IDE (on host)                        │ │
│  │              Connected to DevContainer via Remote         │ │
│  └─────────────────────────┬─────────────────────────────────┘ │
│                            │                                    │
│  ┌─────────────────────────▼─────────────────────────────────┐ │
│  │           DOCKER NETWORK: open-o-network                  │ │
│  │                                                            │ │
│  │  ┌────────────────────────┐    ┌──────────────────────┐  │ │
│  │  │  Container 1           │    │  Container 2         │  │ │
│  │  │  openo-tomcat-dev      │    │  openo-mariadb-dev   │  │ │
│  │  │  ───────────────────   │    │  ─────────────────   │  │ │
│  │  │  • Java 21 + Tomcat    │    │  • MariaDB Server    │  │ │
│  │  │  • OpenO EMR App       │◄───┼──• Patient Database  │  │ │
│  │  │  • Maven builds        │    │  • Appointments      │  │ │
│  │  │  • Your code at        │    │  • Clinical notes    │  │ │
│  │  │    /workspace          │    │  • Billing data      │  │ │
│  │  │  • AI tools            │    │  • Lab results       │  │ │
│  │  │                        │    │                      │  │ │
│  │  │  Connects to DB via:   │    │  Connection:         │  │ │
│  │  │  jdbc:mysql://db:3306  │    │  hostname: db        │  │ │
│  │  │                        │    │  port: 3306          │  │ │
│  │  │  Ports:                │    │                      │  │ │
│  │  │  • 8080 → Web App      │    │  Health Check:       │  │ │
│  │  │  • 18000 → Debugger    │    │  Every 10s ping      │  │ │
│  │  └────────────────────────┘    └──────────────────────┘  │ │
│  │                                                            │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                 │
│  Access from Host Browser:                                     │
│  → http://localhost:8080/oscar       (OpenO EMR)              │
│  → http://localhost:8080/drugref2    (Drug Reference)         │
│                                                                 │
│  Access from Database Tools (DBeaver, MySQL Workbench):       │
│  → Host: localhost, Port: 3306, User: root, Password: password│
│                                                                 │
└────────────────────────────────────────────────────────────────┘
```

---

## Persistent Data (Docker Volumes)

The setup uses **named volumes** to preserve data across container restarts:

### 1. `m2-volume` → Maven repository cache
- Stores downloaded JAR dependencies
- Prevents re-downloading 200+ libraries on every rebuild
- Location in container: `/root/.m2`

### 2. `mariadb-files` → Database storage
- Contains all MariaDB data files
- Persists patient records, appointments, notes
- Location in container: `/var/lib/mysql`

### 3. Workspace mount (bind mount)
- Maps `/workspace` in container to your project folder on host
- Code changes instantly visible in both environments
- Shared via `..:/workspace:cached` (cached mode for performance)

---

## Network Configuration

**Network Name**: `open-o-network` (Bridge network)

**Internal DNS**:
- Container 1 can reach Container 2 using hostname `db`
- Example connection string: `jdbc:mysql://db:3306/oscar`
- Both containers are isolated from other Docker networks

**External Access** (from host machine):
- `localhost:8080` → Web application (Container 1)
- `localhost:18000` → Java debugger (Container 1)
- `localhost:3306` → Database (Container 2)

---

## Team Access Strategies

### Option 1: Shared Server Setup (Recommended)
One developer runs the Docker containers on their machine (acts as server):

**Server Machine:**
```bash
# Start containers
docker compose up -d

# Check IP address
ip addr show  # Linux
ipconfig      # Windows
```

**Other Developers Connect:**
- **Web Access**: `http://<server-ip>:8080/oscar`
- **Database Access**: `<server-ip>:3306`
- **Code Sharing**: Via Git (push/pull)

**Firewall Rules Needed:**
- Allow port 8080 (HTTP)
- Allow port 3306 (MySQL)
- Allow port 18000 (Java debugger, optional)

---

### Option 2: Each Developer Runs Their Own
Every developer has:
- Docker Desktop installed
- VS Code with Remote-Containers extension
- Copy of the repository

**Workflow:**
1. Each dev opens project in VS Code
2. VS Code builds containers automatically
3. Each dev has isolated environment
4. Share code changes via Git

**Pros**: Complete isolation, no network dependencies
**Cons**: More resource usage (3x containers)

---

### Option 3: Dedicated Linux Server (Future)
Run containers on a shared development server:
- All developers SSH into the server
- Use VS Code Remote-SSH extension
- Shared database, shared code
- Closest to production environment

---

## Custom Scripts Available

The development container includes custom scripts in `/scripts/` directory:

### Build & Deploy
```bash
make clean                          # Clean project and remove deployed app
make install                        # Build and deploy without tests
make install --run-tests            # Build, test, and deploy (all tests)
make install --run-modern-tests     # Modern tests only (JUnit 5)
make install --run-legacy-tests     # Legacy tests only (JUnit 4)
```

### Server Management
```bash
server start                        # Start Tomcat server
server stop                         # Stop Tomcat server
server restart                      # Restart Tomcat server
server log                          # Tail application logs in real-time
```

### Database
```bash
db-connect                          # Connect to MariaDB as root user
```

### Logging
```bash
debug-on                            # Switch to DEBUG log level
debug-off                           # Switch back to INFO log level
```

### GitHub
```bash
gh pr create                        # Create pull request (GitHub CLI)
gh issue list                       # List issues
```

---

## What Happens When You Open VS Code?

**Step-by-step startup process:**

1. **VS Code detects** `.devcontainer/devcontainer.json` configuration file
2. **Prompts**: "Reopen in Container?" (click Yes)
3. **Docker Compose builds** both containers from scratch (first time only)
   - Builds `openo-mariadb-dev` (Container 2)
   - Builds `openo-tomcat-dev` (Container 1)
4. **Health check** waits for MariaDB to be ready (up to 60 seconds)
5. **Container 1 starts** and runs `postCreateCommand`:
   - Seeds database with test data
   - Downloads Maven dependencies offline
   - Downloads source code and javadocs
6. **VS Code connects** to Container 1's shell
7. **Extensions auto-install**: Java pack, Git Blame, Server Connector, Claude Code
8. **Terminal opens** inside container at `/workspace`
9. **Ready to code!**

**Subsequent openings** (much faster):
- Containers already built (cached)
- Just starts existing containers (~30 seconds)

---

## Environment Variables & Configuration

### Shared Configuration
`.devcontainer/development/config/shared/local.env`:
- Database credentials
- Timezone: America/Toronto
- Locale: en_US.UTF-8

### Java Configuration
- `CATALINA_OPTS`: JVM options for Tomcat
- `JPDA_OPTS`: Java Debug Wire Protocol settings
- Debug port: 8000 (mapped to 18000 on host)

### VS Code Settings (auto-configured)
- Java memory: 4GB max (-Xmx4G)
- Parallel garbage collection enabled
- Maven source downloads disabled (performance)
- Null analysis enabled

---

## Pre-installed AI Tools

The container includes several AI coding assistants:

1. **Claude Code CLI** (`@anthropic-ai/claude-code`)
   - Command: `claude` (already running via VS Code extension)

2. **GitHub Copilot CLI** (`gh copilot`)
   - Command: `gh copilot suggest`

3. **Aider** (`aider-chat`)
   - Command: `aider`
   - AI pair programming in terminal

4. **Gemini CLI** (`@google/gemini-cli`)
   - Command: `gemini`

---

## Troubleshooting Common Issues

### Container won't start
```bash
# Check container status
docker ps -a

# View logs
docker logs openo-tomcat-dev
docker logs openo-mariadb-dev

# Rebuild from scratch
docker compose down -v  # WARNING: Deletes volumes
docker compose up --build
```

### Database connection failed
```bash
# Check MariaDB is running
docker exec openo-mariadb-dev mysqladmin ping -h localhost -u root -ppassword

# Check health status
docker inspect openo-mariadb-dev | grep Health -A 10
```

### Port already in use
```bash
# Find what's using port 8080
lsof -i :8080  # Linux/Mac
netstat -ano | findstr :8080  # Windows

# Kill the process or change port in docker-compose.yml
```

### Out of disk space
```bash
# Clean up Docker
docker system prune -a --volumes
```

---

## Configuration Files Reference

### Key DevContainer Files
- `.devcontainer/devcontainer.json` - VS Code configuration
- `.devcontainer/docker-compose.yml` - Docker services definition
- `.devcontainer/development/Dockerfile` - Container 1 build instructions
- `.devcontainer/db/Dockerfile` - Container 2 build instructions

### Scripts
- `.devcontainer/development/scripts/` - Custom helper scripts
- `.devcontainer/development/config/` - Configuration files
- `.devcontainer/development/setup/` - Initialization scripts

---

## Related Notes

- [[OpenO EMR - Phase 1 Plan]]
- [[Docker Best Practices]]
- [[Java Development Setup]]
- [[Healthcare EMR Systems]]

---

*Last updated: 2025-12-11*
*Project: OpenO EMR Modernization*
