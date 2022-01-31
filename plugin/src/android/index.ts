import fs from 'fs'
import { ConfigPlugin, withAppBuildGradle, withMainApplication, withProjectBuildGradle} from '@expo/config-plugins';
import { mergeContents } from '@expo/config-plugins/build/utils/generateCode';
import { MarketingCloudSdkPluginProps } from '../types';
import { getGoogleServicesFilePath } from './helpers';

export const withAndroidConfig: ConfigPlugin<MarketingCloudSdkPluginProps> = (config, props) => {
  // 1. Add Marketing Cloud SDK repository
  config = withConfigureRepository(config, props)

  // 2. Provide FCM credentials
  // Configure manually via Expo's googleServicesFile property.
  // @see https://stackoverflow.com/a/63109187

  // 3. Configure the SDK in your MainApplication.java class
  config = withConfigureMainApplication(config, {...props})

  return config;
};

const withConfigureRepository: ConfigPlugin<MarketingCloudSdkPluginProps> = (config) => {
  config = withProjectBuildGradle(config, async config => {
    
    config.modResults.contents = mergeContents({
      src: config.modResults.contents,
      newSrc: `        maven { url "https://salesforce-marketingcloud.github.io/MarketingCloudSDK-Android/repository" }`,
      anchor: /mavenLocal\(\)/,
      offset: 1,
      tag: '@allboatsrise/expo-marketingcloudsdk(maven:repositories)',
      comment: '//'
    }).contents
    
    return config
  })

  return withAppBuildGradle(config, async config => {
    config.modResults.contents = mergeContents({
      src: config.modResults.contents,
      newSrc: `    implementation 'com.salesforce.marketingcloud:marketingcloudsdk:${'8.0.4'}'`,
      anchor: /dependencies\s?{/,
      offset: 1,
      tag: '@allboatsrise/expo-marketingcloudsdk(maven:dependencies)',
      comment: '//'
    }).contents
    
    return config
  })
}

const withConfigureMainApplication: ConfigPlugin<MarketingCloudSdkPluginProps> = (config, props) => {
  return withMainApplication(config, async config => {

    let senderId = props.senderId

    if (!senderId) {
      const googleServicesFilePath = getGoogleServicesFilePath(config, config.modRequest.projectRoot)
      const googleServices = JSON.parse(fs.readFileSync(googleServicesFilePath, 'utf8'));
      senderId = googleServices?.project_info?.project_number

      if (!senderId) {
        throw new Error(`Failed to extract sender id from google services file. (path: ${googleServicesFilePath})`)
      }
    }

    config.modResults.contents = mergeContents({
      src: config.modResults.contents,
      newSrc: `    MarketingCloudSdk.init(this,
      MarketingCloudConfig.builder()
        .setApplicationId(${JSON.stringify(props.appId)})
        .setAccessToken(${JSON.stringify(props.accessToken)})
        .setSenderId(${JSON.stringify(senderId)})
        .setMarketingCloudServerUrl(${JSON.stringify(props.serverUrl)})
        .setNotificationCustomizationOptions(NotificationCustomizationOptions.create(R.drawable.ic_notification))
        .setAnalyticsEnabled(${props.analyticsEnabled ? 'true' : 'false'})
        .build(this),
      initializationStatus -> Log.e("INIT", initializationStatus.toString())
    );`,
      anchor: /super\.onCreate\(\);/,
      offset: 1,
      tag: '@allboatsrise/expo-marketingcloudsdk(onCreate)',
      comment: '//'
    }).contents

    return config;
  })
}