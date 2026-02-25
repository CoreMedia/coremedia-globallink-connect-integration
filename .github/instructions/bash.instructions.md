---
applyTo: "**/*.sh"
---

# GitHub Copilot Instructions: Bash Shell Scripts

These instructions are dedicated to Bash shell scripts in this repository,
applying to all files matching `**/*.sh`.

## General Copilot Instruction

When working with Bash scripts, assume that you have expert knowledge in Bash
scripting, shell best practices, and common Unix utilities.

## Script Header & Shebang

* **Use `#!/usr/bin/env bash` shebang**:
  Always start scripts with `#!/usr/bin/env bash` for better portability:
  ```bash
  #!/usr/bin/env bash
  ```
* **Include a script description header**:
  Use a multi-line comment block at the top of the script to describe its
  purpose:
  ```bash
  #!/usr/bin/env bash
  ### ============================================================================
  ###
  ### Script Name: description of what the script does
  ### Purpose: More detailed explanation if needed
  ###
  ### ============================================================================
  ```

## Strict Mode & Error Handling

* **Enable the strict mode**:
  Always enable the strict mode at the beginning of scripts to catch errors
  early:
  ```bash
  # Enable strict mode
  set -o errexit -o errtrace -o pipefail -o nounset
  ```
  - `errexit`: Exit immediately if a command exits with a non-zero status
  - `errtrace`: Ensure ERR traps are inherited by functions
  - `pipefail`: Return the exit status of the last command in a pipe that failed
  - `nounset`: Treat unset variables as errors
* **Use error handling functions**:
  For scripts that need custom error handling, define error trap functions.

## Variables & Constants

* **Use `readonly` for constants**:
  Mark variables that shouldn't change as `readonly`:
  ```bash
  readonly GIT_ROOT="$(git rev-parse --show-toplevel)"
  ```
* **Use UPPER_CASE for constants**:
  Use uppercase names for constants and environment variables.
* **Use lower_case for local variables**:
  Use lowercase with underscores for local variables and function names.
* **Quote variables**:
  Always quote variables to prevent word splitting and globbing:
  ```bash
  echo "${variable}"
  ```
* **Use `local` for function variables**:
  Declare function-local variables with `local`:
  ```bash
  function my_function() {
    local local_var="value"
  }
  ```

## Functions

* **Define functions with `function` keyword**:
  Use the `function` keyword for clarity:
  ```bash
  function cmd_validate() {
    # function body
  }
  ```
* **Use descriptive function names**:
  Function names should clearly describe what they do, using verb-noun patterns
  when appropriate (e.g., `cmd_validate`, `locate_violating_files`).
* **Document complex functions**:
  Add comments explaining the purpose and behavior of non-trivial functions.

## Command Execution

* **Use command substitution with `$()`**:
  Prefer `$(command)` over backticks for command substitution:
  ```bash
  result=$(command)
  ```
* **Check command existence before use**:
  For non-standard commands, check if they exist before using them:
  ```bash
  if ! command -v tool &> /dev/null; then
    echo "Error: tool not found" >&2
    exit 1
  fi
  ```

## Loops & Input Processing

* **Use `while IFS= read -r` for line processing**:
  When reading lines from a file or command output:
  ```bash
  while IFS= read -r line; do
    echo "${line}"
  done < file.txt
  ```
* **Use process substitution for command output**:
  When looping over command output:
  ```bash
  while IFS= read -r item; do
    echo "${item}"
  done < <(find . -name "*.txt")
  ```
* **Use `find` with `-print0` and `read -d ''` for filenames with spaces**:
  ```bash
  find . -name '*.txt' -print0 | while IFS= read -r -d '' file; do
    echo "${file}"
  done
  ```

## Conditional Logic

* **Use `[[` for conditionals**:
  Prefer `[[ ]]` over `[ ]` for better functionality and safety:
  ```bash
  if [[ "${var}" == "value" ]]; then
    # do something
  fi
  ```
* **Use arithmetic comparisons correctly**:
  For numeric comparisons, use `(( ))`:
  ```bash
  if ((count > 0)); then
    # do something
  fi
  ```

## Output & Logging

* **Redirect errors to stderr**:
  Error messages should go to stderr:
  ```bash
  echo "ERROR: Something went wrong" >&2
  ```
* **Use consistent log prefixes**:
  Prefix log messages with severity level (e.g., `ERROR:`, `WARN:`, `INFO:`).

## Exit Codes

* **Use meaningful exit codes**:
  - `0`: Success
  - `1`: General error
  - `2`: Misuse of shell command
* **Exit with appropriate code**:
  Always exit with a code that reflects the script's success or failure.

## Main Function Pattern

* **Use a main function**:
  Structure scripts with a `main` function that coordinates execution:
  ```bash
  function main() {
    # Main script logic
  }

  main "${@}"
  ```

## Portability

* **Avoid bash-specific features when possible**:
  If the script needs to be POSIX-compliant, avoid bash-specific features.
* **Test on target platforms**:
  Ensure scripts work on all intended platforms (Linux, macOS, WSL, etc.).
