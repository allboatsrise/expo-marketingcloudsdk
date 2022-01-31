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
  // mid?: string;
  // inboxEnabled?: boolean;
  // locationEnabled?: boolean;
  analyticsEnabled?: boolean;
  /**
   * (iOS only) Environment of the app: either 'development' or 'production'. Defaults to 'development'.
   * @default 'development'
   */
  mode?: 'development' | 'production';
};
