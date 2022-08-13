import fs from 'fs'
import path from 'path'
import {
  ConfigPlugin,
  withAppDelegate,
  withDangerousMod,
  withEntitlementsPlist,
  withInfoPlist,
} from '@expo/config-plugins';

import { MarketingCloudSdkPluginProps } from '../types';
import { getProjectName } from '@expo/config-plugins/build/ios/utils/Xcodeproj';
import { mergeContents } from '@expo/config-plugins/build/utils/generateCode';

export const withIOSConfig: ConfigPlugin<MarketingCloudSdkPluginProps> = (
  config,
  props
) => {
  config = withEntitlements(config, props)
  config = withInfo(config, props)

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
