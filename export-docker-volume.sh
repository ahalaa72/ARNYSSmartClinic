#!/bin/bash
# Export Docker Volume Script
# This script exports a Docker volume to a tar.gz file for transfer

VOLUME_NAME="${1}"
OUTPUT_FILE="${2:-volume-backup.tar.gz}"

if [ -z "$VOLUME_NAME" ]; then
    echo "Usage: $0 <volume_name> [output_file]"
    echo ""
    echo "Available volumes:"
    docker volume ls
    exit 1
fi

echo "Exporting volume: $VOLUME_NAME"
echo "Output file: $OUTPUT_FILE"

# Create a temporary container to access the volume and export it
docker run --rm \
    -v "$VOLUME_NAME":/source:ro \
    -v "$(pwd)":/backup \
    alpine \
    tar czf "/backup/$OUTPUT_FILE" -C /source .

if [ $? -eq 0 ]; then
    echo "✓ Volume exported successfully!"
    echo "File: $(pwd)/$OUTPUT_FILE"
    ls -lh "$OUTPUT_FILE"
else
    echo "✗ Export failed!"
    exit 1
fi
