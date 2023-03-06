import fs from 'fs'
import { ConfigPlugin, AndroidConfig, withStringsXml} from '@expo/config-plugins';
import { MarketingCloudSdkPluginProps } from '../types';
import { getGoogleServicesFilePath } from './helpers';

export const withAndroidConfig: ConfigPlugin<MarketingCloudSdkPluginProps> = (config, props) => {
  // Set configuration on resources
  config = withConfiguration(config, props)

  return config;
};

const withConfiguration: ConfigPlugin<MarketingCloudSdkPluginProps> = (config, props) => {
    return withStringsXml(config, config => {

      let senderId = props.senderId
  
      if (!senderId) {
        const googleServicesFilePath = getGoogleServicesFilePath(config, config.modRequest.projectRoot)
        const googleServices = JSON.parse(fs.readFileSync(googleServicesFilePath, 'utf8'));
        senderId = googleServices?.project_info?.project_number
  
        if (!senderId) {
          throw new Error(`Failed to extract sender id from google services file. (path: ${googleServicesFilePath})`)
        }
      }

      // Helper to add string.xml JSON items or overwrite existing items with the same name.
      config.modResults = AndroidConfig.Strings.setStringItem(
        [
          // XML represented as JSON
          { $: { name: 'expo_marketingcloudsdk_app_id', translatable: 'false' }, _: props.appId },
          { $: { name: 'expo_marketingcloudsdk_access_token', translatable: 'false' }, _: props.accessToken },
          { $: { name: 'expo_marketingcloudsdk_server_url', translatable: 'false' }, _: props.serverUrl },
          { $: { name: 'expo_marketingcloudsdk_sender_id', translatable: 'false' }, _: senderId },
          { $: { name: 'expo_marketingcloudsdk_analytics_enabled', translatable: 'false' }, _: props.analyticsEnabled ? 'true' : 'false' },
          { $: { name: 'expo_marketingcloudsdk_delay_registration_until_contact_key_is_set', translatable: 'false' }, _: props.delayRegistrationUntilContactKeyIsSet ? 'true' : 'false' },
          { $: { name: 'expo_marketingcloudsdk_inbox_enabled', translatable: 'false' }, _: props.inboxEnabled ? 'true' : 'false' },
          { $: { name: 'expo_marketingcloudsdk_mark_message_read_on_inbox_notification_open', translatable: 'false' }, _: props.markNotificationReadOnInboxNotificationOpen ? 'true' : 'false' },
        ],
        config.modResults
      );
    
    return config;
  });
}

