package expo.modules.marketingcloudsdk

import android.content.Context
import expo.modules.core.BasePackage
import expo.modules.core.interfaces.ApplicationLifecycleListener

class ExpoMarketingCloudSdkPackage : BasePackage() {
  override fun createApplicationLifecycleListeners(context: Context): List<ApplicationLifecycleListener> {
    return listOf(ExpoMarketingCloudSdkApplicationLifecycleListener())
  }
}
