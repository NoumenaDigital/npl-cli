#!/bin/bash
set -e

# Function to get the latest version
get_latest_version() {
  local repo_url="$1"
  local latest_version=""
  local api_response=""

  # Try using GitHub API if token is available
  if [ -n "$GITHUB_TOKEN" ]; then
    # Get existing releases
    api_response=$(curl -s -H "Authorization: Bearer $GITHUB_TOKEN" \
      "$repo_url")

    # Continue only if API call was successful
    if [ -n "$api_response" ] && [[ "$api_response" != *"Bad credentials"* ]] && [[ "$api_response" == \[* ]]; then
      # Get the latest version that matches our version pattern, sorted by creation date
      latest_version=$(echo "$api_response" | \
        jq -r '[.[] | select(.tag_name | test("^[0-9]+\\.[0-9]+\\.[0-9]+$")) |
                select(.tag_name | test("rc|alpha|beta|SNAPSHOT") | not) |
                {tag: .tag_name, date: .created_at}] |
                sort_by(.date) | reverse | .[0].tag // ""')
    fi
  fi

  echo "$latest_version"
}

# Extract the version from pom.xml
VERSION=$(grep -m 1 "<version>" pom.xml | sed -E 's/.*<version>([^<]+)<\/version>.*/\1/' | sed -E 's/-.*//')

# URL of the GitHub repository
REPO_URL="https://github.com/NoumenaDigital/npl-cli.git"

# Get the latest version
PLATFORM_VERSION=$(get_latest_version "$REPO_URL")

# If we couldn't get a version, display an error
if [ -z "$PLATFORM_VERSION" ]; then
  echo "Could not determine latest version"
  exit 1
fi

# Compare the versions
if [[ "$VERSION" == "$PLATFORM_VERSION" ]]; then
  echo "Versions match: $VERSION"
  exit 0
else
  echo "Versions do not match. pom.xml version: $VERSION, platform version: $PLATFORM_VERSION"
  exit 1
fi
