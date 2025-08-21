import React from 'react';
import { context } from '../ts/context';

interface RepositoryLinkConfig {
  path: string;
  title?: string;
}
const RepositoryLink = ( config: RepositoryLinkConfig ) => {
  const { path, title = config.path } = config;
  return (
    <a href={context.git.repository.resolve(path)} target="_blank" rel="noopener noreferrer">
      {title}
    </a>
  );
};

export default RepositoryLink;
export type { RepositoryLinkConfig };
