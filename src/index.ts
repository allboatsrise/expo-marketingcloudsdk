import { NativeModulesProxy, EventEmitter, Subscription } from 'expo-modules-core';

import ExpoMarketingCloudSdkModule from './ExpoMarketingCloudSdkModule';
import { LogEventPayload } from './ExpoMarketingCloudSdk.types';

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
  return await ExpoMarketingCloudSdkModule.getSystemToken();
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

export async function getSdkState(): Promise<string> {
  return await ExpoMarketingCloudSdkModule.getSdkState();
}

export async function track(name: string, attributes: Record<string, string>): Promise<void> {
  return await ExpoMarketingCloudSdkModule.track(name, attributes);
}


const emitter = new EventEmitter(ExpoMarketingCloudSdkModule ?? NativeModulesProxy.ExpoMarketingCloudSdk);

export function addLogListener(listener: (event: LogEventPayload) => void): Subscription {
  return emitter.addListener<LogEventPayload>('onLog', listener);
}

export { LogEventPayload };
