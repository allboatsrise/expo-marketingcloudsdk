#import <MarketingCloudSDK/MarketingCloudSDK.h>
#import <EXMarketingCloudSdk/EXMarketingCloudSdk.h>
#import <ExpoModulesCore/EXDefines.h>
#import <EXNotifications/EXNotificationCenterDelegate.h>

@interface EXMarketingCloudSdk ()

@property (nonatomic, weak) id<EXNotificationCenterDelegate> notificationCenterDelegate;

@end

@implementation EXMarketingCloudSdk

EX_REGISTER_SINGLETON_MODULE(MarketingCloud);

# pragma mark - EXModuleRegistryConsumer

- (void)setModuleRegistry:(EXModuleRegistry *)moduleRegistry
{
  _notificationCenterDelegate = [moduleRegistry getSingletonModuleForName:@"NotificationCenterDelegate"];
}

# pragma mark - UIApplicationDelegate

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary<UIApplicationLaunchOptionsKey,id> *)launchOptions
{
  NSString *sfmcAccessToken = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"SFMCAccessToken"];
  NSNumber *sfmcAnalyticsEnabled = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"SFMCAnalyticsEnabled"];
  NSString *sfmcAppId = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"SFMCApplicationId"];
  NSNumber *sfmcAppControlsBadging = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"SFMCApplicationControlsBadging"];
  NSNumber *sfmcDelayRegistrationUntilContactKeyIsSet = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"SFMCDelayRegistrationUntilContactKeyIsSet"];
  NSNumber *sfmcInboxEnabled = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"SFMCInboxEnabled"];
  NSNumber *sfmcLocationEnabled = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"SFMCLocationEnabled"];
  NSString *sfmcMid = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"SFMCMid"];
  NSString *sfmcServerUrl = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"SFMCServerUrl"];

  MarketingCloudSDKConfigBuilder *mcsdkBuilder = [MarketingCloudSDKConfigBuilder new];
  [mcsdkBuilder sfmc_setAccessToken:sfmcAccessToken];
  [mcsdkBuilder sfmc_setAnalyticsEnabled:sfmcAnalyticsEnabled];
  [mcsdkBuilder sfmc_setApplicationId:sfmcAppId];
  [mcsdkBuilder sfmc_setApplicationControlsBadging:sfmcAppControlsBadging];
  [mcsdkBuilder sfmc_setDelayRegistrationUntilContactKeyIsSet:sfmcDelayRegistrationUntilContactKeyIsSet];
  [mcsdkBuilder sfmc_setInboxEnabled:sfmcInboxEnabled];
  [mcsdkBuilder sfmc_setLocationEnabled:sfmcLocationEnabled];
  if ([sfmcMid length] > 0) [mcsdkBuilder sfmc_setMid:sfmcMid];
  [mcsdkBuilder sfmc_setMarketingCloudServerUrl:sfmcServerUrl];

  NSError *error = nil;
  BOOL success = [[MarketingCloudSDK sharedInstance] sfmc_configureWithDictionary:[mcsdkBuilder sfmc_build] error:&error];

  if (success != YES) {
    EXLogError(@"[expo-marketingcloudsdk] Failed to initialize instance. (error: %@)", error);
  } else {
    [_notificationCenterDelegate addDelegate:self];
  }
  
  return YES;
}

// MobilePush SDK: REQUIRED IMPLEMENTATION
- (void)application:(UIApplication *)application didRegisterForRemoteNotificationsWithDeviceToken:(NSData *)deviceToken
{
  [[MarketingCloudSDK sharedInstance] sfmc_setDeviceToken:deviceToken];
}

//# pragma mark - EXNotificationsDelegate

// MobilePush SDK: REQUIRED IMPLEMENTATION
/** This delegate method offers an opportunity for applications with the "remote-notification" background mode to fetch appropriate new data in response to an incoming remote notification. You should call the fetchCompletionHandler as soon as you're finished performing that operation, so the system can accurately estimate its power and data cost.
 This method will be invoked even if the application was launched or resumed because of the remote notification. The respective delegate methods will be invoked first. Note that this behavior is in contrast to application:didReceiveRemoteNotification:, which is not called in those cases, and which will not be invoked if this method is implemented. **/
- (void)application:(UIApplication *)application didReceiveRemoteNotification:(NSDictionary *)userInfo fetchCompletionHandler:(void (^)(UIBackgroundFetchResult))completionHandler
{
  [[MarketingCloudSDK sharedInstance] sfmc_setNotificationUserInfo:userInfo];
  completionHandler(UIBackgroundFetchResultNewData);
}

@end
