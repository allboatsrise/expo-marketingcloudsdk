package expo.modules.marketingcloudsdk

import com.facebook.react.bridge.ReadableMap
import com.salesforce.marketingcloud.InitializationStatus
import com.salesforce.marketingcloud.sfmcsdk.SFMCSdk
import com.salesforce.marketingcloud.sfmcsdk.components.events.EventManager
import com.salesforce.marketingcloud.sfmcsdk.components.logging.LogLevel
import com.salesforce.marketingcloud.sfmcsdk.components.logging.LogListener
import com.salesforce.marketingcloud.sfmcsdk.modules.push.PushModuleInterface
import com.salesforce.marketingcloud.messages.inbox.InboxMessage
import expo.modules.kotlin.Promise
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.kotlin.types.JSTypeConverter
import com.google.gson.Gson


class ExpoMarketingCloudSdkModule : Module() {
  private var numberOfListeners = 0
  private var hasOnLogListener = false
  private var inboxResponseListener : ((messages: List<InboxMessage>) -> Unit)? = null

  // Each module class must implement the definition function. The definition consists of components
  // that describes the module's functionality and behavior.
  // See https://docs.expo.dev/modules/module-api for more details about available components.
  override fun definition() = ModuleDefinition {
    // Sets the name of the module that JavaScript code will use to refer to the module. Takes a string as an argument.
    // Can be inferred from module's class name, but it's recommended to set it explicitly for clarity.
    // The module will be accessible from `requireNativeModule('ExpoMarketingCloudSdk')` in JavaScript.
    Name("ExpoMarketingCloudSdk")

    // Defines event names that the module can send to JavaScript.
    Events("onLog", "onInboxResponse")

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
      SFMCSdk.requestSdk { sdk -> promise.resolve(Gson().fromJson(sdk.getSdkState().toString(), HashMap<String, Object>().javaClass)) }
    }

    AsyncFunction("track") {name: String, attributes: ReadableMap, promise: Promise ->
      var event = EventManager.customEvent(name, attributes.toHashMap())
      SFMCSdk.track(event)
      promise.resolve(true)
    }

    AsyncFunction("deleteMessage") { messageId: String, promise: Promise ->
      whenPushModuleReady(promise) {mp -> promise.resolve(mp.inboxMessageManager.deleteMessage(messageId))}
    }

    AsyncFunction("getDeletedMessageCount") { promise: Promise ->
      whenPushModuleReady(promise) {mp -> promise.resolve(mp.inboxMessageManager.deletedMessageCount)}
    }

    AsyncFunction("getDeletedMessages") { promise: Promise ->
      whenPushModuleReady(promise) {mp -> promise.resolve(messagesToJSValue(mp.inboxMessageManager.deletedMessages))}
    }

    AsyncFunction("getMessageCount") { promise: Promise ->
      whenPushModuleReady(promise) {mp -> promise.resolve(mp.inboxMessageManager.messageCount)}
    }

    AsyncFunction("getMessages") { promise: Promise ->
      whenPushModuleReady(promise) {mp -> promise.resolve(messagesToJSValue(mp.inboxMessageManager.messages))}
    }

    AsyncFunction("getReadMessageCount") { promise: Promise ->
      whenPushModuleReady(promise) {mp -> promise.resolve(mp.inboxMessageManager.readMessageCount)}
    }

    AsyncFunction("getReadMessages") { promise: Promise ->
      whenPushModuleReady(promise) {mp -> promise.resolve(messagesToJSValue(mp.inboxMessageManager.readMessages))}
    }

    AsyncFunction("getUnreadMessageCount") { promise: Promise ->
      whenPushModuleReady(promise) {mp -> promise.resolve(mp.inboxMessageManager.unreadMessageCount)}
    }

    AsyncFunction("getUnreadMessages") { promise: Promise ->
      whenPushModuleReady(promise) {mp -> promise.resolve(messagesToJSValue(mp.inboxMessageManager.unreadMessages))}
    }

    AsyncFunction("markAllMessagesDeleted") { promise: Promise ->
      whenPushModuleReady(promise) {mp -> promise.resolve(mp.inboxMessageManager.markAllMessagesDeleted())}
    }

    AsyncFunction("markAllMessagesRead") { promise: Promise ->
      whenPushModuleReady(promise) {mp -> promise.resolve(mp.inboxMessageManager.markAllMessagesRead())}
    }

    AsyncFunction("refreshInbox") { promise: Promise ->
      whenPushModuleReady(promise) {mp -> mp.inboxMessageManager.refreshInbox { successful -> promise.resolve(successful) }}
    }

    AsyncFunction("setMessageRead") { messageId: String, promise: Promise ->
      whenPushModuleReady(promise) {mp -> promise.resolve(mp.inboxMessageManager.setMessageRead(messageId))}
    }

    Function("addListener") {eventName: String ->
      numberOfListeners++

      when (eventName) {
        "onLog" -> {
          if (!hasOnLogListener) {
            hasOnLogListener = true

            SFMCSdk.setLogging(LogLevel.DEBUG, object : LogListener {
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

        "onInboxResponse" -> {
          whenPushModuleReady(null) { mp ->
            if (inboxResponseListener == null) {
              var listener = { messages: List<InboxMessage> ->
                sendEvent("onInboxResponse", mapOf(
                        "messages" to messagesToJSValue(messages)
                ))
              }
              mp.inboxMessageManager.registerInboxResponseListener(listener)
              inboxResponseListener = listener
            }
          }
        }
      }
    }

    Function("removeListeners") {count: Int ->
      numberOfListeners -= count

      if (numberOfListeners == 0) {
        SFMCSdk.setLogging(LogLevel.NONE, null)

        var listener = inboxResponseListener
        if (listener != null) {
          inboxResponseListener = null
          whenPushModuleReady(null) { mp ->
            mp.inboxMessageManager.registerInboxResponseListener(listener)
          }
        }
      }
    }
  }

  private fun whenPushModuleReady(promise: Promise?, callback: (mp: PushModuleInterface) -> Unit) {
    SFMCSdk.requestSdk { sdk -> sdk.mp {mp ->
      if (mp.initializationStatus.status == InitializationStatus.Status.FAILED) {
        promise?.reject("ERR_SFMC_SDK_INIT", "Marketing Cloud Push Module failed to initialize", null)
      } else {
        callback(mp)
      }
    } }
  }

  private fun messagesToJSValue(messages: List<InboxMessage>) {
    JSTypeConverter.convertToJSValue(messages)
  }
}
