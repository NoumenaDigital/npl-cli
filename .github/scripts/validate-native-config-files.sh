#!/bin/bash
set -e

echo "Generating native configuration with Maven..."
mvn clean verify -Pconfig-gen

echo "Validating native-image configs..."

# Store the diff output
DIFF_OUTPUT=$(git diff --ignore-blank-lines -- src/main/resources/META-INF/native-image/)

# If there are changes, analyze them
if [[ -n "$DIFF_OUTPUT" ]]; then
  # Extract only added/removed content lines (lines starting with + or - but not +++ or ---)
  # and remove the leading + or - character
  CHANGED_CONTENT=$(echo "$DIFF_OUTPUT" | grep -E '^(\+|-)[^+-]' | sed 's/^[+-]//')

  # Define acceptable patterns
  # 1. Lines containing jansi-related changes
  JANSI_PATTERN='org/fusesource/jansi'

  # 2. Lines with only JSON syntax (brackets and commas with optional whitespace)
  JSON_SYNTAX_PATTERN='^\s*[{},]+\s*$'

  # Count total content changes
  TOTAL_CHANGES=$(echo "$CHANGED_CONTENT" | wc -l)

  # Count acceptable content changes (matching our patterns)
  ACCEPTABLE_CHANGES=$(echo "$CHANGED_CONTENT" | grep -E "($JANSI_PATTERN|$JSON_SYNTAX_PATTERN)" | wc -l)

  # If all content changes are acceptable, pass validation
  if [[ "$TOTAL_CHANGES" -eq "$ACCEPTABLE_CHANGES" ]]; then
    echo "Validation passed: Only acceptable changes detected in native-image configuration."
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
