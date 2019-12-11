# Development

--------------------------------------------------------------------------------

\[[Up](README.md)\] \[[Top](#top)\]

--------------------------------------------------------------------------------

## Introduction

In order to start using the GCC Labs Project you have to add the project to
your Blueprint extensions. The following process assumes, that you will add
the GCC extension as GIT submodule to your workspace. You may as well decide
to copy the sources to your workspace.

To summarize the steps below, everything you need to do:

1. Add GCC to your Blueprint workspace at `modules/extensions/gcc`.
2. Configure your extension tool.
3. Run your extension tool to activate the gcc extension.
4. Add `translation-global-link.xml` to your workflow server deployment.
5. Later on: Ensure that your homepages link to `/Settings/Options/Settings/GlobalLink`
    in their linked settings.

## Adding GCC Submodule

In order to add GCC as submodule to Blueprint workspace you will use
`git submodule`. You need to add the submodule for your 
gcc extension under the following path: `modules/extensions/gcc`

## Adding GCC as extension

In order to add the gcc extension to your workspace you need to configure your
extension tool. The configuration for the tool can be found under
`workspace-configuration/extensions`. Make sure that you use at least version
4.0.1 of the extension tool.

Here you need to add the following configuration for the `extensions-maven-plugin`
```
        <configuration>
            <projectRoot>../..</projectRoot>
            <extensionsRoot>modules/extensions</extensionsRoot>
            <extensionPointsPath>modules/extension-config</extensionPointsPath>
        </configuration>
```

After adapting the configuration you may run the commands `mvn extensions:sync`
& `mvn extensions:sync -Denable=gcc` in the `workspace-configuration/extensions`
folder. This will activate the globalLink extension. The extension tool will
also set the relative path for the parents of the extension modules.

## Adding GCC Workflow to Workflow Server Deployment

You need to add `translation-global-link.xml` to your _builtin_ workflow definitions
in `docker/management-tools/src/docker/import-default-workflows`. Using
_builtin_ will make the definition to be read from the JAR.


The first part of the expression is the _address_ where to add the workflow,
the second part just replaces the closing parenthesis with an additional
entry for the workflow definition.

## Patch/Edit Site Homepages

For each master site for which you want to start GlobalLink Translation Workflows
from, you need to add `/Settings/Options/Settings/GlobalLink` settings document
to the linked settings. To patch a homepage in server export you can use
the following SED command:

```bash
sed --in-place --expression \
  "\\#.*<linkedSettings>.*#a <link href=\"../../../../../../Settings/Options/Settings/GlobalLink.xml\" path=\"/Settings/Options/Settings/GlobalLink\"/>" \
  "${HOMEPAGE}"
```

where `${HOMEPAGE}` is the server export XML file of your homepage to patch.

As alternative you may manually edit the corresponding homepages later on
in CoreMedia Studio.

--------------------------------------------------------------------------------

\[[Up](README.md)\] \[[Top](#top)\]

--------------------------------------------------------------------------------
