#!/bin/bash
set -e

# Get current version from pom.xml
CURRENT_VERSION=$(grep -m 1 "<version>" pom.xml | sed -E 's/.*<version>([^<]+)<\/version>.*/\1/' | sed -E 's/-.*//')
echo "Current version from pom.xml without patch part: $CURRENT_VERSION"

# Get latest release tag (assuming tags are in the format 1.0.0)
LATEST_VERSION=$(git describe --tags --match "*.*.*" --abbrev=0 2>/dev/null || echo "0.0.0")
echo "Latest version: $LATEST_VERSION"

# Check if version has changed
if [ "$CURRENT_VERSION" != "$LATEST_VERSION" ]; then
  echo "Version changed from $LATEST_VERSION to $CURRENT_VERSION - will publish"
else
  echo "Version unchanged ($CURRENT_VERSION) - skipping publication"
  exit 1
fi
