#!/bin/bash
# Test MariaDB connection from Mac
# Run this script on your Mac (not inside VS Code container)

echo "🔍 Testing MariaDB Connection Methods..."
echo "========================================"
echo ""

# Test 1: Port is reachable
echo "✅ Test 1: Port 3306 reachability"
if nc -zv 127.0.0.1 3306 2>&1 | grep -q succeeded; then
    echo "   ✓ Port 3306 is reachable"
else
    echo "   ✗ Port 3306 is NOT reachable"
    echo "   💡 Run: docker compose -f .devcontainer/docker-compose.yml ps"
    exit 1
fi
echo ""

# Test 2: Container is running
echo "✅ Test 2: Container status"
if docker ps | grep -q openo-mariadb-dev; then
    echo "   ✓ openo-mariadb-dev container is running"
    CONTAINER_IP=$(docker inspect openo-mariadb-dev | grep '"IPAddress"' | head -1 | awk -F'"' '{print $4}')
    echo "   📍 Container IP: $CONTAINER_IP"
else
    echo "   ✗ Container is NOT running"
    exit 1
fi
echo ""

# Test 3: MySQL protocol via localhost
echo "✅ Test 3: MySQL protocol test via localhost (127.0.0.1)"
if timeout 5 mysql -h 127.0.0.1 -P 3306 -u oscar_ext -poscar123 -e "SELECT VERSION();" 2>&1 | grep -q MariaDB; then
    echo "   ✓ MySQL connection to localhost WORKS!"
    mysql -h 127.0.0.1 -P 3306 -u oscar_ext -poscar123 -e "SELECT VERSION();" 2>&1 | grep -v mysql
else
    echo "   ✗ MySQL connection to localhost FAILED or TIMED OUT"
    echo "   💡 This is a Docker Desktop for Mac networking issue"
fi
echo ""

# Test 4: MySQL protocol via container IP
if [ ! -z "$CONTAINER_IP" ]; then
    echo "✅ Test 4: MySQL protocol test via container IP ($CONTAINER_IP)"
    if timeout 5 mysql -h $CONTAINER_IP -P 3306 -u oscar_ext -poscar123 -e "SELECT VERSION();" 2>&1 | grep -q MariaDB; then
        echo "   ✓ MySQL connection to container IP WORKS!"
        mysql -h $CONTAINER_IP -P 3306 -u oscar_ext -poscar123 -e "SELECT VERSION();" 2>&1 | grep -v mysql
    else
        echo "   ✗ MySQL connection to container IP FAILED"
    fi
    echo ""
fi

# Test 5: Container health status
echo "✅ Test 5: Container health status"
HEALTH_STATUS=$(docker inspect openo-mariadb-dev | grep '"Status"' | head -1 | awk -F'"' '{print $4}')
echo "   Health: $HEALTH_STATUS"
if [ "$HEALTH_STATUS" = "healthy" ]; then
    echo "   ✓ Container is healthy"
else
    echo "   ⚠️  Container is not fully healthy yet"
    echo "   💡 Wait 30 seconds and try again"
fi
echo ""

# Summary and recommendations
echo "========================================"
echo "📊 SUMMARY & RECOMMENDATIONS"
echo "========================================"
echo ""

if timeout 5 mysql -h 127.0.0.1 -P 3306 -u oscar_ext -poscar123 -e "SELECT 1;" >/dev/null 2>&1; then
    echo "✅ You can use TablePlus with these settings:"
    echo "   Host: 127.0.0.1"
    echo "   Port: 3306"
    echo "   User: oscar_ext"
    echo "   Password: oscar123"
    echo "   Database: oscar"
    echo "   Over SSH: DISABLED"
elif [ ! -z "$CONTAINER_IP" ] && timeout 5 mysql -h $CONTAINER_IP -P 3306 -u oscar_ext -poscar123 -e "SELECT 1;" >/dev/null 2>&1; then
    echo "⚠️  Localhost connection failed, but container IP works:"
    echo "   Host: $CONTAINER_IP"
    echo "   Port: 3306"
    echo "   User: oscar_ext"
    echo "   Password: oscar123"
    echo "   Database: oscar"
else
    echo "❌ Direct MySQL connections are not working."
    echo ""
    echo "🔧 SOLUTION: Use SSH tunnel in TablePlus:"
    echo "   1. Enable 'Over SSH' (blue button)"
    echo "   2. Connection Settings:"
    echo "      Host: 127.0.0.1"
    echo "      Port: 3306"
    echo "      User: oscar_ext"
    echo "      Password: oscar123"
    echo "   3. SSH Settings:"
    echo "      SSH Host: 127.0.0.1"
    echo "      SSH Port: 22"
    echo "      SSH User: $(whoami)"
    echo "      SSH Password: [your Mac password]"
    echo ""
    echo "   OR restart the database container:"
    echo "   docker compose -f .devcontainer/docker-compose.yml restart db"
fi
echo ""
