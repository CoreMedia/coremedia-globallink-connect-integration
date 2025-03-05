#!/usr/bin/env bash
### ============================================================================
###
### Check that `pom.xml` files do not contain relative paths to parent POMs
### outside the current project directory for GCC. Such a `<relativePath>`
### is a typical left-over when you work with GCC as submodule within the CMS
### workspace, and is created by the extensions tool.
###
### Before the release, these path should have been stripped from the POM files.
### ============================================================================

# Enable strict mode
set -o errexit -o errtrace -o pipefail -o nounset

GIT_ROOT="$(git rev-parse --show-toplevel)"
readonly GIT_ROOT

# Function to locate POM files with relative paths that leave the GIT_ROOT.
function locate_violating_files() {
  find "${GIT_ROOT}" -name 'pom.xml' -print0 | while IFS= read -r -d '' pom_file; do
    if grep -q '<relativePath>' "${pom_file}"; then
      relative_path=$(grep '<relativePath>' "${pom_file}" | sed -e 's/<\/\?relativePath>//g' | xargs)
      absolute_path=$(realpath -m "$(dirname "${pom_file}")/${relative_path}")
      if [[ "${absolute_path}" != "${GIT_ROOT}"* ]]; then
        echo "${pom_file}"
      fi
    fi
  done
}

function cmd_validate() {
  local issues_found=0
  while IFS= read -r pom_file; do
    issues_found=$((issues_found + 1))
    echo "ERROR: ${pom_file} contains a relative path that leaves the GIT_ROOT" >&2
  done < <(locate_violating_files)

  if ((issues_found > 0)); then
    echo "Found ${issues_found} issue(s). Run with command 'fix' to fix." >&2
    exit 1
  else
    echo "All POM files are valid."
  fi
}

function cmd_fix() {
  local issues_found=0
  while IFS= read -r pom_file; do
    issues_found=$((issues_found + 1))
    echo "WARN: ${pom_file} contains a relative path that leaves the GIT_ROOT" >&2
    sed -i '/<relativePath>/d' "${pom_file}"
  done < <(locate_violating_files)
  echo "All ${issues_found} issue(s) fixed."
}
function main() {
  local command="${1:-validate}"
  case "$command" in
    validate)
      cmd_validate
      ;;
    fix)
      cmd_fix
      ;;
    *)
      echo "Unknown command: $command" >&2
      exit 1
      ;;
  esac
}

main "${@}"
