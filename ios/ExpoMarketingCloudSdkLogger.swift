
protocol ExpoMarketingCloudSdkLoggerDelegate {
  func onLog(level
             : SFMCSDK.LogLevel, subsystem
             : String, category
             : SFMCSDK.LoggerCategory, message
             : String)
}

class ExpoMarketingCloudSdkLogger : LogOutputter {
  var delegate : ExpoMarketingCloudSdkLoggerDelegate?
  
  override func out(level
                    : LogLevel, subsystem
                    : String, category
                    : LoggerCategory, message
                    : String) {
    
    self.delegate?.onLog(level
                         : level, subsystem
                         : subsystem, category
                         : category, message
                         : message)
  }
}
