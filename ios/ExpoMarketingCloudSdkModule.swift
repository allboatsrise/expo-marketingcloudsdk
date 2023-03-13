import ExpoModulesCore

public class ExpoMarketingCloudSdkModule: Module {
  // Each module class must implement the definition function. The definition consists of components
  // that describes the module's functionality and behavior.
  // See https://docs.expo.dev/modules/module-api for more details about available components.
  public func definition() -> ModuleDefinition {
    // Sets the name of the module that JavaScript code will use to refer to the module. Takes a string as an argument.
    // Can be inferred from module's class name, but it's recommended to set it explicitly for clarity.
    // The module will be accessible from `requireNativeModule('ExpoMarketingCloudSdk')` in JavaScript.
    Name("ExpoMarketingCloudSdk")

    // Defines event names that the module can send to JavaScript.
    Events("onLog")
  }
}
