#!/bin/bash
set -e

# Get current version from pom.xml
CURRENT_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
echo "Current version from pom.xml: $CURRENT_VERSION"

# Check if the current version exists in the tags
if git tag --list | grep -q "^$CURRENT_VERSION$"; then
  echo "should_release=false"
  echo "Current version ($CURRENT_VERSION) already exists in tags - skipping publication"
else
  echo "should_release=true"
  echo "Version changed from $LATEST_VERSION to $CURRENT_VERSION - will publish"
fi
