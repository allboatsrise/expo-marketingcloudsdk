import {
  ConfigPlugin,
  withEntitlementsPlist,
  withInfoPlist,
} from '@expo/config-plugins';

import { MarketingCloudSdkPluginProps } from '../types';

export const withIOSConfig: ConfigPlugin<MarketingCloudSdkPluginProps> = (
  config,
  props
) => {
  config = withEntitlements(config, props)
  config = withInfo(config, props)
  config = withRemoteNotificationsBackgroundMode(config, props)

  return config;
};

const withEntitlements: ConfigPlugin<MarketingCloudSdkPluginProps> = (config, {mode = 'development'}) => {
  return withEntitlementsPlist(config, async (config) => {
    config.modResults['aps-environment'] = mode;
    return config;
  });
}

const withInfo: ConfigPlugin<MarketingCloudSdkPluginProps> = (config, props) => {
  return withInfoPlist(config, (config) => {
    config.modResults.SFMCApplicationId = props.appId
    config.modResults.SFMCAccessToken = props.accessToken
    config.modResults.SFMCAnalyticsEnabled = props.analyticsEnabled ?? false
    config.modResults.SFMCServerUrl = props.serverUrl
    return config
  })
}

const withRemoteNotificationsBackgroundMode: ConfigPlugin<MarketingCloudSdkPluginProps> = (config, props) => {
  config = withInfoPlist(config, (config) => {
    if (!Array.isArray(config.modResults.UIBackgroundModes)) {
      config.modResults.UIBackgroundModes = [];
    }
    if (!config.modResults.UIBackgroundModes.includes('remote-notification')) {
      config.modResults.UIBackgroundModes.push('remote-notification');
    }
    return config;
  });

  return config;
}
