import {
  ConfigPlugin,
  withInfoPlist,
} from '@expo/config-plugins';

import { MarketingCloudSdkPluginValidProps } from '../types';

export const withIOSConfig: ConfigPlugin<MarketingCloudSdkPluginValidProps> = (
  config,
  props
) => {
  config = withInfo(config, props)
  config = withRemoteNotificationsBackgroundMode(config, props)

  return config;
};

const withInfo: ConfigPlugin<MarketingCloudSdkPluginValidProps> = (config, props) => {
  return withInfoPlist(config, (config) => {
    config.modResults.SFMCDebug = props.debug
    config.modResults.SFMCAccessToken = props.accessToken
    config.modResults.SFMCAnalyticsEnabled = props.analyticsEnabled
    config.modResults.SFMCApplicationId = props.appId
    config.modResults.SFMCApplicationControlsBadging = props.applicationControlsBadging
    config.modResults.SFMCDelayRegistrationUntilContactKeyIsSet = props.delayRegistrationUntilContactKeyIsSet
    config.modResults.SFMCInboxEnabled = props.inboxEnabled
    config.modResults.SFMCLocationEnabled = props.locationEnabled
    config.modResults.SFMCMid = props.mid ?? ''
    config.modResults.SFMCServerUrl = props.serverUrl
    config.modResults.SFMCMarkNotificationReadOnInboxNotificationOpen = props.markNotificationReadOnInboxNotificationOpen
    return config
  })
}

const withRemoteNotificationsBackgroundMode: ConfigPlugin<MarketingCloudSdkPluginValidProps> = (config, props) => {
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
