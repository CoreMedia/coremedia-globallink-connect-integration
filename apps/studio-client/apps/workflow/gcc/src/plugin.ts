export const initPlugin = async () => {
  const module = await import("@coremedia-labs/studio-client.ext.gcc-studio-client");
  await module.addGlobalLinkWorkflowPlugin();
};
