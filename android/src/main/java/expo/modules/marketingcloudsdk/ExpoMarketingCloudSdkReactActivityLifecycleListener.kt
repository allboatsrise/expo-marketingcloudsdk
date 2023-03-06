package expo.modules.marketingcloudsdk

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import com.salesforce.marketingcloud.MCLogListener
import com.salesforce.marketingcloud.MarketingCloudConfig
import com.salesforce.marketingcloud.MarketingCloudSdk
import com.salesforce.marketingcloud.sfmcsdk.SFMCSdk
import com.salesforce.marketingcloud.sfmcsdk.SFMCSdkModuleConfig
import com.salesforce.marketingcloud.sfmcsdk.components.logging.LogLevel
import com.salesforce.marketingcloud.sfmcsdk.components.logging.LogListener
import expo.modules.core.interfaces.ReactActivityLifecycleListener

class ExpoMarketingCloudSdkReactActivityLifecycleListener(activityContext: Context) : ReactActivityLifecycleListener {
  override fun onCreate(activity: Activity, savedInstanceState: Bundle?) {
    // Initialize logging _before_ initializing the SDK to avoid losing valuable debugging information.
    if(BuildConfig.DEBUG) {
      SFMCSdk.setLogging(LogLevel.DEBUG, LogListener.AndroidLogger())
      MarketingCloudSdk.setLogLevel(MCLogListener.VERBOSE)
      MarketingCloudSdk.setLogListener(MCLogListener.AndroidLogListener())
    }

    // Configure Salesforce Marketing Cloud SDK
    SFMCSdk.configure(activity, SFMCSdkModuleConfig.build {
      pushModuleConfig = MarketingCloudConfig.builder().apply {
        setApplicationId(getAppId(activity))
        setAccessToken(getAccessToken(activity))
        setAnalyticsEnabled(getAnalyticsEnabled(activity))
        setMarketingCloudServerUrl(getServerUrl(activity))
        setDelayRegistrationUntilContactKeyIsSet(getDelayRegistrationUntilContactKeyIsSet(activity))
        setSenderId(getSenderId(activity))
        setInboxEnabled(getInboxEnabled(activity))
        setMarkMessageReadOnInboxNotificationOpen(getMarkMessageReadOnInboxNotificationOpen(activity))
      }.build(activity)
    }) { initStatus ->
      // TODO handle initialization status
      Log.e("SMFCSdk: initStatus", initStatus.toString())
    }
  }
  
  private fun getAppId(context: Context): String = context.resources.getString(R.string.expo_marketingcloudsdk_app_id)
  private fun getAccessToken(context: Context): String = context.resources.getString(R.string.expo_marketingcloudsdk_access_token)
  private fun getServerUrl(context: Context): String = context.resources.getString(R.string.expo_marketingcloudsdk_server_url)
  private fun getSenderId(context: Context): String = context.resources.getString(R.string.expo_marketingcloudsdk_sender_id)
  private fun getAnalyticsEnabled(context: Context): Boolean = context.resources.getString(R.string.expo_marketingcloudsdk_analytics_enabled) == "true"
  private fun getDelayRegistrationUntilContactKeyIsSet(context: Context): Boolean = context.resources.getString(R.string.expo_marketingcloudsdk_delay_registration_until_contact_key_is_set) == "true"
  private fun getInboxEnabled(context: Context): Boolean = context.resources.getString(R.string.expo_marketingcloudsdk_inbox_enabled) == "true"
  private fun getMarkMessageReadOnInboxNotificationOpen(context: Context): Boolean = context.resources.getString(R.string.expo_marketingcloudsdk_mark_message_read_on_inbox_notification_open) == "true"
}
