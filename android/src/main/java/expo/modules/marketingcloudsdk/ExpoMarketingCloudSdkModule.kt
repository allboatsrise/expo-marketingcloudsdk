package expo.modules.marketingcloudsdk

import com.facebook.react.bridge.ReadableMap
import com.salesforce.marketingcloud.InitializationStatus
import com.salesforce.marketingcloud.sfmcsdk.SFMCSdk
import com.salesforce.marketingcloud.sfmcsdk.components.events.EventManager
import com.salesforce.marketingcloud.sfmcsdk.components.logging.LogLevel
import com.salesforce.marketingcloud.sfmcsdk.components.logging.LogListener
import com.salesforce.marketingcloud.sfmcsdk.modules.push.PushModuleInterface
import expo.modules.kotlin.Promise
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition


class ExpoMarketingCloudSdkModule : Module() {
  private var numberOfListeners = 0
  private var hasOnLogListener = false

  // Each module class must implement the definition function. The definition consists of components
  // that describes the module's functionality and behavior.
  // See https://docs.expo.dev/modules/module-api for more details about available components.
  override fun definition() = ModuleDefinition {
    // Sets the name of the module that JavaScript code will use to refer to the module. Takes a string as an argument.
    // Can be inferred from module's class name, but it's recommended to set it explicitly for clarity.
    // The module will be accessible from `requireNativeModule('ExpoMarketingCloudSdk')` in JavaScript.
    Name("ExpoMarketingCloudSdk")

    // Defines event names that the module can send to JavaScript.
    Events("onLog")

    AsyncFunction("isPushEnabled") { promise: Promise ->
      whenPushModuleReady(promise) { mp -> promise.resolve(mp.pushMessageManager.isPushEnabled) }
    }

    AsyncFunction("enablePush") { promise: Promise ->
      whenPushModuleReady(promise) { mp ->
        mp.pushMessageManager.enablePush()
        promise.resolve(mp.pushMessageManager.isPushEnabled)
      }
    }

    AsyncFunction("disablePush") { promise: Promise ->
      whenPushModuleReady(promise) { mp ->
        mp.pushMessageManager.disablePush()
        promise.resolve(mp.pushMessageManager.isPushEnabled)
      }
    }

    AsyncFunction("getSystemToken") { promise: Promise ->
      whenPushModuleReady(promise) { mp -> promise.resolve(mp.pushMessageManager.pushToken) }
    }

    AsyncFunction("setSystemToken") {token: String, promise: Promise ->
      whenPushModuleReady(promise) { mp ->
        mp.pushMessageManager.setPushToken(token)
        promise.resolve(mp.pushMessageManager.pushToken)
      }
    }

    AsyncFunction("getDeviceID") { promise: Promise ->
      whenPushModuleReady(promise) { mp -> promise.resolve(mp.registrationManager.deviceId) }
    }

    AsyncFunction("getAttributes") { promise: Promise ->
      whenPushModuleReady(promise) { mp -> promise.resolve(mp.registrationManager.attributes)}
    }

    AsyncFunction("setAttribute") {key: String, value: String, promise: Promise ->
      SFMCSdk.requestSdk { sdk ->
        sdk.identity.setProfileAttribute(key, value)
        whenPushModuleReady(promise) { mp -> promise.resolve(mp.registrationManager.attributes)}
      }
    }

    AsyncFunction("clearAttribute") {key: String, promise: Promise ->
      SFMCSdk.requestSdk { sdk ->
        sdk.identity.clearProfileAttribute(key)
        whenPushModuleReady(promise) { mp -> promise.resolve(mp.registrationManager.attributes)}
      }
    }

    AsyncFunction("addTag") {tag: String, promise: Promise ->
      whenPushModuleReady(promise) { mp ->
        mp.registrationManager.edit().addTag(tag).commit()
        promise.resolve(mp.registrationManager.tags)
      }
    }

    AsyncFunction("removeTag") {tag: String, promise: Promise ->
      whenPushModuleReady(promise) { mp ->
        mp.registrationManager.edit().removeTag(tag).commit()
        promise.resolve(mp.registrationManager.tags)
      }
    }

    AsyncFunction("getTags") { promise: Promise ->
      whenPushModuleReady(promise) { mp -> promise.resolve(mp.registrationManager.tags)}
    }

    AsyncFunction("setContactKey") { contactKey: String, promise: Promise ->
      SFMCSdk.requestSdk { sdk ->
        sdk.identity.setProfileId(contactKey)

        whenPushModuleReady(promise) {mp -> promise.resolve(mp.moduleIdentity.profileId)}
      }
    }

    AsyncFunction("getContactKey") { promise: Promise ->
      whenPushModuleReady(promise) {mp -> promise.resolve(mp.moduleIdentity.profileId)}
    }

    AsyncFunction("getSdkState") { promise: Promise ->
      SFMCSdk.requestSdk { sdk -> promise.resolve(sdk.getSdkState()) }
    }

    AsyncFunction("track") {name: String, attributes: ReadableMap, promise: Promise ->
      var event = EventManager.customEvent(name, attributes.toHashMap())
      SFMCSdk.track(event)
      promise.resolve(true)
    }


    Function("addListener") {eventName: String ->
      numberOfListeners++
      if (eventName === "onLog" && !hasOnLogListener) {
        hasOnLogListener = true

        SFMCSdk.setLogging(LogLevel.DEBUG, object: LogListener {
          override fun out(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
            sendEvent("onLog", mapOf(
                    "level" to level.toString(),
                    "subsystem" to tag,
                    "category" to tag,
                    "message" to message,
                    "stackTrace" to throwable?.toString()
            ))
          }
        })
      }
    }

    Function("removeListeners") {count: Int ->
      numberOfListeners -= count

      if (numberOfListeners <= 0) {
        SFMCSdk.setLogging(LogLevel.NONE, null)
      }
    }
  }

  private fun whenPushModuleReady(promise: Promise, callback: (mp: PushModuleInterface) -> Unit) {
    SFMCSdk.requestSdk { sdk -> sdk.mp {mp ->
      if (mp.initializationStatus.status == InitializationStatus.Status.FAILED) {
        promise.reject("ERR_SFMC_SDK_INIT", "Marketing Cloud Push Module failed to initialize", null)
      } else {
        callback(mp)
      }
    } }
  }
}
