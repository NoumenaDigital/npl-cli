#!/bin/sh

set -o errexit
set -o nounset

# Make sure files are accessible
ls -la /terraform || echo "Cannot list files in /terraform"

retries=0
max=60
while ! wget --tries=1 --spider "${KEYCLOAK_URL}"
do
  retries=$((retries + 1))
  if [ ${retries} -eq ${max} ]
  then
    echo "Failed to contact Keycloak after ${retries} attempts"
    exit 1
  fi
  echo "[${retries}/${max}] Waiting for keycloak at ${KEYCLOAK_URL}"
  sleep 2
done

# Wait for admin API to be accessible
retries=0
while ! wget --tries=1 --spider "${KEYCLOAK_URL}/admin/" -q
do
  retries=$((retries + 1))
  if [ ${retries} -eq ${max} ]
  then
    echo "Failed to access Keycloak admin console after ${retries} attempts"
    exit 1
  fi
  echo "[${retries}/${max}] Waiting for Keycloak admin console"
  sleep 2
done

echo "Keycloak is ready. Running terraform apply..."
exec terraform apply -auto-approve -state="/state/state.tf"
