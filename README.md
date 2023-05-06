# @allboatsrise/expo-marketingcloudsdk

This is an Expo module that provides a wrapper around the Salesforce Marketing Cloud SDK for iOS and Android.

It allows Expo-based apps to integrate with the Marketing Cloud SDK.

## Installation

To install the package use your prefered package manager:

```bash
npm install expo-marketingcloudsdk --save
```
or
```bash
yarn add expo-marketingcloudsdk
```

## Plugin setup
#### [View parameters](#plugin-parameters)

Add package to `plugins` in `app.js`/`app.config.js`.

```javascript
expo: {
    ...
    plugins: [['expo-marketingcloudsdk', {
        appId: 'MARKETING_CLOUD_APP_ID',
        accessToken: "MARKETING_CLOUD_ACCESS_TOKEN",
        serverUrl: MARKETING_CLOUD_SERVER_URL,
        }]]
}
```

## Plugin parameters

| Parameter                                     | Type    | Required | Description                                                                                                                                         |
| --------------------------------------------- | ------- | -------- | --------------------------------------------------------------------------------------------------------------------------------------------------- |
| `appId`                                       | string  | Yes      | Marketing Cloud app id                                                                                                                              |
| `accessToken`                                 | string  | Yes      | Marketing Cloud access token                                                                                                                        |
| `serverUrl`                                   | string  | Yes      | Marketing Cloud server url                                                                                                                          |
| `senderId` (Android only)                     | string  | No       | Marketing Cloud FCM sender id. Defaults to `project_info.project_number` defined in `android.googleServicesFile` (google-services.json) if defined. |
| `mid`                                         | string  | No       | Sets the configuration value to use for the Salesforce MarketingCloud Tenant Specific mid.                                                          |
| `inboxEnabled`                                | boolean | No       | Sets the configuration flag that enables or disables inbox services                                                                                 |
| `locationEnabled`                             | boolean | No       | Sets the configuration flag that enables or disables location services                                                                              |
| `analyticsEnabled`                            | boolean | No       | Sets the configuration flag that enables or disables Salesforce MarketingCloud Analytics services                                                   |
| `applicationControlsBadging`                  | boolean | No       | Sets the configuration value which enables or disables application control over badging                                                             |
| `delayRegistrationUntilContactKeyIsSet`       | boolean | No       | Sets the configuration value which enables or disables application control over delaying SDK registration until a contact key is set                |
| `markNotificationReadOnInboxNotificationOpen` | boolean | No       | Sets the configuration value which enables or disables marking inbox notifications as read on open (Android only)                                   |

# Usage

To use the MarketingCloud SDK in your Expo app, you first need to import requireNativeModule from expo-modules-core:
Next, you can create an instance of the MarketingCloud SDK by calling `requireNativeModule` with the module name, `ExpoMarketingCloudSdk`:

```typescript
import { requireNativeModule } from 'expo-modules-core'

const MarketingCloud = requireNativeModule('ExpoMarketingCloudSdk')

const logIsPushEnabled = async () => {
  const isEnabled = await MarketingCloud.isPushEnabled()
  console.log(isEnabled)
}
```
