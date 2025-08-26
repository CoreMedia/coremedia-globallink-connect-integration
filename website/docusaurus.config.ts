import {themes as prismThemes} from 'prism-react-renderer';
import type {Config} from '@docusaurus/types';
import type * as Preset from '@docusaurus/preset-classic';
import type {Options as DocsOptions} from '@docusaurus/plugin-content-docs';
import { context } from './src/ts/context';
import { NormalizedSidebar } from '@docusaurus/plugin-content-docs/src/sidebars/types.js';
import logger from '@docusaurus/logger';

// This runs in Node.js - Don't use client-side code here (browser APIs, JSX...)

const config: Config = {
  title: `${context.git.repository.name}`,
  titleDelimiter: '–',
  // Set the production url of your site here
  url: `${context.site.url}`,
  // Set the /<baseUrl>/ pathname under which your site is served
  // For GitHub pages deployment, it is often '/<projectName>/'
  baseUrl: `${context.site.context}`,

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

  // GitHub pages deployment config.
  organizationName: `${context.git.repository.organization}`,
  projectName: `${context.git.repository.name}`,
  deploymentBranch: 'gh-pages',
  // Follow advice, that `trailingSlash` must be set (to either `true` or
  // `false` for GitHub pages deployment.)
  trailingSlash: false,

  onBrokenAnchors: 'throw',
  onBrokenLinks: 'throw',
  onBrokenMarkdownLinks: 'throw',
  onDuplicateRoutes: 'throw',

  // Even if you don't use internationalization, you can use this field to set
  // useful metadata like html lang.
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
          editUrl: `${context.site.editUrl}`,
          // file-list: We need to add this before the default plugins,
          // so that we benefit from dynamic asset links being created.
          beforeDefaultRemarkPlugins: [
            [require('./src/remark/file-list.ts'), {}]
          ],
          async sidebarItemsGenerator({defaultSidebarItemsGenerator, ...args}) {
            const items = await defaultSidebarItemsGenerator(args);
            const {categoriesMetadata} = args;

            // Apply reverse sorting to categories with customProps.sort: "descending"
            const processItems = (items: NormalizedSidebar): NormalizedSidebar => {
              return items.map((item) => {
                if (item.type === 'category') {
                  // Try both the exact label and lowercase version for matching
                  const categoryMetadata = categoriesMetadata[item.label] || categoriesMetadata[item.label.toLowerCase()];
                  const shouldReverse = categoryMetadata?.customProps?.sort === 'descending';

                  if (shouldReverse) {
                    logger.info(`Applying descending sort to category: ${item.label}`);
                    return {...item, items: [...item.items].reverse()};
                  }

                  // Recursively process nested categories
                  return {...item, items: processItems(item.items)};
                }
                return item;
              });
            };

            return processItems(items);
          },
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
        editUrl: `${context.site.editUrl}`
      } satisfies DocsOptions,
    ],
  ],

  themeConfig: {
    metadata: [
      {
        name: 'keywords',
        content: 'CoreMedia, GlobalLink, Connect, Integration, Documentation, GCC, Translation, REST',
      },
    ],
    colorMode: {
      defaultMode: 'dark',
      respectPrefersColorScheme: true,
    },
    docs: {
      sidebar: {
        hideable: true,
      },
    },
    navbar: {
      title: 'CoreMedia GlobalLink Connect Cloud Integration',
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
          type: 'html',
          position: 'right',
          value: `<span style="font-weight:var(--ifm-font-weight-semibold);">${context.git.version.tag}</span>`,
        },
        {
          href: `${context.git.repository.url}`,
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
              label: 'Documentation',
              href: 'https://documentation.coremedia.com/',
            },
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
              href: `${context.git.repository.url}`,
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
