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
      
      SFMCSdk.requestPushSdk { mp in
        mp.setRegistrationCallback() { response in
          self.sendEvent("onRegistrationResponseSucceeded", ["response": response])
        }
      }
    }
    
    OnStopObserving {
      NotificationCenter.default.removeObserver(self, name: NSNotification.Name.SFMCInboxMessagesNewInboxMessages, object: nil)
      
      NotificationCenter.default.removeObserver(self, name: NSNotification.Name.SFMCInboxMessagesRefreshComplete, object: nil)
      
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
        promise.resolve(Array(arrayLiteral: mp.tags()))
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
          "media": mediaUrl != nil ? ["url": mediaUrl, "altText": mediaAltText] : nil,
          "read": message["deleted"] as? Int == 0 ? false : true,
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
