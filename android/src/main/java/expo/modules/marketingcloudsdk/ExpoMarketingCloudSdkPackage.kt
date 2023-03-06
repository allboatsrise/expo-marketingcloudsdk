package expo.modules.marketingcloudsdk

import android.content.Context
import expo.modules.core.BasePackage
import expo.modules.core.interfaces.ReactActivityLifecycleListener

class ExpoMarketingCloudSdkPackage : BasePackage() {
  override fun createReactActivityLifecycleListeners(activityContext: Context): List<ReactActivityLifecycleListener> {
    return listOf(ExpoMarketingCloudSdkReactActivityLifecycleListener(activityContext))
  }
}
