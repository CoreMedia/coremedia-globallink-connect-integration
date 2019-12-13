# Release Steps

--------------------------------------------------------------------------------

\[[Up](README.md)\] \[[Top](#top)\]

--------------------------------------------------------------------------------

In here you will find a description of steps to perform a release
of this workspace.

## Quick Path

Assuming all branches (master, develop, ci/develop) already exist, proceed as
follows (example here: update GCC REST Client and CMCC version; assuming
that `origin/master` is the default branch):

```bash
$ git clone https://github.com/CoreMedia/coremedia-globallink-connect-integration.git gcc
$ cd gcc
$ git checkout --track "origin/develop"
# ... perform required updates ...
$ git commit --all --message="Update GCC REST Client to x.y.z"
# ... perform required updates ...
$ git commit --all --message="Update CMCC 1910.1"
$ git push origin develop 
$ git checkout --track "origin/ci/develop"
$ git rebase "origin/develop"
$ git push origin "ci/develop" --force-with-lease

### It is recommended to leave "ci/develop" immediately, as no other commits
### must make it to this branch than those required to run it in CoreMedia CI!

$ git checkout develop
```

Prior to release, ensure you update documentation links and third-party reports
([see below](#documentation-update)) and to adapt the `CHANGELOG.md`.

```bash
$ git checkout master
$ git merge "origin/develop"
$ git push origin master
$ git tag "1910.1-1"
$ git push origin "1910.1-1"
```

## Branches

![Branch Model](../img/branch-model.png)

* **master:** Will be initially used to create `develop` branch. Afterwards,
    it will just be used to merge changes from `develop` branch to `master`,
    i.e., it will just be recipient afterwards. On _release_ the master merge
    commit will be tagged. See below for details on tagging.

* **develop:** After initial creation, all development by CoreMedia and
    merging pull request will happen here.

* **ci/develop:** An artificial branch required for CoreMedia CI systems. It is
    required, as for CoreMedia CI we need to change the parent POMs in that way,
    that we set the version to `9999.9` and add a relative path, so that
    it matches our workspace setup.
    
    As soon as changes from `develop` shall be published to CI, we rebase
    the adaptions:
    
    ```bash
    $ git checkout "ci/develop"
    $ git rebase "origin/develop"
    $ git push --force-with-lease
    ```

## Tags

The structure of tags is as follows:

```text
<CMCC Version>-<GlobalLink Workspace Version>
```

Thus, `1907.1-1` signals compatibility with CMCC 1907.1 and is the first
version of this GlobalLink workspace. `1907.1-2` is a patch version for
version `1907.1-1`, which is based on the same CMCC version, but for example
contains bug fixes.

## Sketch: Incorporating Pull Requests for Given CMCC Versions

If customers want to provide a patch for a given workspace version, create
a branch from the given tag. This will then receive the PR results. If
applicable to current `develop` branch, cherry-pick the PR commits to
`develop` branch.

If we require a CI, create a branch similar to `ci/develop`, for example
`ci/1907`.

## Documentation Update

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

## Post Process

* Review GitHub issues and possibly adjust state.
* Close and possibly rename the milestone.

--------------------------------------------------------------------------------

\[[Up](README.md)\] \[[Top](#top)\]

--------------------------------------------------------------------------------
