#!/bin/bash
# Verify database is accessible from inside the Docker container
# Run this from VS Code terminal to confirm everything is working internally

echo "🔍 Verifying MariaDB from Inside Container..."
echo "=============================================="
echo ""

# Test connection
echo "✅ Testing connection to database..."
if mysql -h db -u root -ppassword -e "SELECT 1;" >/dev/null 2>&1; then
    echo "   ✓ Database connection successful"
else
    echo "   ✗ Cannot connect to database"
    exit 1
fi
echo ""

# Get version
echo "📊 Database Information:"
mysql -h db -u root -ppassword -e "SELECT VERSION();" | tail -n 1
echo ""

# Test oscar_ext user
echo "✅ Testing oscar_ext user..."
if mysql -h db -u oscar_ext -poscar123 -D oscar -e "SELECT 1;" >/dev/null 2>&1; then
    echo "   ✓ oscar_ext user can connect"
else
    echo "   ✗ oscar_ext user cannot connect"
fi
echo ""

# Show current configuration
echo "📋 Network Configuration:"
mysql -h db -u root -ppassword -e "
    SELECT
        VARIABLE_NAME,
        VARIABLE_VALUE
    FROM information_schema.GLOBAL_VARIABLES
    WHERE VARIABLE_NAME IN (
        'bind_address',
        'port',
        'connect_timeout',
        'wait_timeout',
        'net_read_timeout',
        'net_write_timeout',
        'max_connections'
    )
    ORDER BY VARIABLE_NAME;
"
echo ""

# Show database stats
echo "📈 Database Statistics:"
mysql -h db -u root -ppassword oscar -e "
    SELECT
        'Total Tables' as Metric,
        COUNT(*) as Value
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = 'oscar'
    UNION ALL
    SELECT
        'Total Demographics',
        COUNT(*)
    FROM demographic
    UNION ALL
    SELECT
        'Total Appointments',
        COUNT(*)
    FROM appointment
    UNION ALL
    SELECT
        'Total Providers',
        COUNT(*)
    FROM provider;
"
echo ""

# Show active connections
echo "🔌 Active Connections:"
mysql -h db -u root -ppassword -e "SHOW PROCESSLIST;" | awk 'NR==1 || /oscar_ext|root/'
echo ""

echo "=============================================="
echo "✅ Database is healthy and accessible!"
echo ""
echo "🔗 Connection details for external tools:"
echo "   User: oscar_ext"
echo "   Password: oscar123"
echo "   Database: oscar"
echo "   Port: 3306 (exposed to host)"
echo ""
echo "💡 To connect from your Mac:"
echo "   1. Run: ./test-db-connection.sh (on Mac terminal)"
echo "   2. See: fix-database-connection.md for detailed guide"
echo ""
