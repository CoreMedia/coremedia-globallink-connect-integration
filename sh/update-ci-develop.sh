#!/usr/bin/env bash

set -o errexit   # abort on nonzero exit status
set -o nounset   # abort on unbound variable
set -o pipefail  # don't hide errors within pipes

# Respect GitHub Actions debug mode.
declare -ir MODE_DEBUG=${RUNNER_DEBUG:-0}

# Respect GitHub Actions verbosity also for logging bash script.
if (( MODE_DEBUG )); then
  set -o xtrace
else
  set +o xtrace
fi

# Check if required environment variables are set
: "${GIT_USER_NAME:?Environment variable GIT_USER_NAME is required}"
: "${GIT_USER_EMAIL:?Environment variable GIT_USER_EMAIL is required}"

# Configure Git user
git config user.name "${GIT_USER_NAME}"
git config user.email "${GIT_USER_EMAIL}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
declare -r SCRIPT_DIR
declare -r PATCH_FILE="${SCRIPT_DIR}/ci-develop.patch"

# Ensure we are on the develop branch
git switch develop

# Force the ci/develop branch to match the develop branch
git branch -f ci/develop develop

# Checkout the ci/develop branch
git switch ci/develop

# Apply the patch file
git apply "${PATCH_FILE}"

# Commit and push the changes
git add .
git commit -m "ci/develop: Prepare for CoreMedia CI"
git push origin ci/develop --force
