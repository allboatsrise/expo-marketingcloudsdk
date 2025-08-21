import ExpoModulesCore
import EXNotifications
import SFMCSDK
import MarketingCloudSDK

class ExpoMarketingCloudSdkNotificationsDelegate : NSObject, NotificationDelegate {
    public func didReceive(_ userInfo: [AnyHashable : Any], completionHandler: @escaping (UIBackgroundFetchResult) -> Void) -> Bool {
        SFMCSdk.requestPushSdk { mp in
          mp.setNotificationUserInfo(userInfo)
        }
        completionHandler(UIBackgroundFetchResult.newData)
        return true
    }
    
    public func didReceive(_ response: UNNotificationResponse, completionHandler: @escaping () -> Void) -> Bool {
        SFMCSdk.requestPushSdk { mp in
          mp.setNotificationResponse(response)
        }
        return false
    }
}
