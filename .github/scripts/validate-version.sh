#!/bin/bash
set -e

# Get current version from pom.xml
CURRENT_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
echo "Current version from pom.xml without patch part: $CURRENT_VERSION"

# Check if the current version exists in the tags
if git tag --list | grep -q "^$CURRENT_VERSION$"; then
  echo "Current version ($CURRENT_VERSION) already exists in tags - skipping publication"
else
  echo "Version changed from $LATEST_VERSION to $CURRENT_VERSION - will publish"
fi
