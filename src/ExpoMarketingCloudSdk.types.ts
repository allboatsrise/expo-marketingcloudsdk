export type LogEventPayload = {
  level: string
  subsystem: string
  category: string
  message: string
  stackTrace?: string
}

export type Media = {
  altText: null | string
  url: null | string
}

export type InboxMessage = {
  alert: null | string
  custom: null | string
  customKeys: null | string
  deleted: boolean
  endDateUtc: null | string
  id: string
  media: null | Media
  read: boolean
  sendDateUtc: null | string
  sound: null | string
  startDateUtc: null | string
  subject: null | string
  title: null | string
  url: string
}

export type InboxResponsePayload = {
  messages: InboxMessage[]
}
