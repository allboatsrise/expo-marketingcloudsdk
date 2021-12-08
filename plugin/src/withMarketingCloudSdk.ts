import { ConfigPlugin, createRunOncePlugin } from '@expo/config-plugins';
import { MarketingCloudSdkPluginProps } from './types';

import { withMarketingCloudSdkAndroid } from './withMarketingCloudSdkAndroid';
import { withMarketingCloudSdkIOS } from './withMarketingCloudSdkIOS';

const pkg = require('@allboatsrise/expo-marketingcloudsdk/package.json');

const ERROR_PREFIX = 'Marketing Cloud SDK Plugin:';

const withMarketingCloudSdk: ConfigPlugin<Partial<MarketingCloudSdkPluginProps> | undefined> = (config, props) => {
  if (!props) {
    throw new Error(`${ERROR_PREFIX} Must configure plugin options.`);
  }

  const {serverUrl, appId, accessToken} = props;

  if (!serverUrl) {
    throw new Error(`${ERROR_PREFIX} Must provide server url.`);
  }
  if (!appId) {
    throw new Error(`${ERROR_PREFIX} Must provide app id.`);
  }
  if (!accessToken) {
    throw new Error(`${ERROR_PREFIX} Must provide access token.`);
  }

  config = withMarketingCloudSdkAndroid(config, {...props, serverUrl, appId, accessToken});
  config = withMarketingCloudSdkIOS(config, {...props, serverUrl, appId, accessToken});
  return config;
};

export default createRunOncePlugin(withMarketingCloudSdk, pkg.name, pkg.version);
