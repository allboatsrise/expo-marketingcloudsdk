package expo.modules.marketingcloudsdk.service

import com.google.firebase.messaging.RemoteMessage
import com.salesforce.marketingcloud.messages.push.PushMessageManager
import com.salesforce.marketingcloud.sfmcsdk.SFMCSdk
import expo.modules.notifications.service.ExpoFirebaseMessagingService

open class ExpoFirebaseMessagingService : ExpoFirebaseMessagingService() {
  override fun onMessageReceived(message: RemoteMessage) {
    if (PushMessageManager.isMarketingCloudPush(message)) {
      SFMCSdk.requestSdk { sdk ->
        sdk.mp {
          it.pushMessageManager.handleMessage(message)
        }
      }
    } else {
      super.onMessageReceived(message)
    }
  }
}