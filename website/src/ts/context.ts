const gitOrg = "CoreMedia";
const gitRepository = "coremedia-globallink-connect-integration";

const cmccVersion = {
  main: "12",
  major: "2412",
  minor: "0",
  patch: "0",
};

const cmGccVersion = "1";

const gitVersionTag = `v${cmccVersion.major}.${cmccVersion.minor}.${cmccVersion.patch}-${cmGccVersion}`;

const gitRepoUrl = `https://github.com/${gitOrg}/${gitRepository}`;
const gitRepoMainUrl = `${gitRepoUrl}/tree/main`;
const gitRepoWebsiteEditUrl = `${gitRepoMainUrl}/website/`;
const gitRepoVersionUrl = `${gitRepoMainUrl}/tree/${gitVersionTag}`;

export const context = {
  site: {
    url: 'https://coremedia.github.io/',
    context: `/${gitRepository}/`,
    editUrl: `${gitRepoWebsiteEditUrl}`,
  },
  git: {
    repository: {
      organization: `${gitOrg}`,
      name: `${gitRepository}`,
      url: `${gitRepoUrl}`
    },
    version: {
      tag: `${gitVersionTag}`,
    },
  },
  cmcc: {
    version: {
      short: `${cmccVersion.major}.${cmccVersion.minor}`,
      long: `${cmccVersion.major}.${cmccVersion.minor}.${cmccVersion.patch}`,
      full: `v${cmccVersion.major}.${cmccVersion.minor}.${cmccVersion.patch}-${cmGccVersion}`
    }
  }
};
