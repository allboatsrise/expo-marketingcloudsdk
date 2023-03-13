import ExpoModulesCore
import SFMCSDK
import MarketingCloudSDK

class ExpoMarketingCloudSdkNotificationsDelegate : EXNotificationsDelegate {
  @objc
  public func application(_ application: UIApplication, didReceiveRemoteNotification userInfo: [AnyHashable : Any]) async -> UIBackgroundFetchResult {
    SFMCSdk.mp.setNotificationUserInfo(userInfo)
    return UIBackgroundFetchResult.newData
  }
}
