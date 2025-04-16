#!/bin/bash
set -e

# Run the generate-native-configs.sh script
echo "Running generate-native-configs.sh..."
./generate-native-configs.sh

# Check for git diff
echo "Validating that there are no changes in the repository..."
if [[ -n $(git diff --ignore-blank-lines) ]]; then
  echo "Error: Running generate-native-configs.sh resulted in changes to the repository."
  exit 1
fi

echo "Validation passed: No changes detected after running generate-native-configs.sh."
exit 0
