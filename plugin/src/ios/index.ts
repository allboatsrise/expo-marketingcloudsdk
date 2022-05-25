import fs from 'fs'
import path from 'path'
import {
  ConfigPlugin,
  withAppDelegate,
  withDangerousMod,
  withEntitlementsPlist,
} from '@expo/config-plugins';

import { MarketingCloudSdkPluginProps } from '../types';
import { getProjectName } from '@expo/config-plugins/build/ios/utils/Xcodeproj';
import { mergeContents } from '@expo/config-plugins/build/utils/generateCode';

export const withIOSConfig: ConfigPlugin<MarketingCloudSdkPluginProps> = (
  config,
  props
) => {
  config = withEntitlements(config, props)

  config = withExtraAppDelegateProtocols(config, props)

  config = withConfiguration(config, props)
  config = withPushConfiguration(config, props)
  config = withDelegateImplementation(config, props)

  return config;
};

const withEntitlements: ConfigPlugin<MarketingCloudSdkPluginProps> = (config, {mode = 'development'}) => {
  return withEntitlementsPlist(config, async (config) => {
    config.modResults['aps-environment'] = mode;
    return config;
  });
}

const withExtraAppDelegateProtocols: ConfigPlugin<MarketingCloudSdkPluginProps> = (config) => {
  return withDangerousMod(config, [
    'ios',
    async (config) => {
      const appDelegateHeaderFile = path.join(config.modRequest.platformProjectRoot, getProjectName(config.modRequest.projectRoot), 'AppDelegate.h')

      let headerContent = fs.readFileSync(appDelegateHeaderFile, 'utf8');

      ['UIApplicationDelegate', 'UNUserNotificationCenterDelegate'].forEach(protocol => {
        if (headerContent.match(new RegExp(`${protocol}`))) return config

        headerContent = headerContent.replace(/(EXAppDelegateWrapper\s*<.*?RCTBridgeDelegate.*?)(>)/, `$1, ${protocol}$2`)  
      })

      fs.writeFileSync(appDelegateHeaderFile, headerContent)

      return config
    }
  ])
}

const withConfiguration: ConfigPlugin<MarketingCloudSdkPluginProps> = (config, props) => {
  return withAppDelegate(config, async config => {
    config.modResults.contents = mergeContents({
      src: config.modResults.contents,
      newSrc: `#import <MarketingCloudSDK/MarketingCloudSDK.h>`,
      anchor: /#import "AppDelegate\.h"/,
      offset: 1,
      tag: '@allboatsrise/expo-marketingcloudsdk(header)',
      comment: '//'
    }).contents

    config.modResults.contents = mergeContents({
      src: config.modResults.contents,
      newSrc: `  MarketingCloudSDKConfigBuilder *mcsdkBuilder = [MarketingCloudSDKConfigBuilder new];
    [mcsdkBuilder sfmc_setApplicationId:@${JSON.stringify(props.appId)}];
    [mcsdkBuilder sfmc_setAccessToken:@${JSON.stringify(props.accessToken)}];
    [mcsdkBuilder sfmc_setAnalyticsEnabled:@(${props.analyticsEnabled ? 'YES' : 'NO'})];
    [mcsdkBuilder sfmc_setMarketingCloudServerUrl:@${JSON.stringify(props.serverUrl)}];

    NSError *error = nil;
    BOOL success = [[MarketingCloudSDK sharedInstance] sfmc_configureWithDictionary:[mcsdkBuilder sfmc_build] error:&error];`,
      anchor: /\[super application:application didFinishLaunchingWithOptions:launchOptions\];/,
      offset: -1,
      tag: '@allboatsrise/expo-marketingcloudsdk(configuration)',
      comment: '//'
    }).contents

    return config;
  })
}

const withPushConfiguration: ConfigPlugin<MarketingCloudSdkPluginProps> = (config, props) => {
  return withAppDelegate(config, async config => {
    config.modResults.contents = mergeContents({
      src: config.modResults.contents,
      newSrc: `  if (success == YES) {
        dispatch_async(dispatch_get_main_queue(), ^{
          if (@available(iOS 10, *)) {
              // set the UNUserNotificationCenter delegate - the delegate must be set here in
              // didFinishLaunchingWithOptions
              [UNUserNotificationCenter currentNotificationCenter].delegate = self;
              [[UIApplication sharedApplication] registerForRemoteNotifications];

              [[UNUserNotificationCenter currentNotificationCenter]
                  requestAuthorizationWithOptions:UNAuthorizationOptionAlert |
                                                  UNAuthorizationOptionSound |
                                                  UNAuthorizationOptionBadge
                                completionHandler:^(BOOL granted, NSError *_Nullable error) {
                                  if (error == nil) {
                                      if (granted == YES) {
                                          dispatch_async(dispatch_get_main_queue(), ^{
                                                         });
                                      }
                                  }
                                }];
          } else {
#if __IPHONE_OS_VERSION_MIN_REQUIRED < 100000
              UIUserNotificationSettings *settings = [UIUserNotificationSettings
                  settingsForTypes:UIUserNotificationTypeBadge | UIUserNotificationTypeSound |
                                   UIUserNotificationTypeAlert
                        categories:nil];
              [[UIApplication sharedApplication] registerUserNotificationSettings:settings];
#endif
              [[UIApplication sharedApplication] registerForRemoteNotifications];
          }
        });
    } else {
        //  MarketingCloudSDK sfmc_configure failed
        os_log_debug(OS_LOG_DEFAULT, "MarketingCloudSDK sfmc_configure failed with error = %@",
                     error);
    }`,
      anchor: /\[super application:application didFinishLaunchingWithOptions:launchOptions\];/,
      offset: 1,
      tag: '@allboatsrise/expo-marketingcloudsdk(push-configuration)',
      comment: '//'
    }).contents

    return config;
  })
}

const withDelegateImplementation: ConfigPlugin<MarketingCloudSdkPluginProps> = (config, props) => {
  return withAppDelegate(config, async config => {

    config.modResults.contents = mergeContents({
      src: config.modResults.contents,
      newSrc: `    [[MarketingCloudSDK sharedInstance] sfmc_setDeviceToken:deviceToken];`,
      anchor: /return \[super application:application didRegisterForRemoteNotificationsWithDeviceToken:deviceToken\];/,
      offset: 0,
      tag: '@allboatsrise/expo-marketingcloudsdk(user-notification-method-didRegisterForRemoteNotificationsWithDeviceToken)',
      comment: '//'
    }).contents

    config.modResults.contents = mergeContents({
      src: config.modResults.contents,
      newSrc: `os_log_debug(OS_LOG_DEFAULT, "didFailToRegisterForRemoteNotificationsWithError = %@", error);`,
      anchor: /return \[super application:application didFailToRegisterForRemoteNotificationsWithError:error\];/,
      offset: 0,
      tag: '@allboatsrise/expo-marketingcloudsdk(user-notification-method-didFailToRegisterForRemoteNotificationsWithError)',
      comment: '//'
    }).contents


    config.modResults.contents = mergeContents({
      src: config.modResults.contents,
      newSrc: `
// The method will be called on the delegate when the user responded to the notification by opening
// the application, dismissing the notification or choosing a UNNotificationAction. The delegate
// must be set before the application returns from applicationDidFinishLaunching:.
- (void)userNotificationCenter:(UNUserNotificationCenter *)center
    didReceiveNotificationResponse:(UNNotificationResponse *)response
              withCompletionHandler:(void (^)(void))completionHandler {
    // tell the MarketingCloudSDK about the notification
    [[MarketingCloudSDK sharedInstance] sfmc_setNotificationRequest:response.notification.request];

    if (completionHandler != nil) {
        completionHandler();
    }
}

- (void)userNotificationCenter:(UNUserNotificationCenter *)center
        willPresentNotification:(UNNotification *)notification
          withCompletionHandler:
              (void (^)(UNNotificationPresentationOptions options))completionHandler {
    NSLog(@"User Info : %@", notification.request.content.userInfo);
    completionHandler(UNAuthorizationOptionSound | UNAuthorizationOptionAlert |
                      UNAuthorizationOptionBadge);
}`.trim(),
      anchor: /-\s*\(BOOL\)application:\(UIApplication\s*\*\)application\s*didFinishLaunchingWithOptions/,
      offset: -1,
      tag: '@allboatsrise/expo-marketingcloudsdk(user-notification-methods)',
      comment: '//'
    }).contents

    config.modResults.contents = mergeContents({
      src: config.modResults.contents,
      newSrc: `
    // This method is REQUIRED for correct functionality of the SDK.
    // This method will be called on the delegate when the application receives a silent push
    [[MarketingCloudSDK sharedInstance] sfmc_setNotificationUserInfo:userInfo];
    completionHandler(UIBackgroundFetchResultNewData);
`,
      anchor: /return \[super application:application didReceiveRemoteNotification:userInfo fetchCompletionHandler:completionHandler\]/,
      offset: 0,
      tag: '@allboatsrise/expo-marketingcloudsdk(user-notification-method-didReceiveRemoteNotification)',
      comment: '//'
    }).contents

    return config;
  })
}
