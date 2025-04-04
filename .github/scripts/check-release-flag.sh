#!/bin/bash
set -e

# Check if force release is requested through workflow dispatch
if [[ "$FORCE_RELEASE" == "true" ]]; then
  echo "should_release=true"
  echo "Release forced via workflow dispatch"
  exit 0
fi

# Get the commit message
COMMIT_MSG=$(git log -1 --pretty=%B)

# Check for Release trailer
if [[ "$COMMIT_MSG" == *"Release: true"* ]]; then
  echo "should_release=true"
  echo "Release: true found in commit message"
elif [[ "$COMMIT_MSG" == *"Release: false"* ]]; then
  echo "should_release=false"
  echo "Release: false found in commit message - skipping release"
else
  echo "should_release=false"
  echo "No Release trailer found in commit message - skipping release"
fi
