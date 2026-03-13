import { NativeModule, requireNativeModule } from 'expo';
import { ExpoMarketingCloudSdkModuleEvents } from './ExpoMarketingCloudSdk.types';

declare class ExpoMarketingCloudSdkModule extends NativeModule<ExpoMarketingCloudSdkModuleEvents> {
    [key: string]: any;
}

// This call loads the native module object from the JSI.
export default requireNativeModule<ExpoMarketingCloudSdkModule>('ExpoMarketingCloudSdk');

