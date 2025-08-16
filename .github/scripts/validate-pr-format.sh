#!/bin/bash
set -e

# Get PR body from GitHub event file
PR_BODY=$(jq -r '.pull_request.body' "$GITHUB_EVENT_PATH")

# Check for placeholder comment
if [[ "$PR_BODY" == *"<!-- Description of the PR changes -->"* ]]; then
  echo "Error: PR description still contains placeholder comment. Please replace it with actual description."
  exit 1
fi

# Check Ticket format if present
if echo "$PR_BODY" | grep -q "Ticket:"; then
  # Remove carriage returns before checking
  if ! echo "$PR_BODY" | tr -d '\r' | grep -q "^Ticket: [A-Z]*-[0-9]*$"; then
    echo "Error: Ticket format is invalid. Should be 'Ticket: AAA-XXXX' on its own line where AAA is the ticket prefix and XXXX are numbers."
    exit 1
  fi
fi

echo "PR validation passed!"
exit 0
