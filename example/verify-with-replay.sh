#!/usr/bin/env bash

# Example script demonstrating replay verification
# This script shows how to verify an audit trail with replay

set -e

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== NPL Replay Verification Example ===${NC}\n"

# Check if audit file and sources are provided
if [ "$#" -lt 2 ]; then
    echo "Usage: $0 <audit-file> <sources-path>"
    echo ""
    echo "Example:"
    echo "  $0 audit.json ./my-protocol"
    echo ""
    echo "Optional environment variables:"
    echo "  NPL_BASE_URL           - Runtime URL (default: http://localhost:12000)"
    echo "  NPL_SKIP_DOCKER        - Skip Docker startup (default: false)"
    echo "  NPL_CLEANUP            - Clean up Docker after replay (default: false)"
    echo "  NPL_DOCKER_COMPOSE_CMD - Docker compose command (default: docker compose up -d --wait)"
    echo "  NPL_DEPLOY_CMD         - Deploy command (default: npl deploy)"
    exit 1
fi

AUDIT_FILE="$1"
SOURCES_PATH="$2"

# Validate inputs
if [ ! -f "$AUDIT_FILE" ] && [[ ! "$AUDIT_FILE" =~ ^https?:// ]]; then
    echo -e "${YELLOW}Error: Audit file not found: $AUDIT_FILE${NC}"
    exit 1
fi

if [ ! -d "$SOURCES_PATH" ] && [ ! -f "$SOURCES_PATH" ]; then
    echo -e "${YELLOW}Error: Sources path not found: $SOURCES_PATH${NC}"
    exit 1
fi

echo -e "${GREEN}Configuration:${NC}"
echo -e "  Audit file:    $AUDIT_FILE"
echo -e "  Sources path:  $SOURCES_PATH"
echo -e "  Base URL:      ${NPL_BASE_URL:-http://localhost:12000}"
echo -e "  Skip Docker:   ${NPL_SKIP_DOCKER:-false}"
echo -e "  Cleanup:       ${NPL_CLEANUP:-false}"
echo ""

# Run verification with replay
echo -e "${BLUE}Starting verification with replay...${NC}\n"

if npl verify \
    --audit "$AUDIT_FILE" \
    --sources "$SOURCES_PATH"; then
    echo ""
    echo -e "${GREEN}✓ Verification successful!${NC}"
    echo ""
    echo "All checks passed:"
    echo "  ✓ Structure validation"
    echo "  ✓ Hash-chain completeness"
    echo "  ✓ State hash verification"
    echo "  ✓ Signature verification"
    echo "  ✓ Replay verification"
    exit 0
else
    EXIT_CODE=$?
    echo ""
    echo -e "${YELLOW}✗ Verification failed!${NC}"
    echo ""
    echo "Check the error messages above for details."
    exit $EXIT_CODE
fi

