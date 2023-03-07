package expo.modules.marketingcloudsdk.service

import com.google.firebase.messaging.RemoteMessage
import com.salesforce.marketingcloud.messages.push.PushMessageManager
import com.salesforce.marketingcloud.sfmcsdk.SFMCSdk
import expo.modules.notifications.service.ExpoFirebaseMessagingService

open class ExpoFirebaseMessagingService : ExpoFirebaseMessagingService() {
  override fun onMessageReceived(remoteMessage: RemoteMessage) {
    if (PushMessageManager.isMarketingCloudPush(remoteMessage)) {
      SFMCSdk.requestSdk { sdk ->
        sdk.mp {
          it.pushMessageManager.handleMessage(remoteMessage)
        }
      }
    } else {
      super.onMessageReceived(remoteMessage)
    }
  }
}