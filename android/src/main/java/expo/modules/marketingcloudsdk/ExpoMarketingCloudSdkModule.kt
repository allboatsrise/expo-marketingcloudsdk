package expo.modules.marketingcloudsdk

import com.facebook.react.bridge.ReadableMap
import android.util.Log
import com.salesforce.marketingcloud.InitializationStatus
import com.salesforce.marketingcloud.MCLogListener
import com.salesforce.marketingcloud.MarketingCloudSdk
import com.salesforce.marketingcloud.sfmcsdk.SFMCSdk
import com.salesforce.marketingcloud.notifications.NotificationCustomizationOptions
import com.salesforce.marketingcloud.notifications.NotificationManager
import com.salesforce.marketingcloud.notifications.NotificationMessage
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
import org.json.JSONObject


class ExpoMarketingCloudSdkModule : Module() {
  private var defaultLogLevel : Int = MCLogListener.WARN
  private var inboxResponseListener : InboxResponseListener? = null
  private var registrationListener : RegistrationEventListener? = null
  private var isMigratingBu: Boolean = false

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
      val attributeMap = HashMap(
        attributes.toHashMap().filterValues { it != null }
          .mapValues { it.value!! }
      )
      val event = EventManager.customEvent(name, attributeMap)
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

    AsyncFunction("isAnalyticsEnabled") { promise: Promise ->
      whenPushModuleReady(promise) { mp -> promise.resolve(mp.analyticsManager.areAnalyticsEnabled()) }
    }

    AsyncFunction("enableAnalytics") { promise: Promise ->
      whenPushModuleReady(promise) { mp ->
        mp.analyticsManager.enableAnalytics()
        promise.resolve(mp.analyticsManager.areAnalyticsEnabled())
      }
    }

    AsyncFunction("disableAnalytics") { promise: Promise ->
      whenPushModuleReady(promise) { mp ->
        mp.analyticsManager.disableAnalytics()
        promise.resolve(mp.analyticsManager.areAnalyticsEnabled())
      }
    }

    AsyncFunction("migrateBu") { config: ReadableMap, promise: Promise ->
      val newAppId = if (config.hasKey("appId")) config.getString("appId") else null
      val newAccessToken = if (config.hasKey("accessToken")) config.getString("accessToken") else null
      val newServerUrl = if (config.hasKey("serverUrl")) config.getString("serverUrl") else null
      val newMid = if (config.hasKey("mid")) config.getString("mid") else null
      // Internal self-contained timing constants
      val MIGRATION_TIMEOUT_MS = 30_000
      val FORCE_RECONFIGURE_MS = 8_000
      val POLL_INTERVAL_MS = 2_000L
      val REDISABLE_RETRY_MS = 3_000L
      val MAX_REDISABLE_ATTEMPTS = 3
      val TAG = "ExpoMCSdkMigration"

  if (newAppId.isNullOrBlank() || newAccessToken.isNullOrBlank() || newServerUrl.isNullOrBlank()) {
        promise.reject("ERR_INVALID_CONFIG", "appId, accessToken and serverUrl are required", null)
        return@AsyncFunction
      }
      if (isMigratingBu) {
        promise.reject("ERR_MIGRATION_IN_PROGRESS", "A BU migration is already in progress", null)
        return@AsyncFunction
      }
  Log.d(TAG, "Starting BU migration -> newAppId=$newAppId serverUrl=$newServerUrl mid=$newMid")

      whenPushModuleReady(promise) { mp ->
        if (isMigratingBu) {
          promise.reject("ERR_MIGRATION_IN_PROGRESS", "A BU migration is already in progress", null)
          return@whenPushModuleReady
        }
        isMigratingBu = true
        val savedAttributes = HashMap(mp.registrationManager.attributes)
        val savedTags = HashSet(mp.registrationManager.tags)
        val savedContactKey = mp.registrationManager.contactKey
        val savedToken = mp.pushMessageManager.pushToken
        val wasPushEnabled = mp.pushMessageManager.isPushEnabled
        var previousSenderId: String? = null
        var previousAppId: String? = null
        SFMCSdk.requestSdk { sdk ->
          try {
            val state = sdk.getSdkState()
            val pushObj: JSONObject? = state.optJSONObject("PUSH")
            val initConfig = pushObj?.optJSONObject("initConfig")
            previousSenderId = initConfig?.optString("senderId")?.takeIf { !it.isNullOrBlank() }
            previousAppId = initConfig?.optString("applicationId")
            Log.d(TAG, "Previous state: appId=$previousAppId senderId=$previousSenderId")
          } catch (ex: Throwable) {
            Log.w(TAG, "Failed parsing previous sdk state: ${ex.message}")
          }
        }
        val normalizedServerUrl = newServerUrl?.let { if (it.endsWith('/')) it else "$it/" }
        if (normalizedServerUrl == null) {
          // Safety check
          isMigratingBu = false
          promise.reject("ERR_INVALID_CONFIG", "serverUrl missing", null)
          return@whenPushModuleReady
        }
  var completed = false
        val handler = android.os.Handler(android.os.Looper.getMainLooper())

  // Will hold registration listener so reconfigure helper can unregister it
  var tempListener: RegistrationEventListener? = null
  // Track if reconfiguration has started to avoid duplicate calls
  var startedReconfigure = false
  // Forward declarations for runnables so we can remove them selectively
  var pollRunnable: Runnable? = null
  var timeoutRunnable: Runnable? = null
  var reDisableRunnable: Runnable? = null
  var reDisableAttempts = 0

        // Helper to perform reconfiguration once push is disabled
        fun reconfigureAndRestore(force: Boolean = false) {
          if (completed || startedReconfigure) return
          if (!force && mp.pushMessageManager.isPushEnabled) return
          startedReconfigure = true
          val senderIdSnapshot = previousSenderId
          val contactKeySnapshot = savedContactKey
          // stop further polling but keep timeout until success/failure
          pollRunnable?.let { handler.removeCallbacks(it) }
          try { tempListener?.let { mp.registrationManager.unregisterForRegistrationEvents(it) } } catch (_: Throwable) {}
          reDisableRunnable?.let { handler.removeCallbacks(it) }
          Log.d(TAG, "Reconfiguring SDK (force=$force) newAppId=$newAppId prevAppId=$previousAppId url=$normalizedServerUrl mid=$newMid prevSender=$senderIdSnapshot willDelay=${!contactKeySnapshot.isNullOrBlank()}")

          SFMCSdk.configure(appContext.reactContext!!.applicationContext, com.salesforce.marketingcloud.sfmcsdk.SFMCSdkModuleConfig.build {
            pushModuleConfig = com.salesforce.marketingcloud.MarketingCloudConfig.builder().apply {
              setApplicationId(newAppId)
              setAccessToken(newAccessToken)
              normalizedServerUrl?.let { setMarketingCloudServerUrl(it) }
              if (!newMid.isNullOrBlank()) setMid(newMid)
              if (!senderIdSnapshot.isNullOrBlank()) {
                try { setSenderId(senderIdSnapshot) } catch (ex: Throwable) { Log.w(TAG, "Unable to set senderId: ${ex.message}") }
              }
              if (!contactKeySnapshot.isNullOrBlank()) {
                try { setDelayRegistrationUntilContactKeyIsSet(true) } catch (_: Throwable) { Log.w(TAG, "delayRegistrationUntilContactKeyIsSet unsupported") }
              }
              // Provide a basic NotificationCustomizationOptions to satisfy SDK requirement
              try {
                setNotificationCustomizationOptions(
                  NotificationCustomizationOptions.create { ctx, notificationMessage: NotificationMessage ->
                    val channel = NotificationManager.createDefaultNotificationChannel(ctx)
                    NotificationManager.getDefaultNotificationBuilder(
                      ctx,
                      notificationMessage,
                      channel,
                      ctx.applicationInfo.icon
                    )
                  }
                )
              } catch (_: Throwable) {
                // Fallback: attempt with default channel only
                try {
                  setNotificationCustomizationOptions(
                    NotificationCustomizationOptions.create { ctx, notificationMessage: NotificationMessage ->
                      NotificationManager.getDefaultNotificationBuilder(
                        ctx,
                        notificationMessage,
                        NotificationManager.createDefaultNotificationChannel(ctx),
                        ctx.applicationInfo.icon
                      )
                    }
                  )
                } catch (_: Throwable) {}
              }
              setAnalyticsEnabled(true)
            }.build(appContext.reactContext!!.applicationContext)
          }) { status ->
            if (completed) return@configure
            val rawStatus = try { status.status } catch (_: Throwable) { -999 }
            val failed = isInitFailed(rawStatus)
            val success = !failed
            val failedOrdinal = InitializationStatus.Status.FAILED.ordinal
            val successOrdinal = InitializationStatus.Status.SUCCESS.ordinal
            Log.d(TAG, "Configure callback rawStatus=$rawStatus (FAILED.ordinal=$failedOrdinal SUCCESS.ordinal=$successOrdinal) failed=$failed -> treatingSuccess=$success")
            if (success) {
              try {
                SFMCSdk.requestSdk { sdk ->
                  sdk.identity.setProfileAttributes(savedAttributes)
                  if (!savedContactKey.isNullOrBlank()) sdk.identity.setProfileId(savedContactKey)
                  sdk.mp { newPush ->
                    newPush.registrationManager.edit().apply {
                      savedTags.forEach { addTag(it) }
                      commit()
                    }
                    if (!savedToken.isNullOrBlank() && (senderIdSnapshot.isNullOrBlank() || newPush.pushMessageManager.pushToken != savedToken)) {
                      Log.d(TAG, "Restoring push token (prevSender=$senderIdSnapshot)")
                      try { newPush.pushMessageManager.setPushToken(savedToken) } catch (ex: Throwable) { Log.e(TAG, "Failed setting token", ex) }
                    }
                    if (wasPushEnabled) {
                      try { newPush.pushMessageManager.enablePush() } catch (ex: Throwable) { Log.e(TAG, "Enable push failed", ex) }
                    } else {
                      try { newPush.pushMessageManager.disablePush() } catch (_: Throwable) {}
                    }
                  }
                }
                completed = true
                timeoutRunnable?.let { handler.removeCallbacks(it) }
                isMigratingBu = false
                Log.d(TAG, "Migration succeeded for newAppId=$newAppId (attributes=${savedAttributes.size} tags=${savedTags.size})")
                promise.resolve(mapOf(
                  "success" to true,
                  "newAppId" to newAppId,
                  "carried" to mapOf(
                    "contactKey" to (savedContactKey ?: null),
                    "attributeCount" to savedAttributes.size,
                    "tagCount" to savedTags.size,
                    "tokenCarried" to (!savedToken.isNullOrBlank())
                  ),
                  "isPushEnabled" to wasPushEnabled
                ))
                // Persist new BU credentials as stored values for next cold start
                try {
                  val ctx = appContext.reactContext!!.applicationContext
                  val p = ctx.getSharedPreferences("expo_marketingcloudsdk_bu", android.content.Context.MODE_PRIVATE)
                  p.edit().apply {
                    putString("storedAppId", newAppId)
                    putString("storedAccessToken", newAccessToken)
                    putString("storedServerUrl", normalizedServerUrl)
                    if (!newMid.isNullOrBlank()) putString("storedMid", newMid) else remove("storedMid")
                    apply()
                  }
                } catch (ex: Throwable) {
                  Log.w(TAG, "Failed storing migrated BU: ${ex.message}")
                }
              } catch (ex: Throwable) {
                completed = true
                timeoutRunnable?.let { handler.removeCallbacks(it) }
                isMigratingBu = false
                Log.e(TAG, "Migration restore failed", ex)
                promise.reject("ERR_MIGRATION_RESTORE", "Failed to restore data in new BU", ex)
              }
            } else {
              completed = true
              timeoutRunnable?.let { handler.removeCallbacks(it) }
              isMigratingBu = false
              try { SFMCSdk.requestSdk { sdk -> Log.e(TAG, "State after failed init=" + sdk.getSdkState().toString()) } } catch (_: Throwable) {}
              Log.e(TAG, "Re-init failed for new BU newAppId=$newAppId serverUrl=$normalizedServerUrl mid=$newMid senderId=$senderIdSnapshot rawStatus=$rawStatus failedOrdinal=${InitializationStatus.Status.FAILED.ordinal}")
              promise.reject("ERR_REINIT_FAILED", "Failed to initialize SDK with new BU (status=$rawStatus)", null)
            }
          }
        }

  tempListener = object: RegistrationEventListener {
          override fun onRegistrationReceived(reg: Registration) {
            if (!completed) {
              if (!reg.pushEnabled) {
                Log.d(TAG, "Registration event: push disabled -> proceeding to reconfigure")
                reconfigureAndRestore(true)
              } else if (wasPushEnabled && !startedReconfigure) {
                Log.d(TAG, "Registration event: push still enabled, scheduling forced reconfigure in 500ms")
                handler.postDelayed({ reconfigureAndRestore(true) }, 500)
              }
            }
          }
        }

  tempListener?.let { mp.registrationManager.registerForRegistrationEvents(it) }
        mp.pushMessageManager.disablePush()
        // Force registration update attribute to encourage sync
        mp.registrationManager.edit().apply {
          setAttribute("bu_migration_ts", System.currentTimeMillis().toString())
          commit()
        }
        Log.d(TAG, "Issued disablePush; added bu_migration_ts attribute to force registration update")

        // If push already disabled, fast-path after small delay
        if (!mp.pushMessageManager.isPushEnabled) {
          Log.d(TAG, "Push already disabled; fast-path reconfigure in 400ms")
          handler.postDelayed({ reconfigureAndRestore(true) }, 400)
        }

        // Retry disabling push a few times if still enabled (some devices/ROMs lag)
        reDisableRunnable = object: Runnable {
          override fun run() {
            if (completed || startedReconfigure) return
            if (!mp.pushMessageManager.isPushEnabled) return
            if (reDisableAttempts >= MAX_REDISABLE_ATTEMPTS) return
            reDisableAttempts += 1
            try {
              Log.d(TAG, "Re-disabling push attempt $reDisableAttempts")
              mp.pushMessageManager.disablePush()
              mp.registrationManager.edit().apply {
                setAttribute("bu_migration_retry", System.currentTimeMillis().toString())
                commit()
              }
            } catch (ex: Throwable) {
              Log.e(TAG, "Error re-disabling push", ex)
            }
            handler.postDelayed(this, REDISABLE_RETRY_MS)
          }
        }
        handler.postDelayed(reDisableRunnable!!, REDISABLE_RETRY_MS)

        // Poll fallback every 2s in case registration event not received
  pollRunnable = object: Runnable {
          override fun run() {
            if (completed) return
            SFMCSdk.requestSdk { sdk -> sdk.mp { innerMp ->
              if (!innerMp.pushMessageManager.isPushEnabled && !completed) {
    Log.d(TAG, "Poll: detected push disabled -> reconfigure")
    reconfigureAndRestore(true)
              } else if (!startedReconfigure) {
    handler.postDelayed(this, POLL_INTERVAL_MS)
              }
            } }
          }
        }
  pollRunnable?.let { handler.postDelayed(it, POLL_INTERVAL_MS) }

  // Force fallback reconfigure after internal FORCE_RECONFIGURE_MS even if push flag never flipped
  handler.postDelayed({ if (!completed && !startedReconfigure) { Log.d(TAG, "Force fallback reconfigure trigger"); reconfigureAndRestore(true) } }, FORCE_RECONFIGURE_MS.toLong())

        // Schedule timeout
    timeoutRunnable = Runnable {
          if (!completed) {
            completed = true
            pollRunnable?.let { handler.removeCallbacks(it) }
      reDisableRunnable?.let { handler.removeCallbacks(it) }
            try { tempListener?.let { mp.registrationManager.unregisterForRegistrationEvents(it) } } catch (_: Throwable) {}
            if (wasPushEnabled) {
              try { mp.pushMessageManager.enablePush() } catch (_: Throwable) {}
            }
            isMigratingBu = false
      Log.e(TAG, "Migration timeout after ${MIGRATION_TIMEOUT_MS}ms")
      promise.reject("ERR_MIGRATION_TIMEOUT", "BU migration timed out after ${MIGRATION_TIMEOUT_MS}ms", null)
          }
        }
    timeoutRunnable?.let { handler.postDelayed(it, MIGRATION_TIMEOUT_MS.toLong()) }
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

  AsyncFunction("getStoredBu") { promise: Promise ->
      try {
        val ctx = appContext.reactContext!!.applicationContext
        val p = ctx.getSharedPreferences("expo_marketingcloudsdk_bu", android.content.Context.MODE_PRIVATE)
  val appId = p.getString("storedAppId", null)
  val accessToken = p.getString("storedAccessToken", null)
  val serverUrl = p.getString("storedServerUrl", null)
  val mid = p.getString("storedMid", null)
        if (appId == null || accessToken == null || serverUrl == null) {
          promise.resolve(null)
        } else {
          promise.resolve(mapOf(
            "appId" to appId,
            "accessToken" to accessToken,
            "serverUrl" to serverUrl,
            "mid" to mid
          ))
        }
      } catch (ex: Throwable) {
        promise.reject("ERR_GET_STORED_BU", "Failed to get stored BU", ex)
      }
    }

    AsyncFunction("clearStoredBu") { promise: Promise ->
      try {
        val ctx = appContext.reactContext!!.applicationContext
        ctx.getSharedPreferences("expo_marketingcloudsdk_bu", android.content.Context.MODE_PRIVATE).edit().apply {
          remove("storedAppId")
          remove("storedAccessToken")
          remove("storedServerUrl")
          remove("storedMid")
          apply()
        }
        promise.resolve(true)
      } catch (ex: Throwable) {
        promise.reject("ERR_CLEAR_STORED_BU", "Failed to clear stored BU", ex)
      }
    }
  }

  private fun whenPushModuleReady(promise: Promise?, callback: (mp: PushModuleInterface) -> Unit) {
    SFMCSdk.requestSdk { sdk -> sdk.mp { mp ->
      if (isInitFailed(mp.initializationStatus.status)) {
        // Ensure migration flag is cleared if we were attempting a migration when init failed
        isMigratingBu = false
        promise?.reject("ERR_SFMC_SDK_INIT", "Marketing Cloud Push Module failed to initialize", null)
      } else {
        callback(mp)
      }
    } }
  }

  private fun isInitSuccess(raw: Any?): Boolean = when (raw) {
    is InitializationStatus.Status -> raw == InitializationStatus.Status.SUCCESS
    is Int -> raw == InitializationStatus.Status.SUCCESS.ordinal
    else -> false
  }

  private fun isInitFailed(raw: Any?): Boolean = when (raw) {
    is InitializationStatus.Status -> raw == InitializationStatus.Status.FAILED
    is Int -> raw == InitializationStatus.Status.FAILED.ordinal
    else -> false
  }

  private fun messagesToJSValue(messages: List<InboxMessage>): List<Map<String, Any?>> {
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

    return messages.map {
      var media = it.media
      var custom = it.custom

      mapOf(
        "alert" to it.alert,
        "custom" to if (custom != null) Json.decodeFromString(ExpoMarketingCloudSdkKotlinxGenericMapSerializer, custom) else null,
        "customKeys" to it.customKeys,
        "deleted" to it.deleted,
        "endDateUtc" to if (it.endDateUtc != null) dateFormatter.format(it.endDateUtc) else null,
        "id" to it.id,
        "inboxMessage" to it.inboxMessage,
        "inboxMessageType" to when(val type = it.messageType) {
          is Int -> type + 1
          else -> null
        },
        "inboxSubtitle" to it.inboxSubtitle,
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
