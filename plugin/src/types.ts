export type MarketingCloudSdkPluginProps = {
  /** Marketing Cloud app id */
  appId: string;
  /** Marketing Cloud access token */
  accessToken: string;
  /** Marketing Cloud server url */
  serverUrl: string;
  /**
   * (Android only) Marketing Cloud FCM sender id. Defaults to `project_info.project_number`
   * defined in `android.googleServicesFile` (google-services.json) if defined.
   */
  senderId?: string;
  /** Marketing Cloud mid */
  mid?: string;
  inboxEnabled?: boolean;
  locationEnabled?: boolean;
  analyticsEnabled?: boolean;
  /**
   * (iOS only) Environment of the app: either 'development' or 'production'. Defaults to 'development'.
   * @default 'development'
   */
  mode?: 'development' | 'production';
};
