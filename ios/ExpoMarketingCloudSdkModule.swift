import ExpoModulesCore
import SFMCSDK
import MarketingCloudSDK

public class ExpoMarketingCloudSdkModule: Module, ExpoMarketingCloudSdkLoggerDelegate {
  private var refreshInboxPromise: Promise?
  private var logger: ExpoMarketingCloudSdkLogger?
  
  // Each module class must implement the definition function. The definition consists of components
  // that describes the module's functionality and behavior.
  // See https://docs.expo.dev/modules/module-api for more details about available components.
  public func definition() -> ModuleDefinition {
    // Sets the name of the module that JavaScript code will use to refer to the module. Takes a string as an argument.
    // Can be inferred from module's class name, but it's recommended to set it explicitly for clarity.
    // The module will be accessible from `requireNativeModule('ExpoMarketingCloudSdk')` in JavaScript.
    Name("ExpoMarketingCloudSdk")

    // Defines event names that the module can send to JavaScript.
    Events("onLog", "onInboxResponse", "onRegistrationResponseSucceeded")
    
    OnStartObserving {
      if (logger == nil) {
        self.logger = ExpoMarketingCloudSdkLogger()
        self.logger!.delegate = self
        SFMCSdk.setLogger(logLevel: .debug, logOutputter: self.logger!)
      }
      
      NotificationCenter.default.addObserver(self, selector: #selector(self.inboxMessagesNewInboxMessagesListener), name: NSNotification.Name.SFMCInboxMessagesNewInboxMessages, object: nil)
      
      NotificationCenter.default.addObserver(self, selector: #selector(self.inboxMessagesRefreshCompleteListener), name: NSNotification.Name.SFMCInboxMessagesRefreshComplete, object: nil)
      
      NotificationCenter.default.addObserver(self, selector: #selector(self.foundationRegistrationResponseSucceededListener), name: NSNotification.Name.SFMCFoundationRegistrationResponseSucceeded, object: nil)
    }
    
    OnStopObserving {
      NotificationCenter.default.removeObserver(self, name: NSNotification.Name.SFMCInboxMessagesNewInboxMessages, object: nil)
      
      NotificationCenter.default.removeObserver(self, name: NSNotification.Name.SFMCInboxMessagesRefreshComplete, object: nil)
      
      NotificationCenter.default.removeObserver(self, name: NSNotification.Name.SFMCFoundationRegistrationResponseSucceeded, object: nil)
    }

    AsyncFunction("isPushEnabled") { (promise: Promise) in
      promise.resolve(SFMCSdk.mp.pushEnabled())
    }

    AsyncFunction("enablePush") { (promise: Promise) in
      SFMCSdk.mp.setPushEnabled(true)
      promise.resolve(SFMCSdk.mp.pushEnabled())
    }

    AsyncFunction("disablePush") { (promise: Promise) in
      SFMCSdk.mp.setPushEnabled(false)
      promise.resolve(SFMCSdk.mp.pushEnabled())
    }

    AsyncFunction("getSystemToken") { (promise: Promise) in
      promise.resolve(SFMCSdk.mp.deviceToken())
    }
    
    AsyncFunction("setSystemToken") { (token: String, promise: Promise) in
      do {
        let token = try ExpoMarketingCloudSdkDeviceToken.init(hexString: token)
        SFMCSdk.mp.setDeviceToken(token.data)
        promise.resolve(SFMCSdk.mp.deviceToken())
      } catch {
        promise.reject("invalid-hex-token", "Failed to convert token to data type")
      }
    }

    AsyncFunction("getDeviceID") { (promise: Promise) in
      promise.resolve(SFMCSdk.mp.deviceIdentifier())
    }

    AsyncFunction("getAttributes") { (promise: Promise) in
      promise.resolve(SFMCSdk.mp.attributes())
    }

    AsyncFunction("setAttribute") { (key: String, value: String, promise: Promise) in
      SFMCSdk.identity.setProfileAttribute(key, value)
      promise.resolve(SFMCSdk.mp.attributes())
    }

    AsyncFunction("clearAttribute") { (key: String, promise: Promise) in
      SFMCSdk.identity.clearProfileAttribute(key: key)
      promise.resolve(SFMCSdk.mp.attributes())
    }

    AsyncFunction("addTag") { (tag: String, promise: Promise) in
      promise.resolve(SFMCSdk.mp.addTag(tag))
    }

    AsyncFunction("removeTag") { (tag: String, promise: Promise) in
      promise.resolve(SFMCSdk.mp.removeTag(tag))
    }

    AsyncFunction("getTags") { (promise: Promise) in
      promise.resolve(Array(arrayLiteral: SFMCSdk.mp.tags()))
    }

    AsyncFunction("setContactKey") { (contactKey: String, promise: Promise) in
      SFMCSdk.identity.setProfileId(contactKey)
      promise.resolve(SFMCSdk.mp.contactKey())
    }

    AsyncFunction("getContactKey") { (promise: Promise) in
      promise.resolve(SFMCSdk.mp.contactKey())
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
      promise.resolve(SFMCSdk.mp.markMessageWithIdDeleted(messageId: messageId))
    }

    AsyncFunction("getDeletedMessageCount") { (promise: Promise) in
      promise.resolve(SFMCSdk.mp.getDeletedMessagesCount())
    }

    AsyncFunction("getDeletedMessages") { (promise: Promise) in
      promise.resolve(SFMCSdk.mp.getDeletedMessages())
    }

    AsyncFunction("getMessageCount") { (promise: Promise) in      promise.resolve(SFMCSdk.mp.getAllMessagesCount())
    }

    AsyncFunction("getMessages") { (promise: Promise) in
      promise.resolve(SFMCSdk.mp.getAllMessages())
    }

    AsyncFunction("getReadMessageCount") { (promise: Promise) in
      promise.resolve(SFMCSdk.mp.getReadMessagesCount())
    }

    AsyncFunction("getReadMessages") { (promise: Promise) in
      promise.resolve(SFMCSdk.mp.getReadMessages())
    }

    AsyncFunction("getUnreadMessageCount") { (promise: Promise) in
      promise.resolve(SFMCSdk.mp.getUnreadMessagesCount())
    }

    AsyncFunction("getUnreadMessages") { (promise: Promise) in
      promise.resolve(SFMCSdk.mp.getUnreadMessages())
    }

    AsyncFunction("markAllMessagesDeleted") { (promise: Promise) in
      promise.resolve(SFMCSdk.mp.markAllMessagesDeleted())
    }

    AsyncFunction("markAllMessagesRead") { (promise: Promise) in
      promise.resolve(SFMCSdk.mp.markAllMessagesRead())
    }

    AsyncFunction("refreshInbox") { (promise: Promise) in
      let successful = SFMCSdk.mp.refreshMessages()
      if (successful == false) {
        promise.resolve(false)
      } else {
        // resolve previous promise if one exists
        self.refreshInboxPromise?.resolve(false)
        
        // queue latest promise
        self.refreshInboxPromise = promise
      }
    }

    AsyncFunction("setMessageRead") { (messageId: String, promise: Promise) in
      promise.resolve(SFMCSdk.mp.markMessageWithIdRead(messageId: messageId))
    }
  }
  
  @objc
  private func inboxMessagesNewInboxMessagesListener() {
    sendEvent("onInboxResponse", [
      "messages": []
    ])
  }
  
  @objc func inboxMessagesRefreshCompleteListener() {
    if (self.refreshInboxPromise != nil) {
      self.refreshInboxPromise!.resolve(true)
      self.refreshInboxPromise = nil
    }
  }
  
  @objc
  func onLog(level: LogLevel, subsystem: String, category: LoggerCategory, message: String) {
    sendEvent("onLog", [
      "level": level.rawValue,
      "subsystem": subsystem,
      "category": category.rawValue,
      "message": message
    ])
  }
  
  @objc
  func foundationRegistrationResponseSucceededListener(response: [String: Any]) {
    sendEvent("onRegistrationResponseSucceeded", ["response": response])
  }
}
