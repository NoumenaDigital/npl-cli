#!/bin/bash
set -e

echo "Generating native configuration with Maven..."
mvn clean verify -Pconfig-gen

echo "Validating native-image configs..."
if [[ -n $(git diff --ignore-all-space -- src/main/resources/META-INF/native-image/) ]]; then
  echo "Error: Unexpected changes detected in native-image configs:"
  git diff --ignore-all-space -- src/main/resources/META-INF/native-image/
  exit 1
else
  echo "Validation passed: No changes detected in native-image configuration."
  exit 0
fi
