const { sharedModules } = require("@coremedia/studio-client.main.shared-modules");
const { getPluginWebpackConfig } = require("@coremedia/studio-client.build-config");

module.exports = getPluginWebpackConfig({
  name: "main_gccWorkflowPlugin",
  sharedModules: {
    "@coremedia/studio-client.main.shared-modules": sharedModules,
  },
});
