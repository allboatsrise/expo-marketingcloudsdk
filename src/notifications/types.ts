import {
  Notification,
  NotificationContent,
  NotificationContentAndroid,
  NotificationRequest,
  NotificationResponse,
  PushNotificationTrigger,
} from 'expo-notifications'

// android
export interface SfmcAndroidNotificationResponse
  extends Omit<NotificationResponse, 'notification'> {
  notification: SfmcAndroidNotification
}

export interface SfmcAndroidNotification extends Omit<Notification, 'request'> {
  request: SfmcAndroidNotificationRequest
}

export interface SfmcAndroidNotificationRequest extends Omit<NotificationRequest, 'content'> {
  content: SfmcAndroidNotificationContent
}

export interface SfmcAndroidNotificationContent
  extends Omit<NotificationContent, 'data' | 'badge'> {
  data: SfmcAndroidNotificationContentData
  badge: NotificationContentAndroid['badge']
}

export interface SfmcAndroidNotificationContentData {
  payload: SfmcAndroidNotificationResponsePayload
  [key: string]: unknown
}

export interface SfmcAndroidNotificationResponsePayload
  extends SfmcNotificationResponseBasePayload {
  /** notification title */
  title?: string
  /** notification subtitle */
  subtitle?: string
  /** notification body text */
  alert: string
  /** notification sound */
  sound?: 'custom.caf' | 'default'
  /** Stringified JSON object with `jobId` and optional `messageKey` string properties. */
  _pb: string
  /** message type */
  _mt: '1' | '3' | '4' | '5' | '8'
}

// ios
export interface SfmcIosNotificationResponse extends Omit<NotificationResponse, 'notification'> {
  notification: SfmcIosNotification
}

export interface SfmcIosNotification extends Omit<Notification, 'request'> {
  request: SfmcIosNotificationRequest
}

export interface SfmcIosNotificationRequest extends Omit<NotificationRequest, 'trigger'> {
  trigger: SfmcIosNotificationTrigger
}

export interface SfmcIosNotificationTrigger extends Omit<PushNotificationTrigger, 'payload'> {
  payload: SfmcIosNotificationResponseTriggerPayload
}

export interface SfmcIosNotificationResponseTriggerPayload
  extends SfmcNotificationResponseBasePayload {
  _pb: {
    jobId: string
    messageKey?: string
  }
  aps: {
    'mutable-content': 1
    sound?: 'custom.caf' | 'default'
    badge?: number
    alert: {
      /** notification title */
      title?: string
      /** notification subtitle */
      subtitle?: string
      /** notification body text */
      body: string
    }
  }
  // message type
  // 1 - Outbound (default), 3 - Location Entry, 4 - Location Exit, 5 - Beacon, 8 - Inbox
  _mt: 1 | 3 | 4 | 5 | 8
}

// android / ios
export interface SfmcNotificationResponseBasePayload {
  /** Media Url */
  _mediaUrl?: string
  /** Media Alt Text */
  _mediaAlt?: string
  /** Message Instance Id (maybe) */
  _h: string
  /** Message Id */
  _m: string
  /** Url */
  _od?: string
  /** Request Id (aka Job Id) */
  _r: string
  /** Indicates this SFMC push notification. */
  _sid: 'SFMC'
  /** Cloud Page Url */
  _x?: string
  /** custom keys are strings set as extra properties */
  [customKey: string]: unknown
}

export interface SfmcNotificationResponsePayload {
  mediaUrl: string | undefined
  mediaAlt: string | undefined
  messageInstanceId: string
  messageId: string
  openDirectUrl: string | undefined
  requestId: string
  /** @deprecated Since Inbox 2.0 there is no guarantee that this is a cloud page - it could be any url. Use `url` instead. */
  cloudPageUrl: string | undefined
  url: string | undefined
  messageType: SfmcNotificationMessageType
  sound: SfmcNotificationSoundType | undefined
  title: string | undefined
  subtitle: string | undefined
  body: string
  customKeys: Record<string, string>
}

export enum SfmcNotificationMessageType {
  OUTBOUND = 1,
  LOCATION_ENTRY = 3,
  LOCATION_EXIT = 4,
  BEACON = 5,
  INBOX = 8,
}

export enum SfmcNotificationSoundType {
  DEFAULT = 'default',
  CUSTOM = 'custom.caf',
}
