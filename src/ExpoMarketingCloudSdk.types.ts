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
  custom?: null | Record<string, string>
  deleted: boolean | number
  endDateUtc: null | string
  id: string
  media: null | Media
  read: boolean | number
  sendDateUtc: null | string
  sound: null | string
  startDateUtc: null | string
  subject: null | string
  title: null | string
  subtitle?: null | string
  url: string
}

export type InboxResponsePayload = {
  messages: InboxMessage[]
}
