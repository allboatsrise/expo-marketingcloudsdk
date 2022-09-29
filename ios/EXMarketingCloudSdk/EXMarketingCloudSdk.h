#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import <ExpoModulesCore/EXSingletonModule.h>
#import <ExpoModulesCore/EXModuleRegistryConsumer.h>
#import <EXNotifications/EXNotificationsDelegate.h>

NS_ASSUME_NONNULL_BEGIN

@interface EXMarketingCloudSdk : EXSingletonModule <UIApplicationDelegate, EXModuleRegistryConsumer, EXNotificationsDelegate>

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(nullable NSDictionary<UIApplicationLaunchOptionsKey,id> *)launchOptions;
- (void)application:(UIApplication *)application didRegisterForRemoteNotificationsWithDeviceToken:(NSData *)deviceToken;

@end

NS_ASSUME_NONNULL_END
