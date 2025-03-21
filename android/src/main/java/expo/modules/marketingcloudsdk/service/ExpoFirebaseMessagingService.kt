package expo.modules.marketingcloudsdk.service

import android.util.Log
import com.google.firebase.messaging.RemoteMessage
import com.salesforce.marketingcloud.messages.push.PushMessageManager
import com.salesforce.marketingcloud.sfmcsdk.SFMCSdk
import expo.modules.notifications.service.ExpoFirebaseMessagingService

open class ExpoFirebaseMessagingService : ExpoFirebaseMessagingService() {
  override fun onMessageReceived(remoteMessage: RemoteMessage) {
    Log.d("ExpoFirebaseMessagingService", "onMessageReceived called with remoteMessage: ${remoteMessage.messageId}")
    
    if (PushMessageManager.isMarketingCloudPush(remoteMessage)) {
      Log.d("ExpoFirebaseMessagingService", "Message is identified as Marketing Cloud Push")
      
      SFMCSdk.requestSdk { sdk ->
        Log.d("ExpoFirebaseMessagingService", "SFMCSdk requested")
        
        sdk.mp {
          Log.d("ExpoFirebaseMessagingService", "Handling Marketing Cloud Push message")
          it.pushMessageManager.handleMessage(remoteMessage)
        }
      }
    } else {
      Log.d("ExpoFirebaseMessagingService", "Message is not a Marketing Cloud Push, passing to super")
      super.onMessageReceived(remoteMessage)
    }
  }
}