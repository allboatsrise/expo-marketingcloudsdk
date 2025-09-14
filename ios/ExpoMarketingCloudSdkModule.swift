import ExpoModulesCore
import SFMCSDK
import MarketingCloudSDK

let onLogEvent = "onLog"
let onInboxResponseEvent = "onInboxResponse"
let onRegistrationResponseSucceededEvent = "onRegistrationResponseSucceeded"

public class ExpoMarketingCloudSdkModule: Module, ExpoMarketingCloudSdkLoggerDelegate {
  private var refreshInboxPromise: Promise?
  private var logger: ExpoMarketingCloudSdkLogger?
  private var defaultLogLevel: LogLevel = LogLevel.none
  // Prevent concurrent BU migrations
  private var isMigratingBu: Bool = false
  
  // Each module class must implement the definition function. The definition consists of components
  // that describes the module's functionality and behavior.
  // See https://docs.expo.dev/modules/module-api for more details about available components.
  public func definition() -> ModuleDefinition {
    // Sets the name of the module that JavaScript code will use to refer to the module. Takes a string as an argument.
    // Can be inferred from module's class name, but it's recommended to set it explicitly for clarity.
    // The module will be accessible from `requireNativeModule('ExpoMarketingCloudSdk')` in JavaScript.
    Name("ExpoMarketingCloudSdk")

    // Defines event names that the module can send to JavaScript.
    Events(onLogEvent, onInboxResponseEvent, onRegistrationResponseSucceededEvent)
      
    // Event: onLogEvent
    OnStartObserving(onLogEvent) {
      if (logger == nil) {
        self.logger = ExpoMarketingCloudSdkLogger()
        self.logger!.delegate = self
      }
      defaultLogLevel = SFMCSdk.getLogLevel()
      SFMCSdk.setLogger(logLevel: .debug, logOutputter: self.logger!)
    }
    OnStopObserving(onLogEvent) {
      SFMCSdk.setLogger(logLevel: defaultLogLevel)
    }
    
    // Event: onInboxResponseEvent
    OnStartObserving(onInboxResponseEvent) {
      NotificationCenter.default.addObserver(self, selector: #selector(self.inboxMessagesNewInboxMessagesListener), name: NSNotification.Name.SFMCInboxMessagesNewInboxMessages, object: nil)
      NotificationCenter.default.addObserver(self, selector: #selector(self.inboxMessagesRefreshCompleteListener), name: NSNotification.Name.SFMCInboxMessagesRefreshComplete, object: nil)
    }
    OnStopObserving(onInboxResponseEvent) {
      NotificationCenter.default.removeObserver(self, name: NSNotification.Name.SFMCInboxMessagesNewInboxMessages, object: nil)
      NotificationCenter.default.removeObserver(self, name: NSNotification.Name.SFMCInboxMessagesRefreshComplete, object: nil)
    }
    
    // Event: onRegistrationResponseSucceededEvent
    OnStartObserving(onRegistrationResponseSucceededEvent) {
      SFMCSdk.requestPushSdk { mp in
        mp.setRegistrationCallback() { response in
          self.sendEvent("onRegistrationResponseSucceeded", ["response": response])
        }
      }
    }
    OnStopObserving(onRegistrationResponseSucceededEvent) {
      SFMCSdk.requestPushSdk { mp in
        mp.unsetRegistrationCallback()
      }
    }

    AsyncFunction("isPushEnabled") { (promise: Promise) in
      SFMCSdk.requestPushSdk { mp in
        promise.resolve(mp.pushEnabled())
      }
    }

    AsyncFunction("enablePush") { (promise: Promise) in
      SFMCSdk.requestPushSdk { mp in
        mp.setPushEnabled(true)
        promise.resolve(mp.pushEnabled())
      }
    }

    AsyncFunction("disablePush") { (promise: Promise) in
      SFMCSdk.requestPushSdk { mp in
        mp.setPushEnabled(false)
        promise.resolve(mp.pushEnabled())
      }
    }

    AsyncFunction("getSystemToken") { (promise: Promise) in
      SFMCSdk.requestPushSdk { mp in
        promise.resolve(mp.deviceToken())
      }
    }
    
    AsyncFunction("setSystemToken") { (token: String, promise: Promise) in
      do {
        let token = try ExpoMarketingCloudSdkDeviceToken.init(hexString: token)
        
        SFMCSdk.requestPushSdk { mp in
          mp.setDeviceToken(token.data)
          promise.resolve(mp.deviceToken())
        }
      } catch {
        promise.reject("invalid-hex-token", "Failed to convert token to data type")
      }
    }

    AsyncFunction("getDeviceID") { (promise: Promise) in
      SFMCSdk.requestPushSdk { mp in
        promise.resolve(mp.deviceIdentifier())
      }
    }

    AsyncFunction("getAttributes") { (promise: Promise) in
      SFMCSdk.requestPushSdk { mp in
        promise.resolve(mp.attributes())
      }
    }

    AsyncFunction("setAttribute") { (key: String, value: String, promise: Promise) in
      SFMCSdk.identity.setProfileAttribute(key, value)
      
      SFMCSdk.requestPushSdk { mp in
        promise.resolve(mp.attributes())
      }
    }

    AsyncFunction("clearAttribute") { (key: String, promise: Promise) in
      SFMCSdk.identity.clearProfileAttribute(key: key)
      
      SFMCSdk.requestPushSdk { mp in
        promise.resolve(mp.attributes())
      }
    }

    AsyncFunction("migrateBu") { (config: [String: Any], promise: Promise) in
      // Basic expected keys
      guard let newAppId = config["appId"] as? String, !newAppId.isEmpty else {
        promise.reject("ERR_INVALID_CONFIG", "Missing appId in config")
        return
      }
      guard let newAccessToken = config["accessToken"] as? String, !newAccessToken.isEmpty else {
        promise.reject("ERR_INVALID_CONFIG", "Missing accessToken in config")
        return
      }
      guard let serverUrlStrRaw = config["serverUrl"] as? String, let serverUrlTmp = URL(string: serverUrlStrRaw) else {
        promise.reject("ERR_INVALID_CONFIG", "Missing or invalid serverUrl in config")
        return
      }
      // Normalize server url to always have trailing slash to match Android convention
      let serverUrlStr = serverUrlStrRaw.hasSuffix("/") ? serverUrlStrRaw : serverUrlStrRaw + "/"
      let newServerUrl = URL(string: serverUrlStr)!
      let newMid = config["mid"] as? String

      if self.isMigratingBu {
        promise.reject("ERR_MIGRATION_IN_PROGRESS", "A BU migration is already in progress")
        return
      }

      // Ensure SDK is operational before starting migration
      if SFMCSdk.mp.getStatus() != .operational {
        promise.reject("ERR_SDK_NOT_READY", "SDK is not operational; initialize before migrating")
        return
      }

      self.isMigratingBu = true

  // Internal timings (self-sufficient; not exposed via TS typings)
  let timeoutMs = 30000 // overall guard timeout
  let forceReconfigureAfterMs = 8000 // early forced reconfigure fallback

      // Capture current data to carry over
      var savedContactKey: String? = nil
      var savedTags: [String] = []
      var savedAttributes: [String: String] = [:]
  var savedToken: String? = nil
      var wasPushEnabled: Bool = false
      var completed = false
      var timeoutWorkItem: DispatchWorkItem?

      SFMCSdk.requestPushSdk { mp in
        wasPushEnabled = mp.pushEnabled()
        savedContactKey = mp.contactKey()
        if let tagsSet = mp.tags() {
          // Convert AnyHashable elements to String safely
          savedTags = tagsSet.compactMap { $0 as? String }
        }
        if let attrs = mp.attributes() {
          // Safely cast only string key/value pairs
          attrs.forEach { key, value in
            if let k = key as? String, let v = value as? String {
              savedAttributes[k] = v
            }
          }
        }
  savedToken = mp.deviceToken()

        // Helper closure to proceed with reconfiguration once push is disabled
        var proceedMigration: ((Bool) -> Void)? = nil
        proceedMigration = { [weak self] skipPushCheck in
          if completed { return }
          if !skipPushCheck && mp.pushEnabled() { return } // ensure actually disabled unless forced
          completed = true
          mp.unsetRegistrationCallback()

          // Build new push config
          var pushBuilder = PushConfigBuilder(appId: newAppId)
            .setAccessToken(newAccessToken)
            .setMarketingCloudServerUrl(newServerUrl)
          if let mid = newMid, !mid.isEmpty { pushBuilder = pushBuilder.setMid(mid) }

          SFMCSdk.initializeSdk(ConfigBuilder().setPush(config: pushBuilder.build(), onCompletion: { result in
            switch result {
            case .success:
              // Restore carried data in new context
              if let ck = savedContactKey { SFMCSdk.identity.setProfileId(ck) }
              savedAttributes.forEach { (k,v) in SFMCSdk.identity.setProfileAttribute(k, v) }
              SFMCSdk.requestPushSdk { newMp in
                savedTags.forEach { _ = newMp.addTag($0) }
                if let tokenString = savedToken, let tokenObj = try? ExpoMarketingCloudSdkDeviceToken(hexString: tokenString) {
                  newMp.setDeviceToken(tokenObj.data)
                }
                newMp.setPushEnabled(wasPushEnabled)
                timeoutWorkItem?.cancel()
                self?.isMigratingBu = false
                let carried: [String: Any] = [
                  "contactKey": savedContactKey as Any,
                  "attributeCount": savedAttributes.count,
                  "tagCount": savedTags.count,
                  "tokenCarried": savedToken != nil
                ]
                let result: [String: Any] = [
                  "success": true,
                  "newAppId": newAppId,
                  "carried": carried,
                  "isPushEnabled": wasPushEnabled
                ]
                promise.resolve(result)
                // Persist stored BU credentials for next cold start
                let d = UserDefaults.standard
                d.set(newAppId, forKey: "storedAppId")
                d.set(newAccessToken, forKey: "storedAccessToken")
                d.set(newServerUrl.absoluteString, forKey: "storedServerUrl")
                if let mid = newMid, !mid.isEmpty {
                  d.set(mid, forKey: "storedMid")
                } else {
                  d.removeObject(forKey: "storedMid")
                }
              }
            default:
              timeoutWorkItem?.cancel()
              self?.isMigratingBu = false
              promise.reject("ERR_REINIT_FAILED", "Failed to initialize SDK with new BU credentials")
            }
          }).build())
        }

        // Register one-off callback to detect opt-out registration
        mp.setRegistrationCallback { _ in
          // Only proceed after push disabled (opt-out completed)
          if mp.pushEnabled() == false && !completed { proceedMigration?(false) }
        }

        // Disable push to trigger opt-out registration event
        mp.setPushEnabled(false)
        // Force a registration update by setting a transient attribute to encourage server sync
        let ts = Int(Date().timeIntervalSince1970 * 1000)
        SFMCSdk.identity.setProfileAttribute("bu_migration_ts", "\(ts)")

        // Force fallback reconfigure after forceReconfigureAfterMs even if push flag never turned false
        let forceWorkItem = DispatchWorkItem { [weak self] in
          if !completed {
            proceedMigration?(true) // skip push enabled check
          }
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + .milliseconds(forceReconfigureAfterMs), execute: forceWorkItem)

        // Poll fallback: in case registration callback never fires, periodically check pushEnabled
        let pollInterval: TimeInterval = 2.0
        var elapsed: Int = 0
        let maxElapsed = timeoutMs // ms
        let timer = DispatchSource.makeTimerSource(queue: DispatchQueue.main)
        timer.schedule(deadline: .now() + pollInterval, repeating: pollInterval)
        timer.setEventHandler {
          if completed { timer.cancel(); return }
          elapsed += Int(pollInterval * 1000)
          SFMCSdk.requestPushSdk { innerMp in
            if !innerMp.pushEnabled() { proceedMigration?(false) }
          }
          if elapsed >= maxElapsed { timer.cancel() }
        }
        timer.resume()

        // Schedule timeout guard
        let workItem = DispatchWorkItem { [weak self] in
          if !completed {
            completed = true
            mp.unsetRegistrationCallback()
            // Restore original push state if needed
            if wasPushEnabled { mp.setPushEnabled(true) }
            self?.isMigratingBu = false
            promise.reject("ERR_MIGRATION_TIMEOUT", "BU migration timed out after \(timeoutMs) ms (pushEnabled may never have flipped)")
          }
        }
        timeoutWorkItem = workItem
  DispatchQueue.main.asyncAfter(deadline: .now() + .milliseconds(timeoutMs), execute: workItem)
      }
    }

    AsyncFunction("getStoredBu") { (promise: Promise) in
      let d = UserDefaults.standard
  // Read stored keys only (legacy keys removed)
  let appId = d.string(forKey: "storedAppId")
  let accessToken = d.string(forKey: "storedAccessToken")
  let serverUrl = d.string(forKey: "storedServerUrl")
      if appId == nil || accessToken == nil || serverUrl == nil {
        promise.resolve(nil)
        return
      }
  let mid = d.string(forKey: "storedMid")
      promise.resolve([
        "appId": appId as Any,
        "accessToken": accessToken as Any,
        "serverUrl": serverUrl as Any,
        "mid": mid as Any
      ])
    }

    AsyncFunction("clearStoredBu") { (promise: Promise) in
      let d = UserDefaults.standard
      d.removeObject(forKey: "storedAppId")
      d.removeObject(forKey: "storedAccessToken")
      d.removeObject(forKey: "storedServerUrl")
      d.removeObject(forKey: "storedMid")
      promise.resolve(true)
    }

    AsyncFunction("addTag") { (tag: String, promise: Promise) in
      SFMCSdk.requestPushSdk { mp in
        promise.resolve(mp.addTag(tag))
      }
    }

    AsyncFunction("removeTag") { (tag: String, promise: Promise) in
      SFMCSdk.requestPushSdk { mp in
        promise.resolve(mp.removeTag(tag))
      }
    }

    AsyncFunction("getTags") { (promise: Promise) in
      SFMCSdk.requestPushSdk { mp in
        promise.resolve(Array(mp.tags() ?? []))
      }
    }

    AsyncFunction("setContactKey") { (contactKey: String, promise: Promise) in
      SFMCSdk.identity.setProfileId(contactKey)
      
      SFMCSdk.requestPushSdk { mp in
        promise.resolve(mp.contactKey())
      }
    }

    AsyncFunction("getContactKey") { (promise: Promise) in
      SFMCSdk.requestPushSdk { mp in
        promise.resolve(mp.contactKey())
      }
    }

    AsyncFunction("getSdkState") { (promise: Promise) in
      promise.resolve(SFMCSdk.state())
    }

    AsyncFunction("track") { (name: String, attributes: Dictionary<String, Any>, promise: Promise) in
      let event = CustomEvent(name: name, attributes: attributes)!
      SFMCSdk.track(event: event)
      promise.resolve(true)
    }

    AsyncFunction("deleteMessage") { (messageId: String, promise: Promise) in
      SFMCSdk.requestPushSdk { mp in
        promise.resolve(mp.markMessageWithIdDeleted(messageId: messageId))
      }
    }

    AsyncFunction("getDeletedMessageCount") { (promise: Promise) in
      SFMCSdk.requestPushSdk { mp in
        promise.resolve(mp.getDeletedMessagesCount())
      }
    }

    AsyncFunction("getDeletedMessages") { (promise: Promise) in
      SFMCSdk.requestPushSdk { mp in
        promise.resolve(self.messagesToJSValue(messages: mp.getDeletedMessages()))
      }
    }

    AsyncFunction("getMessageCount") { (promise: Promise) in
      SFMCSdk.requestPushSdk { mp in
        promise.resolve(mp.getAllMessagesCount())
      }
    }

    AsyncFunction("getMessages") { (promise: Promise) in
      SFMCSdk.requestPushSdk { mp in
        promise.resolve(self.messagesToJSValue(messages: mp.getAllMessages()))
      }
    }

    AsyncFunction("getReadMessageCount") { (promise: Promise) in
      SFMCSdk.requestPushSdk { mp in
        promise.resolve(mp.getReadMessagesCount())
      }
    }

    AsyncFunction("getReadMessages") { (promise: Promise) in
      SFMCSdk.requestPushSdk { mp in
        promise.resolve(self.messagesToJSValue(messages: mp.getReadMessages()))
      }
    }

    AsyncFunction("getUnreadMessageCount") { (promise: Promise) in
      SFMCSdk.requestPushSdk { mp in
        promise.resolve(mp.getUnreadMessagesCount())
      }
    }

    AsyncFunction("getUnreadMessages") { (promise: Promise) in
      SFMCSdk.requestPushSdk { mp in
        promise.resolve(self.messagesToJSValue(messages: mp.getUnreadMessages()))
      }
    }

    AsyncFunction("markAllMessagesDeleted") { (promise: Promise) in
      SFMCSdk.requestPushSdk { mp in
        promise.resolve(mp.markAllMessagesDeleted())
      }
    }

    AsyncFunction("markAllMessagesRead") { (promise: Promise) in
      SFMCSdk.requestPushSdk { mp in
        promise.resolve(mp.markAllMessagesRead())
      }
    }

    AsyncFunction("refreshInbox") { (promise: Promise) in
      SFMCSdk.requestPushSdk { mp in
        let successful = mp.refreshMessages()
        if (successful == false) {
          promise.resolve(false)
        } else {
          // resolve previous promise if one exists
          self.refreshInboxPromise?.resolve(false)
          
          // queue latest promise
          self.refreshInboxPromise = promise
        }
      }
    }

    AsyncFunction("setMessageRead") { (messageId: String, promise: Promise) in
      SFMCSdk.requestPushSdk { mp in
        promise.resolve(mp.markMessageWithIdRead(messageId: messageId))
      }
    }
    
    AsyncFunction("trackMessageOpened") { (messageId: String, promise: Promise) in
      SFMCSdk.requestPushSdk { mp in
        let messages = mp.getAllMessages()
        
        if let messages = messages {
          let message = (messages as! [[AnyHashable : Any]]).first(where: {mp.messageId(forMessage: $0) == messageId })
          
          if let message = message {
            mp.trackMessageOpened(message)
            promise.resolve(true)
          } else {
            promise.resolve(false)
          }
        } else {
          promise.resolve(false)
        }
      }
    }

    AsyncFunction("isAnalyticsEnabled") { (promise: Promise) in
      SFMCSdk.requestPushSdk { mp in
        promise.resolve(mp.isAnalyticsEnabled())
      }
    }

    AsyncFunction("enableAnalytics") { (promise: Promise) in
      SFMCSdk.requestPushSdk { mp in
        mp.setAnalyticsEnabled(true)
        promise.resolve(mp.isAnalyticsEnabled())
      }
    }

    AsyncFunction("disableAnalytics") { (promise: Promise) in
      SFMCSdk.requestPushSdk { mp in
        mp.setAnalyticsEnabled(false)
        promise.resolve(mp.isAnalyticsEnabled())
      }
    }

  }
  
  @objc
  private func inboxMessagesNewInboxMessagesListener() {
    SFMCSdk.requestPushSdk { mp in
      self.sendEvent("onInboxResponse", [
        "messages": self.messagesToJSValue(messages: mp.getAllMessages())
      ])
    }
  }
  
  @objc
  private func inboxMessagesRefreshCompleteListener() {
    if (self.refreshInboxPromise != nil) {
      self.refreshInboxPromise!.resolve(true)
      self.refreshInboxPromise = nil
    }
  }
  
  internal func onLog(level: LogLevel, subsystem: String, category: LoggerCategory, message: String) {
    sendEvent("onLog", [
      "level": level.rawValue,
      "subsystem": subsystem,
      "category": category.rawValue,
      "message": message
    ])
  }
  
  private func messagesToJSValue(messages: [Any]?) -> [[AnyHashable : Any?]] {
    if let messages = messages {
      let dateFormatter = ISO8601DateFormatter()
      
      return (messages as! [NSDictionary]).map {message in
        let endDateUtc = message["endDateUtc"] as? Date
        let sendDateUtc = message["sendDateUtc"] as? Date
        let startDateUtc = message["startDateUtc"] as? Date
        let keys = message["keys"] as? [[AnyHashable : Any]]
        let mediaUrl = message["media.url"] as? String
        let mediaAltText = message["media.altText"] as? String
        
        return [
          "alert": message["alert"],
          "custom": message["custom"],
          "customKeys": keys != nil ? Dictionary(uniqueKeysWithValues: keys!.map{ ($0["key"] as? String, $0["value"] as? String)}) : [],
          "deleted": message["deleted"] as? Int == 0 ? false : true,
          "endDateUtc": endDateUtc != nil ? dateFormatter.string(from: endDateUtc!) : nil,
          "id": message["id"],
          "inboxMessage": message["inboxMessage"],
          "inboxMessageType": (message["inboxMessageType"] as? Int).map { $0 + 1 },
          "inboxSubtitle": message["inboxSubtitle"],
          "media": mediaUrl != nil ? ["url": mediaUrl, "altText": mediaAltText] : nil,
          "read": message["read"] as? Int == 0 ? false : true,
          "sendDateUtc": sendDateUtc != nil ? dateFormatter.string(from: sendDateUtc!) : nil,
          "sound": message["sound"],
          "startDateUtc": startDateUtc != nil ? dateFormatter.string(from: startDateUtc!) : nil,
          "subject": message["subject"],
          "subtitle":message["subtitle"],
          "title":message["title"],
          "url":message["url"],
      ]}
    } else {
      return []
    }
  }
}
