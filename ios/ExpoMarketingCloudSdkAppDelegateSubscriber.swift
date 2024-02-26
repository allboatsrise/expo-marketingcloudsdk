import ExpoModulesCore
import SFMCSDK
import MarketingCloudSDK

public class ExpoMarketingCloudSdkAppDelegateSubscriber : ExpoAppDelegateSubscriber {
  private var notificationDelegate: ExpoMarketingCloudSdkNotificationsDelegate?
  
  public func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {

    let debug = Bundle.main.object(forInfoDictionaryKey: "SFMCDebug") as? Bool ?? false;
    let accessToken = Bundle.main.object(forInfoDictionaryKey: "SFMCAccessToken") as! String
    let analyticsEnabled = Bundle.main.object(forInfoDictionaryKey: "SFMCAnalyticsEnabled") as? Bool ?? false;
    let appId = Bundle.main.object(forInfoDictionaryKey: "SFMCApplicationId") as! String
    let delayRegistrationUntilContactKeyIsSet = Bundle.main.object(forInfoDictionaryKey: "SFMCDelayRegistrationUntilContactKeyIsSet") as? Bool ?? false;
    let inboxEnabled = Bundle.main.object(forInfoDictionaryKey: "SFMCInboxEnabled") as? Bool ?? false;
    let locationEnabled = Bundle.main.object(forInfoDictionaryKey: "SFMCLocationEnabled") as? Bool ?? false;
    let mid = Bundle.main.object(forInfoDictionaryKey: "SFMCMid") as? String;
    let serverUrl = URL(string: Bundle.main.object(forInfoDictionaryKey: "SFMCServerUrl") as! String)!;
    let markMessageReadOnInboxNotificationOpen = Bundle.main.object(forInfoDictionaryKey: "SFMCMarkNotificationReadOnInboxNotificationOpen") as? Bool ?? false;

    if (debug) {
      // Enable logging for debugging early on. Debug level is not recommended for production apps, as significant data
      // about the MobilePush will be logged to the console.
      SFMCSdk.setLogger(logLevel: .debug)
    }
    
    // Use the Mobile Push Config Builder to configure the Mobile Push Module. This gives you the maximum flexibility in SDK configuration.
    // The builder lets you configure the module parameters at runtime.
    var mobilePushBuilder = PushConfigBuilder(appId: appId)
        .setAccessToken(accessToken)
        .setMarketingCloudServerUrl(serverUrl)
        .setInboxEnabled(inboxEnabled)
        .setLocationEnabled(locationEnabled)
        .setAnalyticsEnabled(analyticsEnabled)
        .setDelayRegistrationUntilContactKeyIsSet(delayRegistrationUntilContactKeyIsSet)
        .setMarkMessageReadOnInboxNotificationOpen(markMessageReadOnInboxNotificationOpen)
    
    if let mid {
      mobilePushBuilder = mobilePushBuilder.setMid(mid)
    }
    
    // Set the completion handler to take action when module initialization is completed. The result indicates if initialization was sucesfull or not.
    // Seting the completion handler is optional.
    let completionHandler: (OperationResult) -> () = { result in
        if result == .success {
          // module is fully configured and ready for user
          let notificationCenterDelegate = ModuleRegistryProvider.getSingletonModule(for: EXNotificationCenterDelegate.self) as! EXNotificationCenterDelegate
          
          self.notificationDelegate = ExpoMarketingCloudSdkNotificationsDelegate()
          notificationCenterDelegate.add(self.notificationDelegate!)
  
        } else if result == .error {
          // module failed to initialize, check logs for more details
          log.warn("[expo-marketingcloudsdk] Failed to initialize instance.")
        } else if result == .cancelled {
          // module initialization was cancelled (for example due to re-confirguration triggered before init was completed)
        } else if result == .timeout {
          // module failed to initialize due to timeout, check logs for more details
        }
    }
            
    // Once you've created the mobile push configuration, intialize the SDK.
    SFMCSdk.initializeSdk(ConfigBuilder().setPush(config: mobilePushBuilder.build(), onCompletion: completionHandler).build())
    
    return true
  }
}
