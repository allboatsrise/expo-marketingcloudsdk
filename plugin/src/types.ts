import { z } from 'zod';

export type MarketingCloudSdkPluginProps = z.input<typeof MarketingCloudSDKPluginPropsSchema>
export type MarketingCloudSdkPluginValidProps = z.output<typeof MarketingCloudSDKPluginPropsSchema>

export const MarketingCloudSDKPluginPropsSchema = z.object({
  /** Marketing Cloud app id */
  appId: z.string({required_error: 'Must provide app id.'}).min(1),

  /** Marketing Cloud access token */
  accessToken: z.string({required_error: 'Must provide access token.'}).min(1),

  /** Marketing Cloud server url */
  serverUrl: z.string({required_error: 'Must provide server url.'}).url({message: 'Invalid server url.'}),

  /** Enable logging debug messages */
  debug: z.boolean().optional().default(false),

  /**
   * (Android only) Marketing Cloud FCM sender id. Defaults to `project_info.project_number`
   * defined in `android.googleServicesFile` (google-services.json) if defined.
   */
  senderId: z.string().min(1).optional(),

  /**
   * Sets the configuration value to use for the Salesforce MarketingCloud Tenant Specific mid.
   */
  mid: z.string().min(1).optional(),

  /**
   * Sets the configuration flag that enables or disables inbox services
   */
  inboxEnabled: z.boolean().optional().default(false),

  /**
   * Sets the configuration flag that enables or disables location services
   */
  locationEnabled: z.boolean().optional().default(false),

  /**
   * Sets the configuration flag that enables or disables Salesforce MarketingCloud Analytics services
   */
  analyticsEnabled: z.boolean().optional().default(true),

  /**
   * Sets the configuration value which enables or disables application control over badging
   */
  applicationControlsBadging: z.boolean().optional().default(false),

  /**
   * Sets the configuration value which enables or disables application control over delaying SDK registration until a contact key is set
   */
  delayRegistrationUntilContactKeyIsSet: z.boolean().optional().default(false),

  markNotificationReadOnInboxNotificationOpen: z.boolean().optional().default(false),

  /**
   * Makes an iOS Notification Service extension to enable rich media push notificaitons.
   */
  shouldCreateServiceExtension: z.boolean().optional().default(false),

  /**
     * Sets the value of the ios notification service extension (NSE). This is appened to the bundle identifier of the app. If not set the default value is `NotificationService`.
     */
  iosNseBundleIdSuffix: z.string().min(1).optional().default('NotificationService'),

  /**
   * Sets the value of the ios notification service extension (NSE) target dev team. Requered if `shouldCreateServiceExtension` is true.
   */
  iosDevTeamId: z.string().min(1).optional(),
}, {required_error: 'Must configure plugin options.'})
