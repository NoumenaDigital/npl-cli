#!/bin/bash
set -e

echo "Generating native configuration with Maven..."
mvn clean verify -Pconfig-gen

echo "Validating native-image configs..."

# Store the diff output
DIFF_OUTPUT=$(git diff --ignore-blank-lines -- src/main/resources/META-INF/native-image/)

# Check if diff contains only acceptable changes
if [[ -n "$DIFF_OUTPUT" ]]; then
  # Count lines that contain jansi patterns or are just brackets/commas
  ACCEPTABLE_LINES=$(echo "$DIFF_OUTPUT" | grep -E '(org/fusesource/jansi|^\s*[{},]+\s*$|^\+|^-|^diff|^index|^---|^\+\+\+)' | wc -l)
  TOTAL_LINES=$(echo "$DIFF_OUTPUT" | wc -l)

  # If all lines are acceptable, pass validation
  if [[ "$ACCEPTABLE_LINES" -eq "$TOTAL_LINES" ]]; then
    echo "Validation passed: Only acceptable changes found in native-image configuration."
    exit 0
  else
    echo "Error: Unexpected changes detected in native-image configs:"
    echo "$DIFF_OUTPUT"
    exit 1
  fi
else
  echo "Validation passed: No changes detected in native-image configuration."
  exit 0
fi
