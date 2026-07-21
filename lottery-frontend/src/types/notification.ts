export type NotificationReadStatus = 'UNREAD' | 'READ'

export interface UserNotification {
  id: number
  userId: number
  notificationType: string
  businessType: string
  businessKey: string
  title: string
  content: string
  readStatus: NotificationReadStatus
  readTime: string | null
  createTime: string | null
  updateTime: string | null
}

export interface UserNotificationPage {
  pageNo: number
  pageSize: number
  total: number
  pages: number
  notifications: UserNotification[]
}
