#!/bin/bash
# Restart MariaDB container to apply new configuration
# Run this from Mac terminal (NOT VS Code)

echo "🔄 Restarting MariaDB container..."
echo "====================================="
echo ""

# Navigate to project directory
cd "$(dirname "$0")"

# Stop the database container
echo "⏸️  Stopping database container..."
docker compose -f .devcontainer/docker-compose.yml stop db

# Start it again
echo "▶️  Starting database container..."
docker compose -f .devcontainer/docker-compose.yml start db

echo ""
echo "⏳ Waiting for database to become healthy..."
echo "   This takes about 30-60 seconds..."
echo ""

# Wait for health check
for i in {1..12}; do
    HEALTH=$(docker inspect openo-mariadb-dev --format='{{.State.Health.Status}}' 2>/dev/null)

    if [ "$HEALTH" = "healthy" ]; then
        echo "✅ Database is healthy!"
        echo ""
        echo "📊 New configuration applied:"
        docker exec openo-mariadb-dev mysql -u root -ppassword -e "SELECT VARIABLE_NAME, VARIABLE_VALUE FROM information_schema.GLOBAL_VARIABLES WHERE VARIABLE_NAME IN ('connect_timeout', 'wait_timeout', 'net_read_timeout', 'net_write_timeout') ORDER BY VARIABLE_NAME;"
        echo ""
        echo "✅ Try TablePlus connection again!"
        exit 0
    fi

    echo "   Status: $HEALTH (attempt $i/12)"
    sleep 5
done

echo "⚠️  Container is running but health check not passed yet"
echo "   Wait another 30 seconds and try connecting"
