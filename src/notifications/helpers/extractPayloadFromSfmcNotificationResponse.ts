import { Platform } from 'react-native'
import { SfmcAndroidNotificationResponse, SfmcIosNotificationResponse } from '../types'
import {
  normalizeSfmcAndroidNotificationResponsePayload,
  normalizeSfmcIosNotificationResponsePayload,
} from './normalizeSfmcNotificationResponsePayload'

export const extractPayloadFromSfmcAndroidNotificationResponse = (
  notificationResponse: SfmcAndroidNotificationResponse
) => {
  return normalizeSfmcAndroidNotificationResponsePayload(
    notificationResponse.notification.request.content.data.payload
  )
}

export const extractPayloadFromSfmcIosNotificationResponse = (
  notificationResponse: SfmcIosNotificationResponse
) => {
  return normalizeSfmcIosNotificationResponsePayload(
    notificationResponse.notification.request.trigger.payload
  )
}

export const extractPayloadFromSfmcNotificationResponse =
  Platform.OS === 'android'
    ? extractPayloadFromSfmcAndroidNotificationResponse
    : extractPayloadFromSfmcIosNotificationResponse
