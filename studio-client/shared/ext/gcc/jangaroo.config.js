const { jangarooConfig } = require("@jangaroo/core");

module.exports = jangarooConfig({
  type: "code",
  sencha: {
    name: "com.coremedia.labs.translation.gcc__gcc-studio-client",
    namespace: "com.coremedia.labs.translation.gcc",
  },
  autoLoad: [
    "./src/GccWorkflowPlugin",
  ],
});
