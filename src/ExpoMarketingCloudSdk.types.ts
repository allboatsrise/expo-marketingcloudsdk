export type LogEventPayload = {
  level: string
  subsystem: string
  category: string
  message: string
  stackTrace?: string
}

export type Media = {
  altText?: string
  url?: string
}

export type InboxMessage = {
  alert?: string
  custom?: string
  customKeys?: Record<string, string>
  deleted: boolean
  endDateUtc?: string
  id: string
  media?: Media
  read: boolean
  sendDateUtc?: string
  sound?: string
  startDateUtc?: string
  subject?: string
  title?: string
  url: string
}

export type InboxResponsePayload = {
  messages: InboxMessage[]
}
