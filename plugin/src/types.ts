export type MarketingCloudSdkPluginProps = {
  /** Marketing Cloud app id */
  appId: string;
  /** Marketing Cloud access token */
  accessToken: string;
  /** Marketing Cloud server url */
  serverUrl: string;
  /** Marketing Cloud FCM sender id */
  senderId?: string;
  /** Marketing Cloud mid */
  mid?: string;
  inboxEnabled?: boolean;
  locationEnabled?: boolean;
  analyticsEnabled?: boolean;
};
