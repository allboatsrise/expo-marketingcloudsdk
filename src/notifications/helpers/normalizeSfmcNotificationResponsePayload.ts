import {
  SfmcAndroidNotificationResponsePayload,
  SfmcNotificationResponsePayload,
  SfmcIosNotificationResponseTriggerPayload,
  SfmcNotificationMessageType,
  SfmcNotificationSoundType,
} from '../types'

export const normalizeSfmcAndroidNotificationResponsePayload = (
  payload: SfmcAndroidNotificationResponsePayload
): SfmcNotificationResponsePayload => {
  const {
    _h,
    _m,
    _mt,
    _pb,
    _r,
    _sid,
    alert,
    _mediaAlt,
    _mediaUrl,
    _od,
    _x,
    sound,
    subtitle,
    title,
    ...customKeys
  } = payload
  return {
    body: alert,
    cloudPageUrl: _x,
    mediaAlt: _mediaAlt,
    mediaUrl: _mediaUrl,
    messageId: _m,
    messageType: convertMessageTypeStringToMessageTypeEnum(_mt),
    messageInstanceId: _h,
    openDirectUrl: _od,
    requestId: _r,
    sound: convertSoundStringToSoundEnum(sound),
    subtitle: subtitle,
    title: title,
    customKeys: extractCustomKeysFromObject(customKeys),
  }
}

export const normalizeSfmcIosNotificationResponsePayload = (
  payload: SfmcIosNotificationResponseTriggerPayload
): SfmcNotificationResponsePayload => {
  const { _h, _m, _mt, _pb, _r, _sid, aps, _mediaAlt, _mediaUrl, _od, _x, ...customKeys } = payload
  return {
    body: aps.alert.body,
    cloudPageUrl: _x,
    mediaAlt: _mediaAlt,
    mediaUrl: _mediaUrl,
    messageId: _m,
    messageType: _mt,
    messageInstanceId: _h,
    openDirectUrl: _od,
    requestId: _r,
    sound: convertSoundStringToSoundEnum(aps.sound),
    subtitle: aps.alert.subtitle,
    title: aps.alert.title,
    customKeys: extractCustomKeysFromObject(customKeys),
  }
}

const convertMessageTypeStringToMessageTypeEnum = (
  messageType: SfmcAndroidNotificationResponsePayload['_mt']
): SfmcNotificationMessageType => {
  switch (messageType) {
    case '1':
      return SfmcNotificationMessageType.OUTBOUND
    case '3':
      return SfmcNotificationMessageType.LOCATION_ENTRY
    case '4':
      return SfmcNotificationMessageType.LOCATION_EXIT
    case '5':
      return SfmcNotificationMessageType.BEACON
    case '8':
      return SfmcNotificationMessageType.INBOX
    default:
      return messageType
  }
}

const convertSoundStringToSoundEnum = (
  sound: SfmcAndroidNotificationResponsePayload['sound']
): SfmcNotificationSoundType | undefined => {
  switch (sound) {
    case 'custom.caf':
      return SfmcNotificationSoundType.CUSTOM
    case 'default':
      return SfmcNotificationSoundType.DEFAULT
    default:
      return sound
  }
}

const extractCustomKeysFromObject = (obj: Record<string, unknown>): Record<string, string> => {
  return Object.entries(obj).reduce(
    (carry, [key, value]) => {
      if (typeof value === 'string') {
        carry[key] = value
      }
      return carry
    },
    {} as Record<string, string>
  )
}
