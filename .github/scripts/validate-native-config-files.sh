#!/bin/bash
set -e

# Run the Maven config-gen profile to generate native configuration
echo "Generating native configuration with Maven..."
mvn clean verify -Pconfig-gen

# Check for git diff
echo "Validating that there are no changes in the repository..."
if [[ -n $(git diff --ignore-blank-lines) ]]; then
  echo "Error: Running 'mvn verify -Pconfig-gen' resulted in changes to the repository:"
  git diff --ignore-blank-lines
  exit 1
fi

echo "Validation passed: No changes detected after generating native configuration."
exit 0
