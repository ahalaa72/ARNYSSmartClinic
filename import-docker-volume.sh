#!/bin/bash
# Import Docker Volume Script
# This script imports a Docker volume from a tar.gz file

VOLUME_NAME="${1}"
INPUT_FILE="${2}"

if [ -z "$VOLUME_NAME" ] || [ -z "$INPUT_FILE" ]; then
    echo "Usage: $0 <volume_name> <input_file>"
    echo ""
    echo "Example: $0 openo_mysql_data volume-backup.tar.gz"
    exit 1
fi

if [ ! -f "$INPUT_FILE" ]; then
    echo "✗ Error: Input file '$INPUT_FILE' not found!"
    exit 1
fi

echo "Importing to volume: $VOLUME_NAME"
echo "From file: $INPUT_FILE"

# Create the volume if it doesn't exist
docker volume create "$VOLUME_NAME"

# Import the data into the volume
docker run --rm \
    -v "$VOLUME_NAME":/target \
    -v "$(pwd)":/backup \
    alpine \
    sh -c "cd /target && tar xzf /backup/$INPUT_FILE"

if [ $? -eq 0 ]; then
    echo "✓ Volume imported successfully!"
    echo "Volume: $VOLUME_NAME"
    docker volume inspect "$VOLUME_NAME"
else
    echo "✗ Import failed!"
    exit 1
fi
