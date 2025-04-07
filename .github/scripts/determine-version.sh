#!/bin/bash
set -e

# Extract current platform version from pom.xml
CURRENT_PLATFORM_VERSION=$(grep -m 1 "<platform.version>" pom.xml | sed -E 's/.*<platform.version>(.*)<\/platform.version>.*/\1/')
echo "Current platform version: $CURRENT_PLATFORM_VERSION"

# Check if this commit changed the platform version
git fetch origin
PREVIOUS_COMMIT=$(git rev-parse HEAD~1)
PREVIOUS_PLATFORM_VERSION=$(git show $PREVIOUS_COMMIT:pom.xml | grep -m 1 "<platform.version>" | sed -E 's/.*<platform.version>(.*)<\/platform.version>.*/\1/')
echo "Previous platform version: $PREVIOUS_PLATFORM_VERSION"

# Default to using plain platform version
RELEASE_TAG="$CURRENT_PLATFORM_VERSION"

# If platform version didn't change, check if we need to use a suffix
if [ "$CURRENT_PLATFORM_VERSION" = "$PREVIOUS_PLATFORM_VERSION" ] && [ -n "$GITHUB_TOKEN" ]; then
  # Get existing releases
  API_RESPONSE=$(curl -s -H "Authorization: Bearer $GITHUB_TOKEN" \
    "https://api.github.com/repos/NoumenaDigital/npl-cli/releases")

  # Continue only if API call was successful
  if [ -n "$API_RESPONSE" ] && [[ "$API_RESPONSE" != *"Bad credentials"* ]] && [[ "$API_RESPONSE" == \[* ]]; then
    # Get releases matching current platform version
    LATEST_RELEASE=$(echo "$API_RESPONSE" | \
      jq -r "[.[] | select(.tag_name | startswith(\"$CURRENT_PLATFORM_VERSION\")) | .tag_name] | sort_by(split(\"-\")[1] | if . == null then 0 else tonumber end) | last // \"\"")

    # Only add/increment suffix if we already have a release for this platform version
    if [ -n "$LATEST_RELEASE" ] && [ "$LATEST_RELEASE" != "null" ]; then
      if [ "$LATEST_RELEASE" = "$CURRENT_PLATFORM_VERSION" ]; then
        # Previous release was plain version, start numbering
        RELEASE_TAG="$CURRENT_PLATFORM_VERSION-1"
      else
        # Increment existing suffix
        SUFFIX=$(echo $LATEST_RELEASE | sed -E 's/.*-([0-9]+)$/\1/')
        NEW_SUFFIX=$((SUFFIX + 1))
        RELEASE_TAG="$CURRENT_PLATFORM_VERSION-$NEW_SUFFIX"
      fi
    fi
  fi
fi

RELEASE_NAME="NPL CLI $RELEASE_TAG"

# Output variables
echo "release_tag=$RELEASE_TAG"
echo "release_name=$RELEASE_NAME"
export release_tag=$RELEASE_TAG
export release_name=$RELEASE_NAME
