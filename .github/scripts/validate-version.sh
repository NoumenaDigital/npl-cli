#!/bin/bash
set -e

# Extract the version from pom.xml
NPL_CLI_VERSION=$(grep -m 1 "<version>" pom.xml | sed -E 's/.*<version>([^<]+)<\/version>.*/\1/' | sed -E 's/-.*//')

# URL of the GitHub repository
REPO_URL="https://$GITHUB_TOKEN@github.com/NoumenaDigital/platform.git"

# Get the latest version
PLATFORM_VERSION=$(git ls-remote --quiet --exit-code --tags --sort="-version:refname" $REPO_URL origin '????.*.*' | head -n1 | cut -d'/' -f3)
# If we couldn't get a version, display an error
if [ -z "$PLATFORM_VERSION" ]; then
  echo "Could not determine latest version"
  exit 1
fi

# Compare the versions
if [[ "$NPL_CLI_VERSION" == "$PLATFORM_VERSION" ]]; then
  echo "Versions match: $NPL_CLI_VERSION"
  exit 0
else
  echo "Versions do not match. pom.xml version: $VERSION, platform version: $PLATFORM_VERSION"
  exit 1
fi
