import { NativeModulesProxy, EventEmitter, Subscription } from 'expo-modules-core';

import ExpoMarketingCloudSdkModule from './ExpoMarketingCloudSdkModule';
import { InboxResponsePayload, LogEventPayload, InboxMessage } from './ExpoMarketingCloudSdk.types';

export async function isPushEnabled(): Promise<boolean> {
  return await ExpoMarketingCloudSdkModule.isPushEnabled();
}

export async function enablePush(): Promise<void> {
  return await ExpoMarketingCloudSdkModule.enablePush();
}

export async function disablePush(): Promise<void> {
  return await ExpoMarketingCloudSdkModule.disablePush();
}

export async function getSystemToken(): Promise<string> {
  return await ExpoMarketingCloudSdkModule.getSystemToken();
}

export async function setSystemToken(token: string): Promise<void> {
  return await ExpoMarketingCloudSdkModule.setSystemToken(token);
}

export async function getAttributes(): Promise<Record<string, string>> {
  return await ExpoMarketingCloudSdkModule.getAttributes();
}

export async function setAttribute(key: string, value: string): Promise<void> {
  return await ExpoMarketingCloudSdkModule.setAttribute(key, value);
}

export async function clearAttribute(key: string): Promise<void> {
  return await ExpoMarketingCloudSdkModule.clearAttribute(key);
}

export async function addTag(tag: string): Promise<void> {
  return await ExpoMarketingCloudSdkModule.addTag(tag);
}

export async function removeTag(tag: string): Promise<void> {
  return await ExpoMarketingCloudSdkModule.removeTag(tag);
}

export async function getTags(): Promise<string[]> {
  return await ExpoMarketingCloudSdkModule.getTags();
}

export async function setContactKey(contactKey: string): Promise<void> {
  return await ExpoMarketingCloudSdkModule.setContactKey(contactKey);
}

export async function getContactKey(): Promise<string> {
  return await ExpoMarketingCloudSdkModule.getContactKey();
}

export async function getSdkState(): Promise<Record<string, unknown>> {
  const state = await ExpoMarketingCloudSdkModule.getSdkState();
  return JSON.parse(state);
}

export async function track(name: string, attributes: Record<string, string>): Promise<void> {
  return await ExpoMarketingCloudSdkModule.track(name, attributes);
}

export async function deleteMessage(messageId: string): Promise<void> {
  return await ExpoMarketingCloudSdkModule.deleteMessage(messageId);
}

export async function getDeletedMessageCount(): Promise<number> {
  return await ExpoMarketingCloudSdkModule.getDeletedMessageCount();
}

export async function getDeletedMessages(): Promise<InboxMessage[]> {
  return await ExpoMarketingCloudSdkModule.getDeletedMessages();
}

export async function getMessageCount(): Promise<number> {
  return await ExpoMarketingCloudSdkModule.getMessageCount();
}

export async function getMessages(): Promise<InboxMessage[]> {
  return await ExpoMarketingCloudSdkModule.getMessages();
}

export async function getReadMessageCount(): Promise<number> {
  return await ExpoMarketingCloudSdkModule.getReadMessageCount();
}

export async function getReadMessages(): Promise<InboxMessage[]> {
  return await ExpoMarketingCloudSdkModule.getReadMessages();
}

export async function getUnreadMessageCount(): Promise<number> {
  return await ExpoMarketingCloudSdkModule.getUnreadMessageCount();
}

export async function getUnreadMessages(): Promise<InboxMessage[]> {
  return await ExpoMarketingCloudSdkModule.getUnreadMessages();
}

export async function markAllMessagesDeleted(): Promise<void> {
  return await ExpoMarketingCloudSdkModule.markAllMessagesDeleted();
}

export async function markAllMessagesRead(): Promise<void> {
  return await ExpoMarketingCloudSdkModule.markAllMessagesRead();
}

export async function refreshInbox(): Promise<boolean> {
  return await ExpoMarketingCloudSdkModule.refreshInbox();
}

export async function setMessageRead(messageId: string): Promise<void> {
  return await ExpoMarketingCloudSdkModule.setMessageRead(messageId);
}


const emitter = new EventEmitter(ExpoMarketingCloudSdkModule ?? NativeModulesProxy.ExpoMarketingCloudSdk);

export function addLogListener(listener: (event: LogEventPayload) => void): Subscription {
  return emitter.addListener<LogEventPayload>('onLog', listener);
}

export function addInboxResponseListener(listener: (event: InboxResponsePayload) => void): Subscription {
  return emitter.addListener<InboxResponsePayload>('onInboxResponse', listener)
}

export { LogEventPayload, InboxResponsePayload, InboxMessage }
