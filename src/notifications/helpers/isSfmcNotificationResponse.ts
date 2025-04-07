import { Platform } from 'react-native'
import { NotificationResponse } from 'expo-notifications'
import {
  SfmcAndroidNotificationResponse,
  SfmcIosNotificationResponse,
  SfmcNotificationResponseBasePayload,
} from '../types'

export const isSfmcAndroidNotificationResponse = (
  notificationResponse: NotificationResponse
): notificationResponse is SfmcAndroidNotificationResponse => {
  const payload: unknown = notificationResponse.notification.request.content.data.payload
  return isSfmcNotificationResponseBasePayload(payload)
}

export const isSfmcIosNotificationResponse = (
  notificationResponse: NotificationResponse
): notificationResponse is SfmcIosNotificationResponse => {
  const trigger = notificationResponse.notification.request.trigger

  if (!trigger) return false
  if (!('type' in trigger)) return false
  if (trigger.type !== 'push') return false

  const payload: unknown = trigger.payload

  return isSfmcNotificationResponseBasePayload(payload)
}

export const isSfmcNotificationResponse =
  Platform.OS === 'android' ? isSfmcAndroidNotificationResponse : isSfmcIosNotificationResponse

const isSfmcNotificationResponseBasePayload = (
  payload: unknown
): payload is SfmcNotificationResponseBasePayload => {
  if (typeof payload !== 'object') return false
  if (payload === null) return false

  if (!('_sid' in payload) || payload._sid !== 'SFMC') return false

  return true
}
