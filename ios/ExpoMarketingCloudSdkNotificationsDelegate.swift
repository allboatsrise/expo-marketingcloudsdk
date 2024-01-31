import ExpoModulesCore
import SFMCSDK
import MarketingCloudSDK

class ExpoMarketingCloudSdkNotificationsDelegate : NSObject, EXNotificationsDelegate {
  @objc
  public func application(_ application: UIApplication, didReceiveRemoteNotification userInfo: [AnyHashable : Any]) async -> UIBackgroundFetchResult {
    SFMCSdk.requestPushSdk { mp in
      mp.setNotificationUserInfo(userInfo)
    }
    return UIBackgroundFetchResult.newData
  }
  @objc
  public func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse, withCompletionHandler completionHandler: @escaping () -> Void) {
    SFMCSdk.requestPushSdk { mp in
      mp.setNotificationRequest(response.notification.request)
    }
    completionHandler()
  }
}
