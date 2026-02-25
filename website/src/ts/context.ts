const gitOrg = "CoreMedia";
const gitRepository = "coremedia-globallink-connect-integration";

const cmccVersion = {
  main: "13",
  major: "2512",
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
      url: `${gitRepoUrl}`,
      resolve: (path: string) => `${gitRepoUrl}/blob/${gitVersionTag}/${path}`
    },
    version: {
      tag: `${gitVersionTag}`,
      url: `${gitRepoVersionUrl}`
    },
  },
  cmcc: {
    version: {
      ...cmccVersion,
      short: `${cmccVersion.major}.${cmccVersion.minor}`,
      long: `${cmccVersion.major}.${cmccVersion.minor}.${cmccVersion.patch}`,
      full: `${gitVersionTag}`
    }
  }
};
