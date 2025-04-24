#!/bin/bash
set -e

# Run the Maven config-gen profile to generate native configuration
echo "Generating native configuration with Maven..."
mvn clean verify -Pconfig-gen

# Check for git diff, ignoring jansi-related entries
echo "Validating that there are no unexpected changes in the repository..."

# Create a temporary file for filtered diff
DIFF_FILE=$(mktemp)

# Get the diff and filter out jansi-related lines
git diff --ignore-blank-lines > "$DIFF_FILE"

# Filter out jansi-related differences
if grep -v "org/fusesource/jansi" "$DIFF_FILE" | grep -q .; then
  echo "Error: Running 'mvn verify -Pconfig-gen' resulted in unexpected changes to the repository:"
  cat "$DIFF_FILE"
  rm "$DIFF_FILE"
  exit 1
fi

# Clean up
rm "$DIFF_FILE"

echo "Validation passed: No unexpected changes detected after generating native configuration."
echo "Note: Jansi-related differences were intentionally ignored."
exit 0
