# Release Steps

--------------------------------------------------------------------------------

\[[Up](README.md)\] \[[Top](#top)\]

--------------------------------------------------------------------------------

In here you will find a description of steps to perform a release
of this workspace.

## 1. Development Workflow

Assuming all branches (main, develop, ci/develop) already exist, proceed as
follows (example here: update GCC REST Client and CMCC version; assuming
that `origin/main` is the default branch):

```bash
$ git clone https://github.com/CoreMedia/coremedia-globallink-connect-integration.git gcc
$ cd gcc
$ git checkout --track "origin/develop"
# ... perform required updates ...
$ git commit --all --message="Update GCC REST Client to x.y.z"
# ... perform required updates ...
$ git commit --all --message="Update CMCC 1910.1"
$ git push origin develop 

### For the following steps, it is recommended to use the provided script
### `./sh/update-ci-develop.sh` instead. Also, a GitHub Action is available
### to automate this process.

$ git checkout --track "origin/ci/develop"
$ git rebase "origin/develop"
# ... resolve possible conflicts ...
$ git push origin "ci/develop" --force-with-lease

### It is recommended to leave "ci/develop" immediately, as no other commits
### must make it to this branch than those required to run it in CoreMedia CI!

$ git checkout develop
```

See https://github.com/CoreMedia/coremedia-globallink-connect-integration for current and most recent version of GCC REST Client (labels on top of page). 

## 2. Verify Release Versions of Core Packages

If the release of this adapter targets a newer CMCC release, make sure that the
versions mentioned in the `package.json` files of the Studio Client and 
the `gcc-workflow-server-parent` Maven module match.

## 3. Documentation Update

* Ensure you have built the CMCC version (snapshot versions) which this
    workspace dedicates to. Otherwise, the third-party versions won't
    match the declared CMCC version (most third-party dependencies)
    are managed in Blueprint and CMCC Core.

* Update [THIRD-PARTY.txt](../THIRD-PARTY.txt) and license downloads by running:

    ```bash
    $ mvn -Pdocs-third-party generate-resources
    ```

* Update CMCC and GCC Version in badges at main workspace `README.md`.

* Update documentation links in [development.md](../development.md) right at
    the bottom of the MarkDown file.

* And, do not forget to mention all the changes including upgrade advice in 
  `CHANGELOG.md`

## 4. Manual Testing

Follow the test steps as described in [Manual Test Steps](manual-test-steps.md).

## 5. Create Tag for the Release

Now that you have updated the documentation, the third-party reports, the 
changelog, and the version badges, you can proceed with creating the tag.

```bash
$ git checkout main
$ git merge "origin/develop"
$ git push origin main
$ git tag "v1910.1-1"
$ git push origin "v1910.1-1"
```

Alternatively, for a more transparent review process, create a PR from `develop` to `main`. In this case, you can 
skip the first three steps. The reviewer gets an overview of the recent changes on GitHub, and the PR can be directly 
merged after approval.

## 6. Create GitHub Release

* Create a GitHub release from the tag and the copy the changelog entries to the
  release description. Please use the same pattern for release title as the
  previous releases.
* Review GitHub issues and possibly adjust state.

## * Sketch: Incorporating Pull Requests for Given CMCC Versions

If customers want to provide a patch for a given workspace version, create
a branch from the given tag. This will then receive the PR results. If
applicable to current `develop` branch, cherry-pick the PR commits to
`develop` branch.

If we require a CI, create a branch similar to `ci/develop`, for example
`ci/1907`.

--------------------------------------------------------------------------------

\[[Up](README.md)\] \[[Top](#top)\]

--------------------------------------------------------------------------------
