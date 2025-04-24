#!/bin/bash
set -e

echo "Generating native configuration with Maven..."
mvn clean verify -Pconfig-gen

echo "Validating that there are no unexpected changes in the native-image configs..."

# Define patterns to ignore in diffs (whitelist)
IGNORE_PATTERNS=(
  "org/fusesource/jansi"  # Ignore jansi-related changes
  # Add more patterns here as needed
)

# Create temporary files for diff processing
RAW_DIFF=$(mktemp)
FILTERED_DIFF=$(mktemp)

# Get the raw diff for native-image directory
git diff --ignore-blank-lines -- src/main/resources/META-INF/native-image/ > "$RAW_DIFF"

# Copy raw diff to filtered diff as starting point
cp "$RAW_DIFF" "$FILTERED_DIFF"

# Filter out whitelisted patterns
for pattern in "${IGNORE_PATTERNS[@]}"; do
  # Create a new temp file for each iteration
  TEMP_FILTERED=$(mktemp)
  # Filter out lines containing the current pattern
  grep -v "$pattern" "$FILTERED_DIFF" > "$TEMP_FILTERED" || true
  # Replace the filtered diff with the new filtered content
  mv "$TEMP_FILTERED" "$FILTERED_DIFF"
done

# Count remaining diff lines (actual changes)
REMAINING_CHANGES=$(grep -c "^[+-]" "$FILTERED_DIFF" || true)

if [ "$REMAINING_CHANGES" -gt 0 ]; then
  echo "Error: Unexpected changes detected in native-image configs (excluding whitelisted patterns):"
  echo "Full diff:"
  cat "$RAW_DIFF"
  echo -e "\nUnexpected changes (after filtering whitelisted patterns):"
  cat "$FILTERED_DIFF"
  # Clean up
  rm -f "$RAW_DIFF" "$FILTERED_DIFF"
  exit 1
else
  echo "Only whitelisted changes detected or no changes at all."
fi

# Clean up
rm -f "$RAW_DIFF" "$FILTERED_DIFF"

echo "Validation passed: No unexpected changes detected in native-image configuration."
exit 0
