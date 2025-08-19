import {themes as prismThemes} from 'prism-react-renderer';
import type {Config} from '@docusaurus/types';
import type * as Preset from '@docusaurus/preset-classic';
import type {Options as DocsOptions} from '@docusaurus/plugin-content-docs';

// This runs in Node.js - Don't use client-side code here (browser APIs, JSX...)

const config: Config = {
  title: 'coremedia-globallink-connect-integration',
  tagline: 'Translation integration via GlobalLink Connect Cloud',
  favicon: 'img/favicon-picture--thumbnail.ico',

  headTags: [
    {
      tagName: 'link',
      attributes: {
        rel: 'apple-touch-icon',
        sizes: '180x180',
        href: 'img/favicon-apple-touch-picture--thumbnail.png',
      },
    },
    {
      tagName: 'link',
      attributes: {
        rel: 'mask-icon',
        color: '#672779',
        href: 'img/favicon-safari-picture--thumbnail.svg',
      },
    },
    {
      tagName: 'link',
      attributes: {
        rel: 'icon',
        type: 'image/png',
        sizes: '16x16',
        href: 'img/favicon-16x16-picture--thumbnail.png',
      },
    },
    {
      tagName: 'link',
      attributes: {
        rel: 'icon',
        type: 'image/png',
        sizes: '32x32',
        href: 'img/favicon-32x32-picture--thumbnail.png',
      },
    },
    {
      tagName: 'link',
      attributes: {
        rel: 'shortcut icon',
        href: 'img/favicon-picture--thumbnail.ico',
      },
    },
    {
      tagName: 'meta',
      attributes: {
        name: 'msapplication-TileImage',
        content: 'img/favicon-mstile-picture--thumbnail.png',
      },
    },
    {
      tagName: 'meta',
      attributes: {
        name: 'msapplication-TileColor',
        content: '#672779',
      },
    },
  ],

  // Future flags, see https://docusaurus.io/docs/api/docusaurus-config#future
  future: {
    v4: true, // Improve compatibility with the upcoming Docusaurus v4
  },

  // Set the production url of your site here
  url: 'https://coremedia.github.io/',
  // Set the /<baseUrl>/ pathname under which your site is served
  // For GitHub pages deployment, it is often '/<projectName>/'
  baseUrl: '/coremedia-globallink-connect-integration/',

  // GitHub pages deployment config.
  organizationName: 'CoreMedia',
  projectName: 'coremedia-globallink-connect-integration',
  deploymentBranch: 'gh-pages',
  // Follow advice, that `trailingSlash` must be set (to either `true` or
  // `false` for GitHub pages deployment.)
  trailingSlash: false,

  onBrokenLinks: 'throw',
  onBrokenMarkdownLinks: 'warn',

  // Even if you don't use internationalization, you can use this field to set
  // useful metadata like html lang. For example, if your site is Chinese, you
  // may want to replace "en" with "zh-Hans".
  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },

  markdown: {
    mermaid: true,
  },
  themes: ['@docusaurus/theme-mermaid'],

  presets: [
    [
      'classic',
      {
        docs: {
          routeBasePath: '/', // Serve the docs at the site's root
          sidebarPath: './sidebars.ts',
          editUrl:
            'https://github.com/CoreMedia/coremedia-globallink-connect-integration/tree/main/website/',
          // remark-file-list: We need to add this before the default plugins,
          // so that we benefit from dynamic asset links being created.
          beforeDefaultRemarkPlugins: [
            [require('./src/remark/remark-file-list.ts'), {}]
          ],
        },
        blog: false, // Disable blog feature
        theme: {
          customCss: './src/css/custom.css',
        },
      } satisfies Preset.Options,
    ],
  ],

  plugins: [
    [
      'content-docs',
      {
        id: 'dev',
        path: 'dev',
        routeBasePath: 'dev',
        sidebarPath: './sidebarsDev.ts',
        // Please change this to your repo.
        editUrl:
            'https://github.com/CoreMedia/coremedia-globallink-connect-integration/tree/main/website/',
      } satisfies DocsOptions,
    ]
  ],

  themeConfig: {
    // Replace with your project's social card
    // image: 'img/docusaurus-social-card.jpg',
    navbar: {
      title: 'coremedia-globallink-connect-integration',
      logo: {
        alt: 'CoreMedia Logo',
        src: 'img/logo.svg',
        srcDark: 'img/logo-dark.svg',
        href: 'https://www.coremedia.com/',
      },
      items: [
        {
          type: 'doc',
          position: 'left',
          docId: 'introduction',
          label: 'Docs',
        },
        {
          to: '/dev/home',
          label: 'Contributors',
          position: 'left',
          activeBaseRegex: `/dev/`,
        },
        {
          type: 'docsVersionDropdown',
          position: 'right',
        },
        {
          href: 'https://github.com/CoreMedia/coremedia-globallink-connect-integration',
          label: 'GitHub',
          position: 'right',
        },
      ],
    },
    footer: {
      style: 'dark',
      links: [
        {
          title: 'About CoreMedia',
          items: [
            {
              label: 'Homepage',
              href: 'https://www.coremedia.com/',
            },
            {
              label: 'Blog',
              href: 'https://www.coremedia.com/blog',
            },
          ],
        },
        {
          title: 'Customer Support',
          items: [
            {
              label: 'Support',
              href: 'https://www.coremedia.com/support',
            },
            {
              label: 'CoreMedia Help Center',
              href: 'https://support.coremedia.com/hc/en-us',
            },
          ],
        },
        {
          title: 'More',
          items: [
            {
              label: 'Docusaurus',
              href: 'https://docusaurus.io/',
            },
            {
              label: 'GitHub',
              href: 'https://github.com/CoreMedia/coremedia-globallink-connect-integration',
            },
          ],
        },
      ],
      copyright: `Copyright © ${new Date().getFullYear()} CoreMedia GmbH, CoreMedia Corporation. Built with Docusaurus.`,
    },
    prism: {
      theme: prismThemes.github,
      darkTheme: prismThemes.dracula,
    },
  } satisfies Preset.ThemeConfig,
};

export default config;
