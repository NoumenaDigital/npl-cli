#!/bin/bash
set -e

echo "Generating native configuration with Maven..."
mvn clean verify -Pconfig-gen

echo "Validating that there are no unexpected changes in the native-image configs..."

DIFF_FILE=$(mktemp)

git diff --ignore-blank-lines -- src/main/resources/META-INF/native-image/ > "$DIFF_FILE"

# Filter out jansi-related differences
if grep -v "org/fusesource/jansi" "$DIFF_FILE" | grep -q .; then
  echo "Error: Running 'mvn verify -Pconfig-gen' resulted in unexpected changes to native-image configs:"
  cat "$DIFF_FILE"
  rm "$DIFF_FILE"
  exit 1
fi

rm "$DIFF_FILE"

echo "Validation passed: No unexpected changes detected in native-image configuration."
echo "Note: Jansi-related differences were intentionally ignored."
exit 0
