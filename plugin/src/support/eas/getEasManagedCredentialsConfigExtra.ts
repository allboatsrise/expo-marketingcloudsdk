import {ExpoConfig} from '@expo/config-types';

interface Params {
  config: ExpoConfig;
  targetName: string;
  bundleSuffix?: string;
}

export default function getEasManagedCredentialsConfigExtra({ config, targetName, bundleSuffix}: Params): {
  [k: string]: any;
} {
  return {
    ...config.extra,
    eas: {
      ...config.extra?.eas,
      build: {
        ...config.extra?.eas?.build,
        experimental: {
          ...config.extra?.eas?.build?.experimental,
          ios: {
            ...config.extra?.eas?.build?.experimental?.ios,
            appExtensions: [
              ...(config.extra?.eas?.build?.experimental?.ios?.appExtensions ?? []),
              {
                // keep in sync with native changes in NSE
                targetName: targetName,
                bundleIdentifier: `${config?.ios?.bundleIdentifier}.${
                  bundleSuffix ?? targetName
                }`,
              },
            ],
          },
        },
      },
    },
  };
}
