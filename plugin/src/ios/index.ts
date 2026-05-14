import {
  ConfigPlugin,
  withInfoPlist,
  withDangerousMod,
  withXcodeProject,
} from '@expo/config-plugins';

import { MarketingCloudSdkPluginValidProps } from '../types';

import * as fs from 'fs';
import * as path from 'path';
import { FileManager } from '../support/FileManager';
import NseUpdaterManager from '../support/NseUpdaterManager';

const targetName = 'ExpoMarketingCloudSdkNSE';
const targetFiles = ['NotificationService.swift', `${targetName}-Info.plist`];

import getEasManagedCredentialsConfigExtra from '../support/eas/getEasManagedCredentialsConfigExtra';
import { ExpoConfig } from '@expo/config-types';

export const withIOSConfig: ConfigPlugin<MarketingCloudSdkPluginValidProps> = (
  config,
  props
) => {
  config = withInfo(config, props)
  config = withRemoteNotificationsBackgroundMode(config, props)
  if (props.shouldCreateServiceExtension && props.iosDevTeamId) {
    config = withServiceExtension(config, props);
    config = withXcodeProjectExtension(config, props);
    config = withEasManagedCredentials(config, props);
  } else if (props.shouldCreateServiceExtension) {
    console.warn('[expo-marketingcloudsdk] iosDevTeamId is required when shouldCreateServiceExtension is true. Extension not created.');
  }
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
};

const withServiceExtension: ConfigPlugin<MarketingCloudSdkPluginValidProps> = (config, props) => {
  const pluginDir = require.resolve('@allboatsrise/expo-marketingcloudsdk/package.json');
  const sourceDir = path.join(pluginDir, '../plugin/build/support/extension-files/');
  return withDangerousMod(config, [
    'ios',
    async config => {
      const iosPath = path.join(config.modRequest.projectRoot, 'ios');
      fs.mkdirSync(`${iosPath}/${targetName}`, {recursive: true});

      for (let i = 0; i < targetFiles.length; i++) {
        const extFile = targetFiles[i];
        const targetFile = `${iosPath}/${targetName}/${extFile}`;
        await FileManager.copyFile(`${sourceDir}${extFile}`, targetFile);
      }
      const nseUpdater = new NseUpdaterManager({iosPath, targetName, plistFileName: `${targetName}-Info.plist`});
      await nseUpdater.updateNSEBundleVersion(config.ios?.buildNumber ?? '1');
      await nseUpdater.updateNSEBundleShortVersion(config?.version ?? '1.0.0');

      return config;
    },
  ]);
};

const withXcodeProjectExtension: ConfigPlugin<MarketingCloudSdkPluginValidProps> = (
  config,
  props,
) => {
  return withXcodeProject(config, newConfig => {
    const xcodeProject = newConfig.modResults;

    if (xcodeProject.pbxTargetByName(targetName)) {
      return newConfig;
    }

    // Create new PBXGroup for the extension
    const extGroup = xcodeProject.addPbxGroup([...targetFiles], targetName, targetName);

    // Add the new PBXGroup to the top level group. This makes the
    // files / folder appear in the file explorer in Xcode.
    const groups = xcodeProject.hash.project.objects['PBXGroup'];
    Object.keys(groups).forEach(function (key) {
      if (
        typeof groups[key] === 'object' &&
        groups[key].name === undefined &&
        groups[key].path === undefined
      ) {
        xcodeProject.addToPbxGroup(extGroup.uuid, key);
      }
    });

    // WORK AROUND for codeProject.addTarget BUG
    // Xcode projects don't contain these if there is only one target
    // An upstream fix should be made to the code referenced in this link:
    //   - https://github.com/apache/cordova-node-xcode/blob/8b98cabc5978359db88dc9ff2d4c015cba40f150/lib/pbxProject.js#L860
    const projObjects = xcodeProject.hash.project.objects;
    projObjects['PBXTargetDependency'] = projObjects['PBXTargetDependency'] || {};
    projObjects['PBXContainerItemProxy'] = projObjects['PBXTargetDependency'] || {};

    // Add the NSE target
    // This adds PBXTargetDependency and PBXContainerItemProxy for you
    const nseTarget = xcodeProject.addTarget(
      targetName,
      'app_extension',
      targetName,
      `${config.ios?.bundleIdentifier}.${props.iosNseBundleIdSuffix ?? targetName}`,
    );

    // Add build phases to the new target
    xcodeProject.addBuildPhase(
      ['NotificationService.swift'],
      'PBXSourcesBuildPhase',
      'Sources',
      nseTarget.uuid,
    );

    xcodeProject.addBuildPhase([], 'PBXResourcesBuildPhase', 'Resources', nseTarget.uuid);
    xcodeProject.addBuildPhase([], 'PBXFrameworksBuildPhase', 'Frameworks', nseTarget.uuid);

    const configurations = xcodeProject.pbxXCBuildConfigurationSection();
    for (const key in configurations) {
      if (
        typeof configurations[key].buildSettings !== 'undefined' &&
        configurations[key].buildSettings.PRODUCT_NAME === `"${targetName}"`
      ) {
        const buildSettingsObj = configurations[key].buildSettings;
        buildSettingsObj.SWIFT_VERSION = '5.4';
        buildSettingsObj.DEVELOPMENT_TEAM = props?.iosDevTeamId;
        buildSettingsObj.TARGETED_DEVICE_FAMILY = `"1,2"`;
        buildSettingsObj.CODE_SIGN_STYLE = 'Automatic';
      }
    }
    xcodeProject.addTargetAttribute('DevelopmentTeam', props?.iosDevTeamId, nseTarget);
    xcodeProject.addTargetAttribute('DevelopmentTeam', props?.iosDevTeamId);
    return newConfig;
  });
};

const withEasManagedCredentials: ConfigPlugin<MarketingCloudSdkPluginValidProps> = (
  config,
  props,
) => {
  config.extra = getEasManagedCredentialsConfigExtra(
    {
      config: config as ExpoConfig,
      targetName,
      bundleSuffix: props?.iosNseBundleIdSuffix,
    }
  );
  return config;
};
