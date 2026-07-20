<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { logout } from '@/api/auth'
import { fetchUserProfile, updateUserProfile } from '@/api/user'
import ProfileShell from '@/components/profile/ProfileShell.vue'
import type { CurrentUser } from '@/types/auth'
import type { UserProfile } from '@/types/user'
import { getErrorMessage } from '@/utils/lotteryFormat'

const router = useRouter()
const profile = ref<UserProfile | null>(null)
const loading = ref(false)
const errorMessage = ref('')
const noticeMessage = ref('')
const authLoading = ref(false)
const saving = ref(false)
const nicknameDraft = ref('')
const avatarDraft = ref<string | null>(null)

const avatarOptions = Array.from({ length: 8 }, (_, index) => {
  const avatarNo = String(index + 1).padStart(2, '0')

  return `/avatars/avatar-${avatarNo}.svg`
})

const currentUser = computed<CurrentUser | null>(() => {
  if (!profile.value) {
    return null
  }

  return {
    userId: profile.value.userId,
    nickname: profile.value.nickname,
    avatarUrl: profile.value.avatarUrl,
    roles: profile.value.roles,
  }
})

const userInitial = computed(() => profile.value?.nickname?.trim().slice(0, 1) || '用')
const roleText = computed(() => {
  if (!profile.value?.roles.length) {
    return '-'
  }
  if (profile.value.roles.includes('ADMIN')) {
    return '管理员'
  }

  return '普通用户'
})
const statusText = computed(() => profile.value?.status === 'ACTIVE' ? '正常' : profile.value?.status || '-')
const hasProfileChanges = computed(() => {
  if (!profile.value) {
    return false
  }

  return nicknameDraft.value.trim() !== profile.value.nickname || avatarDraft.value !== profile.value.avatarUrl
})

function formatTime(value: string | null | undefined): string {
  if (!value) {
    return '-'
  }

  return value.replace('T', ' ').slice(0, 16)
}

async function loadProfile() {
  loading.value = true
  errorMessage.value = ''

  try {
    profile.value = await fetchUserProfile()
    resetDraft()
  } catch (error) {
    const message = getErrorMessage(error)
    if (message.includes('请先登录')) {
      await router.push({
        path: '/login',
        query: { redirect: '/profile' },
      })
      return
    }

    errorMessage.value = message
    profile.value = null
  } finally {
    loading.value = false
  }
}

function resetDraft() {
  nicknameDraft.value = profile.value?.nickname ?? ''
  avatarDraft.value = profile.value?.avatarUrl ?? null
  noticeMessage.value = ''
}

async function saveProfile() {
  if (!profile.value) {
    return
  }

  saving.value = true
  errorMessage.value = ''
  noticeMessage.value = ''

  try {
    profile.value = await updateUserProfile({
      nickname: nicknameDraft.value,
      avatarUrl: avatarDraft.value,
    })
    resetDraft()
    noticeMessage.value = '个人资料已保存'
  } catch (error) {
    errorMessage.value = getErrorMessage(error)
  } finally {
    saving.value = false
  }
}

async function handleLogout() {
  authLoading.value = true
  try {
    await logout()
  } finally {
    profile.value = null
    authLoading.value = false
    await router.push('/login')
  }
}

onMounted(loadProfile)
</script>

<template>
  <ProfileShell
    :current-user="currentUser"
    :loading="authLoading || loading"
    active-nav="profile"
    @logout="handleLogout"
  >
    <section class="profile-card">
          <div class="profile-card__header">
            <div>
              <h1>个人资料</h1>
              <p>查看你的账号基本信息</p>
            </div>
            <button class="profile-refresh-button" type="button" :disabled="loading" @click="loadProfile">
              {{ loading ? '加载中' : '刷新' }}
            </button>
          </div>

          <div v-if="loading && !profile" class="profile-state">正在加载个人资料</div>
          <div v-else-if="errorMessage && !profile" class="profile-state profile-state--error">{{ errorMessage }}</div>

          <template v-else-if="profile">
            <div v-if="errorMessage" class="profile-state profile-state--error">{{ errorMessage }}</div>
            <div v-if="noticeMessage" class="profile-state profile-state--success">{{ noticeMessage }}</div>

            <div class="profile-overview">
              <div class="profile-avatar" aria-hidden="true">
                <img v-if="profile.avatarUrl" :src="profile.avatarUrl" alt="" />
                <span v-else>{{ userInitial }}</span>
              </div>
              <div>
                <div class="profile-name-row">
                  <strong>{{ profile.nickname }}</strong>
                  <span class="profile-status">{{ statusText }}</span>
                </div>
                <p>{{ roleText }}</p>
              </div>
            </div>

            <form class="profile-edit-form" @submit.prevent="saveProfile">
              <div class="profile-field">
                <label for="profileNickname">昵称</label>
                <input
                  id="profileNickname"
                  v-model="nicknameDraft"
                  type="text"
                  maxlength="64"
                  autocomplete="nickname"
                />
              </div>

              <fieldset class="profile-avatar-picker">
                <legend>默认头像</legend>
                <div class="profile-avatar-grid">
                  <button
                    v-for="avatarUrl in avatarOptions"
                    :key="avatarUrl"
                    class="profile-avatar-option"
                    :class="{ selected: avatarDraft === avatarUrl }"
                    type="button"
                    :aria-pressed="avatarDraft === avatarUrl"
                    @click="avatarDraft = avatarUrl"
                  >
                    <img :src="avatarUrl" alt="" />
                  </button>
                </div>
              </fieldset>

              <div class="profile-form-actions">
                <button
                  class="profile-save-button"
                  type="submit"
                  :disabled="saving || loading || !hasProfileChanges"
                >
                  {{ saving ? '保存中' : '保存修改' }}
                </button>
                <button
                  class="profile-cancel-button"
                  type="button"
                  :disabled="saving || loading || !hasProfileChanges"
                  @click="resetDraft"
                >
                  取消
                </button>
              </div>
            </form>

            <dl class="profile-info-grid">
              <div>
                <dt>用户名</dt>
                <dd>{{ profile.username || '-' }}</dd>
              </div>
              <div>
                <dt>手机号</dt>
                <dd>{{ profile.maskedPhone || '-' }}</dd>
              </div>
              <div>
                <dt>邮箱</dt>
                <dd>{{ profile.maskedEmail || '-' }}</dd>
              </div>
              <div>
                <dt>角色</dt>
                <dd>{{ roleText }}</dd>
              </div>
              <div>
                <dt>注册时间</dt>
                <dd>{{ formatTime(profile.createTime) }}</dd>
              </div>
              <div>
                <dt>最近登录</dt>
                <dd>{{ formatTime(profile.lastLoginTime) }}</dd>
              </div>
            </dl>
          </template>
        </section>
  </ProfileShell>
</template>

<style scoped>
.profile-card {
  max-width: 760px;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  background: #ffffff;
  padding: 28px;
  box-shadow: 0 12px 36px rgb(15 23 42 / 0.06);
}

.profile-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid #f1f5f9;
  padding-bottom: 22px;
}

.profile-card__header h1 {
  margin: 0;
  color: #111827;
  font-size: 22px;
  font-weight: 900;
}

.profile-card__header p {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 14px;
}

.profile-refresh-button {
  height: 36px;
  border: 1px solid #dbeafe;
  border-radius: 10px;
  background: #eff6ff;
  color: #1d4ed8;
  padding: 0 14px;
  font-size: 13px;
  font-weight: 900;
  cursor: pointer;
}

.profile-refresh-button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.profile-state {
  margin-top: 24px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #f8fafc;
  color: #64748b;
  padding: 18px;
  font-size: 14px;
  font-weight: 800;
}

.profile-state--error {
  border-color: #fecaca;
  background: #fef2f2;
  color: #b91c1c;
}

.profile-state--success {
  border-color: #bbf7d0;
  background: #f0fdf4;
  color: #15803d;
}

.profile-overview {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 28px 0;
}

.profile-avatar {
  display: flex;
  width: 80px;
  height: 80px;
  flex: 0 0 80px;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 1px solid #bfdbfe;
  border-radius: 999px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 28px;
  font-weight: 900;
  box-shadow: 0 8px 20px rgb(37 99 235 / 0.12);
}

.profile-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-name-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.profile-name-row strong {
  font-size: 22px;
}

.profile-status {
  border-radius: 999px;
  background: #dcfce7;
  color: #15803d;
  padding: 4px 9px;
  font-size: 12px;
  font-weight: 900;
}

.profile-overview p {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 14px;
  font-weight: 800;
}

.profile-edit-form {
  display: grid;
  gap: 18px;
  border-top: 1px solid #f1f5f9;
  border-bottom: 1px solid #f1f5f9;
  padding: 22px 0;
}

.profile-field {
  display: grid;
  max-width: 420px;
  gap: 8px;
}

.profile-field label,
.profile-avatar-picker legend {
  color: #334155;
  font-size: 14px;
  font-weight: 900;
}

.profile-field input {
  height: 42px;
  border: 1px solid #dbeafe;
  border-radius: 12px;
  background: #ffffff;
  color: #0f172a;
  padding: 0 13px;
  font-size: 14px;
  font-weight: 800;
  outline: none;
}

.profile-field input:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgb(191 219 254 / 0.7);
}

.profile-avatar-picker {
  display: grid;
  gap: 12px;
  min-width: 0;
  margin: 0;
  border: 0;
  padding: 0;
}

.profile-avatar-grid {
  display: grid;
  grid-template-columns: repeat(8, 48px);
  gap: 10px;
}

.profile-avatar-option {
  display: flex;
  width: 48px;
  height: 48px;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 2px solid #e2e8f0;
  border-radius: 999px;
  background: #ffffff;
  padding: 0;
  cursor: pointer;
  transition:
    border-color 0.18s,
    box-shadow 0.18s,
    transform 0.18s;
}

.profile-avatar-option:hover {
  border-color: #93c5fd;
  transform: translateY(-1px);
}

.profile-avatar-option.selected {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px #dbeafe;
}

.profile-avatar-option img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-form-actions {
  display: flex;
  gap: 12px;
}

.profile-save-button,
.profile-cancel-button {
  height: 40px;
  border-radius: 12px;
  padding: 0 18px;
  font-size: 14px;
  font-weight: 900;
  cursor: pointer;
}

.profile-save-button {
  border: 1px solid #2563eb;
  background: #2563eb;
  color: #ffffff;
  box-shadow: 0 8px 18px rgb(37 99 235 / 0.16);
}

.profile-cancel-button {
  border: 1px solid #e2e8f0;
  background: #ffffff;
  color: #64748b;
}

.profile-save-button:disabled,
.profile-cancel-button:disabled {
  cursor: not-allowed;
  opacity: 0.58;
}

.profile-info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin: 22px 0 0;
}

.profile-info-grid > div {
  min-width: 0;
  border: 1px solid #f1f5f9;
  border-radius: 12px;
  background: #f8fafc;
  padding: 14px;
}

.profile-info-grid dt {
  color: #94a3b8;
  font-size: 12px;
  font-weight: 900;
}

.profile-info-grid dd {
  margin: 8px 0 0;
  overflow-wrap: anywhere;
  color: #0f172a;
  font-size: 14px;
  font-weight: 900;
}

@media (max-width: 760px) {
  .profile-card {
    padding: 20px;
  }

  .profile-card__header,
  .profile-overview {
    align-items: flex-start;
    flex-direction: column;
  }

  .profile-info-grid {
    grid-template-columns: 1fr;
  }

  .profile-avatar-grid {
    grid-template-columns: repeat(4, 48px);
  }
}
</style>
