package expo.modules.marketingcloudsdk

import android.app.Activity
import android.content.Context
import android.os.Bundle
import expo.modules.core.interfaces.ReactActivityLifecycleListener

// EXPO_VERSIONING_NEEDS_EXPOVIEW_R

class MarketingCloudSdkReactActivityLifecycleListener(activityContext: Context) : ReactActivityLifecycleListener {
  override fun onCreate(activity: Activity, savedInstanceState: Bundle?) {
    // Execute static tasks before the JS engine starts.
    // These values are defined via config plugins.

    var value = getValue(activity)
    if (value != "") {
      // Do something to the Activity that requires the static value...
    }
  }
  
  private fun getValue(context: Context): String = context.getString(R.string.expo_marketingcloudsdk_value).toLowerCase()
}
