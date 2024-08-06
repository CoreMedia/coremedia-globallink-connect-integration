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

# GIT_USER_NAME and GIT_USER_EMAIL: Prefer possibly set environment variables,
# that are expected to be set by the GitHub Action. If not set, use the
# default values from the local Git configuration. Fail, if these are not set.
#
# While this script is intended to be used in a GitHub Actions, it should also
# be possible to run it locally. In this case, the local Git configuration
# should be used.
#
# Note, that `git config user.name` will fail, if unset, so that we add
# `|| true` to avoid the script to fail, if the Git configuration is not set.

declare -r GIT_USER_NAME="${GIT_USER_NAME:-$(git config user.name || true)}"
declare -r GIT_USER_EMAIL="${GIT_USER_EMAIL:-$(git config user.email || true)}"

# Check requirements: Both GIT_USER_NAME and GIT_USER_EMAIL must be set.

if [[ -z "${GIT_USER_NAME}" ]]; then
  echo "Error: Environment variable GIT_USER_NAME is not set."
  exit 1
fi

if [[ -z "${GIT_USER_EMAIL}" ]]; then
  echo "Error: Environment variable GIT_USER_EMAIL is not set."
  exit 1
fi

# Ensure, Git is configured with some user name and email.
git config user.name "${GIT_USER_NAME}"
git config user.email "${GIT_USER_EMAIL}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
declare -r SCRIPT_DIR
declare -r PATCH_FILE="${SCRIPT_DIR}/ci-develop.patch"

# Ensure we are on the develop branch
git switch develop

# Force the ci/develop branch to match the develop branch
git branch --force ci/develop develop

# Checkout the ci/develop branch
git switch ci/develop

# Apply the patch file
git apply "${PATCH_FILE}"

# Commit and push the changes
git add .
git commit --message="ci/develop: Prepare for CoreMedia CI"
git push origin ci/develop --force

# Return to the develop branch
# Especially meant for interactive use, so that the user is not left on the
# ci/develop branch.
git switch develop
