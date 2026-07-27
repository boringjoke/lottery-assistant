export interface PasswordLoginRequest {
  account: string
  password: string
}

export interface RegisterRequest {
  username: string
  nickname: string
  password: string
  confirmPassword: string
}

export interface CurrentUser {
  userId: number
  nickname: string
  avatarUrl: string | null
  roles: string[]
}

export interface AuthSession extends CurrentUser {
  token: string
  expireTime: string
}
