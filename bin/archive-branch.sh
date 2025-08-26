#!/usr/bin/env bash
# ==============================================================================
###
### # archive-stat.sh
###
### Archives the given branch by tagging it as `archive/<branch name>` and
### deleting the branch both locally and remotely.
###
### **Usage**:
###
### ```bash
### ./bin/archive-branch.sh "maintenance/2406.x"
### ```
###
# ==============================================================================

# ------------------------------------------------------------------------------
###
### ## Constants
###
# ------------------------------------------------------------------------------

### * `DEBUG`: Provide option to trigger debug output with different verbosity
###   levels.
declare -ri DEBUG=${DEBUG:-0}

# ------------------------------------------------------------------------------
# Bash Options
# ------------------------------------------------------------------------------

set -o errexit  # abort on nonzero exit status
set -o nounset  # abort on unbound variable
set -o pipefail # don't hide errors within pipes
### Call with `DEBUG=2 <command>.sh <file>` to enable verbose debug output
if ((DEBUG > 1)); then
    set -o xtrace # show expanded commands
else
    set +o xtrace # do not show expanded commands
fi

# ------------------------------------------------------------------------------
# Functions
# ------------------------------------------------------------------------------

# Determines, if the current working directory is part of a Git repository.
function is_git_repository() {
    git rev-parse --is-inside-work-tree &>/dev/null
}

# Verifies, that the current working directory is part of a Git repository.
# Fails with error, otherwise.
function verify_git_repository() {
    if ! is_git_repository; then
        printf "Error: The current working directory is not part of a Git repository.\n"
        exit 1
    fi
}

function is_remote_branch() {
    local -r branch_name="${1}"
    local -r remote_name="${2:-origin}"

    git show-ref --verify --quiet "refs/remotes/${remote_name}/${branch_name}"

    if git ls-remote --heads "${remote_name}" "${branch_name}" | grep -q "refs/heads/${branch_name}"; then
        return 0
    else
        return 1
    fi
}

function is_local_branch() {
    local -r branch_name="${1}"

    git show-ref --verify --quiet "refs/heads/${branch_name}"
}

function is_branch() {
    local -r branch_name="${1}"
    local -r remote_name="${2:-origin}"

    is_local_branch "${branch_name}" || is_remote_branch "${branch_name}" "${remote_name}"
}

function verify_branch_exists() {
    local -r branch_name="${1}"
    local -r remote_name="${2:-origin}"

    if ! is_branch "${branch_name}" "${remote_name}"; then
        printf "Error: Branch '%s' does not exist.\n" "${branch_name}" >&2
        exit 1
    fi
}

function archive_branch() {
    local -r branch_name="${1}"
    local -r remote_name="${2:-origin}"

    verify_branch_exists "${branch_name}" "${remote_name}"

    if is_remote_branch "${branch_name}" "${remote_name}"; then
        archive_remote_branch "${branch_name}" "${remote_name}"
        elif is_local_branch "${branch_name}"; then
        archive_local_only_branch "${branch_name}"
    fi
}

function archive_remote_branch() {
    local -r branch_name="${1}"
    local -r remote_name="${2:-origin}"
    local -r archive_tag="archive/${branch_name}"

    # Remove local branch, if existing. Do this first, so that the script
    # may fail early, if the local branch cannot be removed.
    if is_local_branch "${branch_name}"; then
        git branch --delete --force "${branch_name}"
        printf "Removed local branch '%s'.\n" "${branch_name}"
    fi

    # Fetch the latest information for the specific branch
    git fetch --depth=1 "${remote_name}" "${branch_name}"

    # Create the tag locally
    git tag --annotate "${archive_tag}" --message "Archived branch '${branch_name}'" "refs/remotes/${remote_name}/${branch_name}"

    printf "Created tag '%s' for remote branch '%s'.\n" "${archive_tag}" "${branch_name}"

    # Push the tag to the remote
    git push "${remote_name}" "${archive_tag}"

    printf "Pushed tag '%s' to remote '%s'.\n" "${archive_tag}" "${remote_name}"

    # Remove the branch remotely
    git push "${remote_name}" --delete "${branch_name}"
    printf "Removed remote branch '%s' on remote '%s'.\n" "${branch_name}" "${remote_name}"
}

function archive_local_only_branch() {
    local -r branch_name="${1}"
    local -r archive_tag="archive/${branch_name}"

    # Create the tag locally
    git tag --annotate "${archive_tag}" --message "Archived branch '${branch_name}'" "refs/heads/${branch_name}"

    printf "Created tag '%s' for branch '%s'.\n" "${archive_tag}" "${branch_name}"

    # Remove the branch locally
    git branch --delete --force "${branch_name}"
    printf "Removed local branch '%s'.\n" "${branch_name}"
}

function main() {
    local -r possibly_remote_name="${1:-}"
    local -r possibly_branch_name="${2:-}"
    local remote_name
    local branch_name

    verify_git_repository

    if [[ -z "${possibly_remote_name}" ]]; then
        printf "Error: No branch name provided.\n" >&2
        printf "Usage: %s [<remote name>] <branch name>\n" "$(basename "$0")" >&2
        printf "Example: %s origin maintenance/2406.x\n" "$(basename "$0")" >&2
        exit 1
    fi

    if [[ -z "${possibly_branch_name}" ]]; then
        branch_name="${possibly_remote_name}"
        remote_name="origin"
    else
        remote_name="${possibly_remote_name}"
        branch_name="${possibly_branch_name}"
    fi

    archive_branch "${branch_name}" "${remote_name}"

    echo "Done."
}

# ------------------------------------------------------------------------------
# Main
# ------------------------------------------------------------------------------

main "${@}"
