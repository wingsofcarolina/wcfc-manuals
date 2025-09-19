#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

JAR_FILE="$PROJECT_ROOT/docker/wcfc-manuals.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo -e "JAR file not found at $JAR_FILE"
    echo -e "Please build the application first (e.g., using the Makefile)"
    exit 1
fi

if [[ -z "$@" ]]; then
  CMD=/app/test-scripts/run-tests.sh
else
  CMD="$@"
fi

echo -e "Running integration test in container..."
export GMAIL_SERVICE_ACCOUNT_KEY=$(<$SCRIPT_DIR/test-data/gmail-test-key.json)
export WCFC_JWT_SECRET=$(openssl rand -base64 36)
podman run -it --rm -p 9300:9300 \
    -v "$JAR_FILE:/app/wcfc-manuals.jar" \
    -v "$SCRIPT_DIR/test-data:/app/test-data" \
    -v "$SCRIPT_DIR/test-scripts:/app/test-scripts" \
    -e GMAIL_SERVICE_ACCOUNT_KEY \
    -e WCFC_JWT_SECRET \
    wcfc-integration-testing \
    bash -c "$CMD"

