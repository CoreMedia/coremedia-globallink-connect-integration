import { sharedModules } from "@coremedia/studio-client.workflow.shared-modules";
import { getPluginWebpackConfig } from "@coremedia/studio-client.build-config";

export default getPluginWebpackConfig({
  name: "workflow_react_gccWorkflowPlugin",
  sharedModules: {
    "@coremedia/studio-client.workflow.shared-modules": sharedModules,
  },
});
