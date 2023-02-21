import fs from 'fs';
import path from 'path';
import { ConfigPlugin, createRunOncePlugin } from '@expo/config-plugins';
import { MarketingCloudSdkPluginProps, MarketingCloudSDKPluginPropsSchema } from './types';
import { withAndroidConfig } from './android';
import { withIOSConfig } from './ios';

const ERROR_PREFIX = 'Expo Marketing Cloud SDK Plugin:';

const withMarketingCloudSdk: ConfigPlugin<MarketingCloudSdkPluginProps | unknown> = (config, unsafeProps) => {
  const result = MarketingCloudSDKPluginPropsSchema.safeParse(unsafeProps)

  if (!result.success) {
    throw new Error(`${ERROR_PREFIX} ${result.error.toString()}`);
  }

  const props = result.data
  const {serverUrl, appId, accessToken} = props;

  config = withAndroidConfig(config, {...props, serverUrl, appId, accessToken});
  config = withIOSConfig(config, {...props, serverUrl, appId, accessToken});
  return config;
};

const pkg = JSON.parse(fs.readFileSync(path.join(path.dirname(path.dirname(__dirname)), 'package.json'), 'utf8'));
export default createRunOncePlugin(withMarketingCloudSdk, pkg.name, pkg.version);
