import { NativeModule, requireNativeModule } from 'expo';
import { ExpoMarketingCloudSdkModuleEvents, InboxMessage } from './ExpoMarketingCloudSdk.types';

declare class ExpoMarketingCloudSdkModule extends NativeModule<ExpoMarketingCloudSdkModuleEvents> {
	isPushEnabled(): Promise<boolean>;
	enablePush(): Promise<boolean>;
	disablePush(): Promise<boolean>;
	getSystemToken(): Promise<string | null>;
	setSystemToken(token: string): Promise<string | null>;
	getDeviceID(): Promise<string | null>;
	getAttributes(): Promise<Record<string, string> | null>;
	setAttribute(key: string, value: string): Promise<Record<string, string> | null>;
	clearAttribute(key: string): Promise<Record<string, string> | null>;
	addTag(tag: string): Promise<void>;
	removeTag(tag: string): Promise<void>;
	getTags(): Promise<string[] | null>;
	setContactKey(contactKey: string): Promise<string | null>;
	getContactKey(): Promise<string | null>;
	getSdkState(): Promise<string>;
	track(name: string, attributes: Record<string, string>): Promise<true>;
	deleteMessage(messageId: string): Promise<void>;
	getDeletedMessageCount(): Promise<number>;
	getDeletedMessages(): Promise<InboxMessage[]>;
	getMessageCount(): Promise<number>;
	getMessages(): Promise<InboxMessage[]>;
	getReadMessageCount(): Promise<number>;
	getReadMessages(): Promise<InboxMessage[]>;
	getUnreadMessageCount(): Promise<number>;
	getUnreadMessages(): Promise<InboxMessage[]>;
	markAllMessagesDeleted(): Promise<void>;
	markAllMessagesRead(): Promise<void>;
	refreshInbox(): Promise<boolean>;
	setMessageRead(messageId: string): Promise<void>;
	trackMessageOpened(messageId: string): Promise<boolean>;
	isAnalyticsEnabled(): Promise<boolean>;
	enableAnalytics(): Promise<boolean>;
	disableAnalytics(): Promise<boolean>;
}

// This call loads the native module object from the JSI.
export default requireNativeModule<ExpoMarketingCloudSdkModule>('ExpoMarketingCloudSdk');

