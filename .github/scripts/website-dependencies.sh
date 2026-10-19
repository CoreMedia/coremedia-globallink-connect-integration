#!/usr/bin/env bash
### ============================================================================
###
### website-dependencies.sh: runs one action of the "Website Dependencies"
### workflow from the website directory.
###
### Environment:
###   ACTION          maintain | block-advisory
###   RECHECK_BLOCKS  true | false (maintain)
###   GHSA            advisory ID (block-advisory)
###   RATIONALE       why the fix cannot be applied (block-advisory)
###
### Step outputs (GITHUB_OUTPUT):
###   status  0 = done, 1 = error, 2 = done but action required
###   report  path of the Markdown report (pull request body)
###   branch  pull request branch
###   title   pull request title and commit message
###
### The script itself exits 0, so that the workflow can still open a pull
### request for status 2; the workflow fails afterward for status 1 and 2.
###
### ============================================================================

# Enable strict mode
set -o errexit -o errtrace -o pipefail -o nounset

function set_output() {
  local name="${1}"
  local value="${2}"
  echo "${name}=${value}" >> "${GITHUB_OUTPUT:-/dev/stdout}"
}

function main() {
  local report="${RUNNER_TEMP:-/tmp}/dependency-report.md"
  local status=0
  local branch=""
  local title=""

  case "${ACTION:-maintain}" in
    maintain)
      branch="website/automation-dependencies"
      title="build(pnpm): website dependency maintenance"
      local args=(--report "${report}")
      if [[ "${RECHECK_BLOCKS:-false}" == "true" ]]; then
        args+=(--recheck-blocks)
      fi
      node ./scripts/deps/maintain.mjs "${args[@]}" || status=$?
      ;;
    block-advisory)
      branch="website/block-advisory-${GHSA:-unknown}"
      title="build(pnpm): block advisory ${GHSA:-unknown}"
      node ./scripts/deps/block.mjs \
        --ghsa "${GHSA:-}" \
        --rationale "${RATIONALE:-}" \
        --report "${report}" || status=$?
      ;;
    *)
      echo "ERROR: Unknown action: ${ACTION}" >&2
      status=1
      ;;
  esac

  set_output status "${status}"
  set_output report "${report}"
  set_output branch "${branch}"
  set_output title "${title}"
}

main "${@}"
