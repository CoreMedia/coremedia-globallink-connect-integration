export const initPlugin = async () => {
  const module = await import("@coremedia-labs/studio-client.gcc-studio-client");
  await module.addGlobalLinkWorkflowPlugin();
};
