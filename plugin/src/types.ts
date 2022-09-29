export type MarketingCloudSdkPluginProps = {
  /** Marketing Cloud app id */
  appId: string;
  /** Marketing Cloud access token */
  accessToken: string;
  /** Marketing Cloud server url */
  serverUrl: string;
  /**
   * (Android only) Local path to an image to use as the icon for push notifications.
   * 96x96 all-white png with transparency. We recommend following
   * [Google’s design guidelines](https://material.io/design/iconography/product-icons.html#design-principles).
   */
  iconFile?: string;
  /**
   * (Android only) Marketing Cloud FCM sender id. Defaults to `project_info.project_number`
   * defined in `android.googleServicesFile` (google-services.json) if defined.
   */
  senderId?: string;

  /**
   * Sets the configuration value to use for the Salesforce MarketingCloud Tenant Specific mid.
   */
  mid?: string;

  /**
   * Sets the configuration flag that enables or disables inbox services
   */
  inboxEnabled?: boolean

  /**
   * Sets the configuration flag that enables or disables location services
   */
  locationEnabled?: boolean;

  /**
   * Sets the configuration flag that enables or disables Salesforce MarketingCloud Analytics services
   */
  analyticsEnabled?: boolean;

  /**
   * Sets the configuration value which enables or disables application control over badging
   */
  applicationControlsBadging?: boolean;

  /**
   * Sets the configuration value which enables or disables application control over delaying SDK registration until a contact key is set
   */
  delayRegistrationUntilContactKeyIsSet?: boolean;
};
