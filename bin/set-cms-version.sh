#!/usr/bin/env bash
### ----------------------------------------------------------------------------
###
### Sets versions for CoreMedia CMS Core artifacts (for Maven and PNPM
### ecosystems) to the denoted CMS version.
###
### This script is required as part of the release process, as tagged versions
### in the CoreMedia GlobalLink Connect Integration repository must refer to
### specific CMS versions for easy integration into releases CoreMedia Blueprint
### workspaces.
###
### Usage:
###   bin/set-cms-version.sh <cms-version>
###
### Examples:
###   bin/set-cms-version.sh 2506.0.1
###
### Operation Details:
###   - Updates cm.middle.core.version in `apps/workflow-server/pom.xml`.
###   - Replaces `workspace:*` references for `@coremedia/` dependencies in
###     all `apps/studio-client/*/package.json` files with the denoted CMS
###     version.
###
### Maintenance Note:
###   It is expected, that over time this script needs to be adjusted for the
###   respective approved CMS versions, thus, ensure, that the script is executed
###   from the respective maintenance branch instead from main.
###
### ----------------------------------------------------------------------------

set -o errexit
set -o nounset
set -o pipefail

unset CDPATH || true

# The path where we start operating from.
GIT_ROOT="$(git rev-parse --show-toplevel)"
readonly GIT_ROOT

# Cleanup function to remove temporary files
cleanup_temp_files() {
  if [[ -n "${GIT_ROOT:-}" ]]; then
    find "${GIT_ROOT}" -name "package.json.tmp" -type f -delete 2>/dev/null || true
  fi
}

# Set up trap to cleanup on exit, interrupt, or termination
trap cleanup_temp_files EXIT INT TERM

# JQ filter to update @coremedia/* dependencies that are set to workspace:*
# shellcheck disable=SC2016 # $ver is a jq variable, not a shell variable
readonly JQ_FILTER='
  (.dependencies // {}) |= (to_entries | map(
    if (.key | startswith("@coremedia/")) and .value == "workspace:*"
    then .value = $ver
    else .
    end
  ) | from_entries)
  | (.devDependencies // {}) |= (to_entries | map(
      if (.key | startswith("@coremedia/")) and .value == "workspace:*"
      then .value = $ver
      else .
      end
    ) | from_entries)
'

# Override cd to suppress any output (stdout/stderr) to guard against
# environments # that emit directory names (e.g., due to CDPATH or shell
# customization). Always use the builtin and exit on failure.
cd() {
	# Silent directory change. Let caller / set -e handle failures.
	builtin cd "$@" >/dev/null 2>&1 || return 1
}

function set_maven_version() {
  local cms_version="${1}"
  local pom_file="apps/workflow-server/pom.xml"

  cd "${GIT_ROOT}"
  printf "Setting cm.middle.core.version to '%s' in '%s'.\n" "${cms_version}" "${pom_file}"
  mvn --batch-mode \
      --file "${pom_file}" \
      versions:set-property \
      -Dproperty=cm.middle.core.version \
      -DnewVersion="${cms_version}" \
      -DgenerateBackupPoms=false
}

function set_pnpm_versions() {
  local cms_version="${1}"

  cd "${GIT_ROOT}"
  local package_json_files
  mapfile -t -d '' package_json_files < <(find apps/studio-client -name 'package.json' -print0 || true)

  for package_json_file in "${package_json_files[@]}"; do
    printf "Setting @coremedia/* dependencies to '%s' in '%s'.\n" "${cms_version}" "${package_json_file}"
    jq --arg ver "${cms_version}" "${JQ_FILTER}" "${package_json_file}" > "${package_json_file}.tmp" && mv "${package_json_file}.tmp" "${package_json_file}"
  done
}

function main() {
  if (( $# != 1 )); then
    echo "Usage: $0 <cms-version>" >&2
    exit 1
  fi

  local cms_version="${1}"

  set_maven_version "${cms_version}"
  set_pnpm_versions "${cms_version}"

  echo "Version update to CMS version '${cms_version}' completed."
}

main "$@"
