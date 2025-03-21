package expo.modules.marketingcloudsdk

import com.facebook.react.bridge.ReadableMap
import com.salesforce.marketingcloud.InitializationStatus
import com.salesforce.marketingcloud.MCLogListener
import com.salesforce.marketingcloud.MarketingCloudSdk
import com.salesforce.marketingcloud.sfmcsdk.SFMCSdk
import com.salesforce.marketingcloud.sfmcsdk.components.events.EventManager
import com.salesforce.marketingcloud.sfmcsdk.components.logging.LogLevel
import com.salesforce.marketingcloud.sfmcsdk.components.logging.LogListener
import com.salesforce.marketingcloud.sfmcsdk.modules.push.PushModuleInterface
import com.salesforce.marketingcloud.messages.inbox.InboxMessage
import com.salesforce.marketingcloud.messages.inbox.InboxMessageManager.InboxResponseListener
import com.salesforce.marketingcloud.registration.Registration
import com.salesforce.marketingcloud.registration.RegistrationManager.RegistrationEventListener
import expo.modules.kotlin.Promise
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import kotlinx.serialization.json.*
import java.text.SimpleDateFormat


class ExpoMarketingCloudSdkModule : Module() {
  private var defaultLogLevel : Int = MCLogListener.WARN
  private var inboxResponseListener : InboxResponseListener? = null
  private var registrationListener : RegistrationEventListener? = null

  // Each module class must implement the definition function. The definition consists of components
  // that describes the module's functionality and behavior.
  // See https://docs.expo.dev/modules/module-api for more details about available components.
  override fun definition() = ModuleDefinition {
    // Sets the name of the module that JavaScript code will use to refer to the module. Takes a string as an argument.
    // Can be inferred from module's class name, but it's recommended to set it explicitly for clarity.
    // The module will be accessible from `requireNativeModule('ExpoMarketingCloudSdk')` in JavaScript.
    Name("ExpoMarketingCloudSdk")

    // Defines event names that the module can send to JavaScript.
    Events("onLog", "onInboxResponse", "onRegistrationResponseSucceeded")

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
      SFMCSdk.requestSdk { sdk -> promise.resolve(sdk.getSdkState().toString()) }
    }

    AsyncFunction("track") {name: String, attributes: ReadableMap, promise: Promise ->
      val event = EventManager.customEvent(name, attributes.toHashMap())
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

    AsyncFunction("trackMessageOpened") { messageId: String, promise: Promise ->
      whenPushModuleReady(promise) {mp ->
        val message = mp.inboxMessageManager.messages.find { m -> m.id === messageId }
        if (message != null) {
          mp.analyticsManager.trackInboxOpenEvent(message)
        }
        promise.resolve(message != null)
      }
    }

    AsyncFunction("startObserving") {eventName: String? ->
      when (eventName) {
        "onLog" -> {
          defaultLogLevel = MarketingCloudSdk.getLogLevel()

          SFMCSdk.setLogging(LogLevel.DEBUG, object : LogListener.AndroidLogger() {
            override fun out(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
              sendEvent("onLog", mapOf(
                "level" to level.toString(),
                "subsystem" to tag,
                "category" to tag,
                "message" to message,
                "stackTrace" to throwable?.toString()
              ))
              super.out(level, tag, message,  throwable)
            }
          })

          MarketingCloudSdk.setLogLevel(MCLogListener.DEBUG)
          MarketingCloudSdk.setLogListener(object : MCLogListener.AndroidLogListener() {
            override fun out(level: Int, tag: String, message: String, throwable: Throwable?) {
              sendEvent("onLog", mapOf(
                "level" to when (level) {
                  MCLogListener.VERBOSE -> "VERBOSE"
                  MCLogListener.DEBUG -> "DEBUG"
                  MCLogListener.WARN -> "WARN"
                  MCLogListener.INFO -> "INFO"
                  MCLogListener.ERROR -> "ERROR"
                  else -> "DEBUG"
                },
                "subsystem" to tag,
                "category" to tag,
                "message" to message,
                "stackTrace" to throwable?.toString()
              ))
              super.out(level, tag, message,  throwable)
            }
          })
        }

        "onInboxResponse" -> {
          whenPushModuleReady(null) { mp ->
            if (inboxResponseListener == null) {
              val listener = object: InboxResponseListener {
                override fun onInboxMessagesChanged(messages: MutableList<InboxMessage>) {
                  sendEvent("onInboxResponse", mapOf(
                          "messages" to messagesToJSValue(messages)
                  ))
                }
              }

              mp.inboxMessageManager.registerInboxResponseListener(listener)
              inboxResponseListener = listener
            }
          }
        }

        "onRegistrationResponseSucceeded" -> {
          whenPushModuleReady(null) { mp ->
            if (registrationListener == null) {
              val listener = object: RegistrationEventListener {
                override fun onRegistrationReceived(it: Registration) {
                  sendEvent("onRegistrationResponseSucceeded", mapOf(
                    "response" to mapOf(
                      "etAppId" to it.appId,
                      "appVersion" to it.appVersion,
                      "attributes" to it.attributes,
                      "subscriberKey" to it.contactKey,
                      "deviceID" to it.deviceId,
                      "dst" to it.dst,
                      "hwid" to it.hwid,
                      "locale" to it.locale,
                      "locationEnabled" to it.locationEnabled,
                      "platform" to it.platform,
                      "platformVersion" to it.platformVersion,
                      "proximityEnabled" to it.proximityEnabled,
                      "pushEnabled" to it.pushEnabled,
                      "sdkVersion" to it.sdkVersion,
                      "signedString" to it.signedString,
                      "systemToken" to it.systemToken,
                      "tags" to it.tags,
                      "timeZone" to it.timeZone
                    )
                  ))
                }
              }


              mp.registrationManager.registerForRegistrationEvents(listener)
              registrationListener = listener
            }
          }
        }
      }
    }

    AsyncFunction("stopObserving") {eventName: String? ->

      when (eventName) {
        "onLog" -> {
          SFMCSdk.setLogging(when(defaultLogLevel) {
            MCLogListener.VERBOSE -> LogLevel.DEBUG
            MCLogListener.DEBUG -> LogLevel.DEBUG
            MCLogListener.WARN -> LogLevel.WARN
            MCLogListener.INFO -> LogLevel.DEBUG
            MCLogListener.ERROR -> LogLevel.ERROR
            else -> LogLevel.DEBUG
          }, LogListener.AndroidLogger())

          MarketingCloudSdk.setLogLevel(defaultLogLevel)
          MarketingCloudSdk.setLogListener(MCLogListener.AndroidLogListener())
        }

        "onInboxResponse" -> {
          val listener = inboxResponseListener
          if (listener != null) {
            inboxResponseListener = null
            whenPushModuleReady(null) { mp ->
              try {
                mp.inboxMessageManager.unregisterInboxResponseListener(listener)
              } catch (ex: Throwable) {
                throw ex
              }
            }
          }
        }

        "onRegistrationResponseSucceeded" -> {
          val listener2 = registrationListener
          if (listener2 != null) {
            registrationListener = null
            whenPushModuleReady(null) { mp ->
              try {
                mp.registrationManager.unregisterForRegistrationEvents(listener2)
              } catch (ex: Throwable) {
                throw ex
              }
            }
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

  private fun messagesToJSValue(messages: List<InboxMessage>): List<Map<String, Any?>> {
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

    return messages.map {
      var media = it.media
      var custom = it.custom

      mapOf(
        "id" to it.id,
        "alert" to it.alert,
        "custom" to if (custom != null) Json.decodeFromString(ExpoMarketingCloudSdkKotlinxGenericMapSerializer, custom) else null,
        "customKeys" to it.customKeys,
        "deleted" to it.deleted,
        "endDateUtc" to if (it.endDateUtc != null) dateFormatter.format(it.endDateUtc) else null,
        "media" to if (media != null) mapOf(
          "url" to media.url,
          "altText" to media.altText,
        ) else null,
        "read" to it.read,
        "sendDateUtc" to if (it.sendDateUtc != null) dateFormatter.format(it.sendDateUtc) else null,
        "sound" to it.sound,
        "startDateUtc" to if (it.startDateUtc != null) dateFormatter.format(it.startDateUtc) else null,
        "subject" to it.subject,
        "title" to it.title,
        "url" to it.url,
      )
    }
  }
}
