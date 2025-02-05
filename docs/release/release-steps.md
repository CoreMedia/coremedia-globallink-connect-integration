# Release Steps

--------------------------------------------------------------------------------

\[[Up](README.md)\] \[[Top](#release-steps)\]

--------------------------------------------------------------------------------

In here you will find a description of steps to perform a release of this
workspace.

## 1. Development Workflow

Assuming all branches (main, develop) already exist, proceed as follows (example
here: update GCC REST Client and CMCC version; assuming that `origin/main` is
the default branch):

```bash
$ git clone \
  https://github.com/CoreMedia/coremedia-globallink-connect-integration.git gcc
$ cd gcc
$ git switch --create --track "gcc-update-x.y.z" "origin/develop"
# ... perform required updates ...
$ git commit --all --message="Update GCC REST Client to x.y.z"
# ... perform required updates ...
$ git push --set-upstream origin "gcc-update-x.y.z"
```

**TL;DR**: Create a feature branch from `develop`, update GCC REST Client and
CMCC version, commit changes, and push the branch to the remote repository.

Next, create a pull request from the feature branch to `develop` and wait for
the approval.

See
[CoreMedia/coremedia-globallink-connect-integration](https://github.com/CoreMedia/coremedia-globallink-connect-integration)
for the current, and most recent version of GCC REST Client (labels on top of
page). 

## 2. Verify Release Versions of Core Packages

If the release of this adapter targets a newer CMCC release, make sure that the
versions mentioned in the `package.json` files of the Studio Client and 
the `gcc-workflow-server-parent` Maven module match.

## 3. Documentation Update

* Ensure you have built the CMCC version (snapshot versions) which this
  workspace dedicates to. Otherwise, the third-party versions won't match the
  declared CMCC version (most third-party dependencies) are managed in Blueprint
  and CMCC Core.

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

Follow the test steps as described in [Manual Test Steps](manual-test-steps.md), starting with
running `DefaultGCExchangeFacadeContractTest`, as it provides data, that are
meant to be reviewed during the manual tests.

## 5. Create Tag for the Release

Now that you have updated the documentation, the third-party reports, the 
changelog, and the version badges, you can proceed with creating the tag.

```bash
$ git checkout main
$ git merge "origin/develop"
$ git push origin main
$ git tag "v2406.1.0-1"
$ git push origin "v2406.1.0-1"
```

Alternatively, for a more transparent review process, create a PR from `develop`
to `main`. In this case, you can skip the first three steps. The reviewer gets
an overview of the recent changes on GitHub, and the PR can be directly merged
after approval.

## 6. Create GitHub Release

* Create a GitHub release from the tag, and the copy the changelog entries to
  the release description. Please use the same pattern for release title as the
  previous releases.
* Review GitHub issues and possibly adjust state.

## Sketch: Incorporating Pull Requests for Given CMCC Versions

If customers want to provide a patch for a given workspace version, create
a branch from the given tag. This will then receive the PR results. If
applicable to current `develop` branch, cherry-pick the PR commits to
`develop` branch.

--------------------------------------------------------------------------------

\[[Up](README.md)\] \[[Top](#release-steps)\]

--------------------------------------------------------------------------------
