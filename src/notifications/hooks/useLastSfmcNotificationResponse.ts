import { useMemo } from 'react'
import { Platform } from 'react-native'
import { useLastNotificationResponse } from 'expo-notifications'
import {
  isSfmcAndroidNotificationResponse,
  isSfmcIosNotificationResponse,
} from '../helpers/isSfmcNotificationResponse'
import {
  extractPayloadFromSfmcAndroidNotificationResponse,
  extractPayloadFromSfmcIosNotificationResponse,
} from '../helpers/extractPayloadFromSfmcNotificationResponse'

export const useLastSfmcAndroidNotificationResponse = () => {
  const response = useLastNotificationResponse()

  return useMemo(() => {
    if (!response || !isSfmcAndroidNotificationResponse(response))
      return { response: null, payload: null }
    return { response, payload: extractPayloadFromSfmcAndroidNotificationResponse(response) }
  }, [response])
}

export const useLastSfmcIosNotificationResponse = () => {
  const response = useLastNotificationResponse()

  return useMemo(() => {
    if (!response || !isSfmcIosNotificationResponse(response))
      return { response: null, payload: null }
    return { response, payload: extractPayloadFromSfmcIosNotificationResponse(response) }
  }, [response])
}

export const useLastSfmcNotificationResponse =
  Platform.OS === 'android'
    ? useLastSfmcAndroidNotificationResponse
    : useLastSfmcIosNotificationResponse
