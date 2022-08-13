#import <MarketingCloudSDK/MarketingCloudSDK.h>
#import <EXMarketingCloudSdk/EXMarketingCloudSdk.h>
#import <ExpoModulesCore/EXDefines.h>

@interface EXMarketingCloudSdk ()

@property (nonatomic, weak) EXModuleRegistry *moduleRegistry;

@end

@implementation EXMarketingCloudSdk

EX_REGISTER_SINGLETON_MODULE(MarketingCloud);

# pragma mark - EXModuleRegistryConsumer

- (void)setModuleRegistry:(EXModuleRegistry *)moduleRegistry
{
  _moduleRegistry = moduleRegistry;
}

# pragma mark - UIApplicationDelegate

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary<UIApplicationLaunchOptionsKey,id> *)launchOptions
{
  NSString *sfmcApplicationId = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"SFMCApplicationId"];
  NSString *sfmcAccessToken = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"SFMCAccessToken"];
  NSNumber *sfmcAnalyticsEnabled = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"SFMCAnalyticsEnabled"];
  NSString *sfmcServerUrl = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"SFMCServerUrl"];

  MarketingCloudSDKConfigBuilder *mcsdkBuilder = [MarketingCloudSDKConfigBuilder new];
  [mcsdkBuilder sfmc_setApplicationId:sfmcApplicationId];
  [mcsdkBuilder sfmc_setAccessToken:sfmcAccessToken];
  [mcsdkBuilder sfmc_setAnalyticsEnabled:sfmcAnalyticsEnabled];
  [mcsdkBuilder sfmc_setMarketingCloudServerUrl:sfmcServerUrl];

  NSError *error = nil;
  BOOL success = [[MarketingCloudSDK sharedInstance] sfmc_configureWithDictionary:[mcsdkBuilder sfmc_build] error:&error];

  if (success == YES) {
        dispatch_async(dispatch_get_main_queue(), ^{
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
        });
    } else {
        //  MarketingCloudSDK sfmc_configure failed
        os_log_debug(OS_LOG_DEFAULT, "MarketingCloudSDK sfmc_configure failed with error = %@",
                     error);
    }
  return YES;
}

- (void)application:(UIApplication *)application didReceiveRemoteNotification:(NSDictionary *)userInfo fetchCompletionHandler:(void (^)(UIBackgroundFetchResult))completionHandler
{
  // This method is REQUIRED for correct functionality of the SDK.
  // This method will be called on the delegate when the application receives a silent push
  [[MarketingCloudSDK sharedInstance] sfmc_setNotificationUserInfo:userInfo];
  completionHandler(UIBackgroundFetchResultNewData);
}

# pragma mark - UNUserNotificationCenterDelegate

- (void)userNotificationCenter:(UNUserNotificationCenter *)center willPresentNotification:(UNNotification *)notification withCompletionHandler:(void (^)(UNNotificationPresentationOptions))completionHandler
{
  NSLog(@"User Info : %@", notification.request.content.userInfo);
  completionHandler(UNAuthorizationOptionSound | UNAuthorizationOptionAlert | UNAuthorizationOptionBadge);
}

// The method will be called on the delegate when the user responded to the notification by opening
// the application, dismissing the notification or choosing a UNNotificationAction. The delegate
// must be set before the application returns from applicationDidFinishLaunching:.
- (void)userNotificationCenter:(UNUserNotificationCenter *)center didReceiveNotificationResponse:(UNNotificationResponse *)response withCompletionHandler:(void (^)(void))completionHandler
{
  // tell the MarketingCloudSDK about the notification
  [[MarketingCloudSDK sharedInstance] sfmc_setNotificationRequest:response.notification.request];

  if (completionHandler != nil) {
      completionHandler();
  }
}

- (void)userNotificationCenter:(UNUserNotificationCenter *)center openSettingsForNotification:(UNNotification *)notification
{
}

@end
