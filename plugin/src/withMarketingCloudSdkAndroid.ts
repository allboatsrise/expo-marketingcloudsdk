import { AndroidConfig, ConfigPlugin, withAndroidManifest, withMainApplication, withMainActivity, WarningAggregator } from '@expo/config-plugins';
import { MarketingCloudSdkPluginProps } from './types';

const {
  addMetaDataItemToMainApplication,
  getMainApplicationOrThrow,
} = AndroidConfig.Manifest;

export const withMarketingCloudSdkAndroid: ConfigPlugin<MarketingCloudSdkPluginProps> = (config, props) => {
  config = withAndroidManifest(config, (config) => {
    
    const mainApplication = getMainApplicationOrThrow(config.modResults);
    addMetaDataItemToMainApplication(mainApplication, 'com.sfmc.appId', props.appId);
    addMetaDataItemToMainApplication(mainApplication, 'com.sfmc.accessToken', props.accessToken);
    addMetaDataItemToMainApplication(mainApplication, 'com.sfmc.serverUrl', props.serverUrl);

    return config;
  });

  
  WarningAggregator.addWarningAndroid(
    'expo-marketingcloudsdk',
    JSON.stringify(withMainApplication, undefined, 2)
  );

  config = withMainActivity(config, (config) => {
    console.log(config);
    return config;
  });


  return config;
};
