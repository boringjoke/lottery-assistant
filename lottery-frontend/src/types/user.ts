export interface UserProfile {
  userId: number
  nickname: string
  avatarUrl: string | null
  status: string
  roles: string[]
  username: string | null
  maskedPhone: string | null
  maskedEmail: string | null
  createTime: string | null
  lastLoginTime: string | null
}

export interface UserProfileUpdateRequest {
  nickname: string
  avatarUrl: string | null
}
