import {
  ConfigPlugin,
  withInfoPlist,
} from '@expo/config-plugins';

import { MarketingCloudSdkPluginProps } from './types';

export const withMarketingCloudSdkIOS: ConfigPlugin<MarketingCloudSdkPluginProps> = (
  config,
  props
) => {
  config = withInfoPlist(config, (config) => {
    config.modResults['SFMC_APP_ID'] = props.appId;
    config.modResults['SFMC_ACCESS_TOKEN'] = props.accessToken;
    config.modResults['SFMC_SERVER_URL'] = props.serverUrl;
    if (typeof props.analyticsEnabled === 'boolean') config.modResults['SFMC_ANALYTICS_ENABLED'] = props.analyticsEnabled ? 1 : 0;
    if (typeof props.inboxEnabled === 'boolean') config.modResults['SFMC_INBOX_ENABLED'] = props.inboxEnabled ? 1 : 0;
    if (typeof props.locationEnabled === 'boolean') config.modResults['SFMC_LOCATION_ENABLED'] = props.locationEnabled ? 1 : 0;
    if (props.mid) config.modResults['SFMC_MID'] = props.mid;
    if (props.senderId) config.modResults['SFMC_SENDER_ID'] = props.senderId;
    return config;
  })

  return config;
};
