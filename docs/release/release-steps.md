# Release Steps

--------------------------------------------------------------------------------

\[[Up](README.md)\] \[[Top](#top)\]

--------------------------------------------------------------------------------

In here you will find a description of steps to perform a release
of this workspace.

## Documentation Update

* Update [THIRD-PARTY.txt](../THIRD-PARTY.txt) and license downloads by running:

    ```bash
    $ mvn -Pdocs-third-party generate-resources
    ```

* Update CMCC and GCC Version in badges at main workspace `README.md`.
* Update documentation links in [development.md](../development.md) right at
    the bottom of the MarkDown file.

--------------------------------------------------------------------------------

\[[Up](README.md)\] \[[Top](#top)\]

--------------------------------------------------------------------------------
