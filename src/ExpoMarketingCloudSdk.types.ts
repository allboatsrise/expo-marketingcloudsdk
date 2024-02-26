export type LogEventPayload = {
  level: string
  subsystem: string
  category: string
  message: string
  stackTrace?: string
}

export type Media = {
  altText: string | null
  url: string
}

export type InboxMessage = {
  /** Push notification message body */
  alert: string | null
  /** Custom payload defined via POST /push/v1/messageContact/{messageId}/send */
  custom: Record<string, unknown> | null
  /** Custom key/value pairs defined for the app and set in message */
  customKeys: Record<string, string> | null
  deleted: boolean
  endDateUtc: string
  id: string
  media: Media | null
  /** Indicates whether inbox message has been marked as read */
  read: boolean
  sendDateUtc: string
  sound: 'default' | 'custom.caf' | null
  /** Inbox Message Title */
  subject: string
  /** Push notification title */
  title: string | null
  startDateUtc: string
  /** Push notification subtitle */
  subtitle: string | null
  /** Url to SFMC CloudPage */
  url: string
}

export type InboxResponsePayload = {
  messages: InboxMessage[]
}

export type RegistrationResponseSucceededPayload = {
  response: {
    "quietPushEnabled" : boolean,
    "location_Enabled" : boolean,
    "registrationId" : string,
    "timeZone" : string,
    "locale" : string,
    "etAppId" : string,
    "attributes" : Array<
      {
        "key" : string,
        "value" : string
      }
    >,
    "proximity_Enabled" : boolean,
    "subscriberKey" : string,
    "platform" : string,
    "sdk_Version" : string,
    "language" : string,
    "app_Version" : string,
    "deviceID" : string,
    "tags" : string[],
    "hwid" : string,
    "push_Enabled" : boolean,
    "device_Token" : string,
    "dST" : boolean,
    "platform_Version" : string
  }
}
