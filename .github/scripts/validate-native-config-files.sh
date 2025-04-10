#!/bin/bash
set -e

# Run the generate-native-configs.sh script
printf "Running generate-native-configs.sh..."
./generate-native-configs.sh

# Check for git diff
printf "\nValidating that there are no changes in the repository..."
if [[ -n $(git diff) ]]; then
  printf "\nError: Running generate-native-configs.sh resulted in changes to the repository."
  exit 1
fi

printf "Validation passed: No changes detected after running generate-native-configs.sh."
exit 0
