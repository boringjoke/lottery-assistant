<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { logout } from '@/api/auth'
import {
  fetchNotificationPage,
  fetchUnreadNotificationCount,
  markAllNotificationsAsRead,
  markNotificationAsRead,
} from '@/api/notifications'
import { fetchUserProfile } from '@/api/user'
import AppToast from '@/components/AppToast.vue'
import ProfileShell from '@/components/profile/ProfileShell.vue'
import type { CurrentUser } from '@/types/auth'
import type { UserNotification, UserNotificationPage } from '@/types/notification'
import type { UserProfile } from '@/types/user'
import { getErrorMessage } from '@/utils/lotteryFormat'

const router = useRouter()
const profile = ref<UserProfile | null>(null)
const notificationPage = ref<UserNotificationPage | null>(null)
const loadingProfile = ref(false)
const loadingNotifications = ref(false)
const authLoading = ref(false)
const operatingNotificationId = ref<number | null>(null)
const markingAll = ref(false)
const totalUnreadCount = ref(0)
const errorMessage = ref('')
const toastMessage = ref('')
const toastType = ref<'success' | 'error' | 'info'>('info')
const pageNo = ref(1)
const pageSize = 10

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
const notifications = computed(() => notificationPage.value?.notifications ?? [])
const totalPages = computed(() => Math.max(notificationPage.value?.pages ?? 0, 1))
const hasPreviousPage = computed(() => pageNo.value > 1)
const hasNextPage = computed(() => pageNo.value < totalPages.value)
const hasUnreadNotifications = computed(() => totalUnreadCount.value > 0)

function isUnauthorized(error: unknown): boolean {
  return getErrorMessage(error).includes('请先登录')
}

function formatTime(value: string | null | undefined): string {
  if (!value) {
    return '-'
  }

  return value.replace('T', ' ').slice(0, 16)
}

function readStatusText(notification: UserNotification): string {
  return notification.readStatus === 'READ' ? '已读' : '未读'
}

function showToast(message: string, type: 'success' | 'error' | 'info' = 'info') {
  toastType.value = type
  toastMessage.value = message
}

function closeToast() {
  toastMessage.value = ''
}

async function loadProfile() {
  loadingProfile.value = true
  errorMessage.value = ''

  try {
    profile.value = await fetchUserProfile()
  } catch (error) {
    if (isUnauthorized(error)) {
      await router.push({
        path: '/login',
        query: { redirect: '/profile/notifications' },
      })
      return
    }

    errorMessage.value = getErrorMessage(error)
    profile.value = null
  } finally {
    loadingProfile.value = false
  }
}

async function loadNotifications() {
  if (!profile.value) {
    return
  }

  loadingNotifications.value = true
  errorMessage.value = ''

  try {
    notificationPage.value = await fetchNotificationPage({
      pageNo: pageNo.value,
      pageSize,
    })
  } catch (error) {
    if (isUnauthorized(error)) {
      await router.push({
        path: '/login',
        query: { redirect: '/profile/notifications' },
      })
      return
    }

    errorMessage.value = getErrorMessage(error)
    notificationPage.value = null
  } finally {
    loadingNotifications.value = false
  }
}

async function loadUnreadCount() {
  if (!profile.value) {
    totalUnreadCount.value = 0
    return
  }

  try {
    totalUnreadCount.value = await fetchUnreadNotificationCount()
  } catch {
    totalUnreadCount.value = 0
  }
}

async function initializePage() {
  await loadProfile()
  await Promise.all([loadNotifications(), loadUnreadCount()])
}

async function refreshNotifications() {
  closeToast()
  await Promise.all([loadNotifications(), loadUnreadCount()])
}

async function changePage(nextPageNo: number) {
  pageNo.value = nextPageNo
  closeToast()
  await loadNotifications()
}

async function readNotification(notification: UserNotification) {
  if (notification.readStatus === 'READ') {
    return
  }

  operatingNotificationId.value = notification.id
  closeToast()

  try {
    await markNotificationAsRead(notification.id)
    showToast('通知已标记为已读', 'success')
    await Promise.all([loadNotifications(), loadUnreadCount()])
  } catch (error) {
    showToast(getErrorMessage(error), 'error')
  } finally {
    operatingNotificationId.value = null
  }
}

async function readAllNotifications() {
  markingAll.value = true
  closeToast()

  try {
    const count = await markAllNotificationsAsRead()
    showToast(count > 0 ? `已标记 ${count} 条通知` : '当前没有未读通知', 'success')
    await Promise.all([loadNotifications(), loadUnreadCount()])
  } catch (error) {
    showToast(getErrorMessage(error), 'error')
  } finally {
    markingAll.value = false
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

onMounted(initializePage)
</script>

<template>
  <ProfileShell
    :current-user="currentUser"
    :loading="authLoading || loadingProfile"
    :notification-unread-count="totalUnreadCount"
    active-nav="notifications"
    @logout="handleLogout"
  >
    <AppToast :message="toastMessage" :type="toastType" @close="closeToast" />
    <section class="notifications-panel">
      <div class="notifications-panel__header">
        <div>
          <h1>我的通知</h1>
          <p>查看收藏号码中奖提醒</p>
        </div>
        <div class="notifications-header-actions">
          <button
            class="notifications-secondary-button"
            type="button"
            :disabled="loadingNotifications"
            @click="refreshNotifications"
          >
            {{ loadingNotifications ? '加载中' : '刷新' }}
          </button>
          <button
            class="notifications-primary-button"
            type="button"
            :disabled="markingAll || !hasUnreadNotifications"
            @click="readAllNotifications"
          >
            {{ markingAll ? '处理中' : '全部已读' }}
          </button>
        </div>
      </div>

      <div v-if="errorMessage" class="notifications-state notifications-state--error">{{ errorMessage }}</div>
      <div v-if="loadingNotifications && !notificationPage" class="notifications-state">正在加载通知</div>
      <div v-else-if="!loadingNotifications && !notifications.length" class="notifications-empty">
        <strong>暂无通知</strong>
        <span>收藏号码中奖后，提醒会显示在这里。</span>
      </div>

      <div v-else class="notifications-list">
        <article
          v-for="notification in notifications"
          :key="notification.id"
          class="notification-card"
          :class="{ unread: notification.readStatus === 'UNREAD' }"
        >
          <div class="notification-card__status">
            <span>{{ readStatusText(notification) }}</span>
          </div>
          <div class="notification-card__body">
            <div class="notification-card__title-row">
              <h2>{{ notification.title }}</h2>
              <time>{{ formatTime(notification.createTime) }}</time>
            </div>
            <p>{{ notification.content }}</p>
            <small>{{ notification.businessKey }}</small>
          </div>
          <button
            v-if="notification.readStatus === 'UNREAD'"
            data-test="read-notification"
            type="button"
            :disabled="operatingNotificationId === notification.id"
            @click="readNotification(notification)"
          >
            标记已读
          </button>
        </article>
      </div>

      <div v-if="notificationPage && notificationPage.total > 0" class="notifications-pagination">
        <span>共 {{ notificationPage.total }} 条，第 {{ pageNo }} / {{ totalPages }} 页</span>
        <div>
          <button type="button" :disabled="!hasPreviousPage || loadingNotifications" @click="changePage(pageNo - 1)">
            上一页
          </button>
          <button type="button" :disabled="!hasNextPage || loadingNotifications" @click="changePage(pageNo + 1)">
            下一页
          </button>
        </div>
      </div>
    </section>
  </ProfileShell>
</template>

<style scoped>
.notifications-panel {
  max-width: 860px;
  border: 1px solid #dbeafe;
  border-radius: 16px;
  background: #ffffff;
  padding: 28px;
  box-shadow: 0 12px 36px rgb(15 23 42 / 0.06);
}

.notifications-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid #f1f5f9;
  padding-bottom: 22px;
}

.notifications-panel__header h1 {
  margin: 0;
  color: #111827;
  font-size: 22px;
  font-weight: 900;
}

.notifications-panel__header p {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 14px;
}

.notifications-header-actions,
.notifications-pagination div {
  display: flex;
  gap: 8px;
}

.notifications-primary-button,
.notifications-secondary-button,
.notifications-pagination button,
.notification-card button {
  height: 36px;
  border-radius: 10px;
  padding: 0 14px;
  font-size: 13px;
  font-weight: 900;
  white-space: nowrap;
  cursor: pointer;
}

.notifications-primary-button {
  border: 1px solid #2563eb;
  background: #2563eb;
  color: #ffffff;
  box-shadow: 0 8px 18px rgb(37 99 235 / 0.16);
}

.notifications-secondary-button,
.notifications-pagination button,
.notification-card button {
  border: 1px solid #dbeafe;
  background: #eff6ff;
  color: #1d4ed8;
}

.notifications-primary-button:disabled,
.notifications-secondary-button:disabled,
.notifications-pagination button:disabled,
.notification-card button:disabled {
  cursor: not-allowed;
  opacity: 0.58;
}

.notifications-state {
  margin-top: 18px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #f8fafc;
  color: #64748b;
  padding: 14px 16px;
  font-size: 14px;
  font-weight: 800;
}

.notifications-state--error {
  border-color: #fecaca;
  background: #fef2f2;
  color: #b91c1c;
}

.notifications-empty {
  display: grid;
  gap: 8px;
  margin-top: 18px;
  border: 1px dashed #cbd5e1;
  border-radius: 12px;
  background: #f8fafc;
  color: #64748b;
  padding: 42px 16px;
  text-align: center;
  font-size: 14px;
}

.notifications-empty strong {
  color: #0f172a;
  font-size: 16px;
}

.notifications-list {
  display: grid;
  gap: 12px;
  margin-top: 18px;
}

.notification-card {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr) auto;
  gap: 14px;
  align-items: start;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #ffffff;
  padding: 14px;
}

.notification-card.unread {
  border-color: #bfdbfe;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  box-shadow: 0 10px 26px rgb(37 99 235 / 0.1);
}

.notification-card__status span {
  display: inline-flex;
  height: 26px;
  align-items: center;
  border-radius: 999px;
  background: #f1f5f9;
  color: #64748b;
  padding: 0 10px;
  font-size: 12px;
  font-weight: 900;
}

.notification-card.unread .notification-card__status span {
  background: #dbeafe;
  color: #1d4ed8;
}

.notification-card__body {
  min-width: 0;
}

.notification-card__title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.notification-card h2 {
  margin: 0;
  overflow-wrap: anywhere;
  color: #0f172a;
  font-size: 15px;
  font-weight: 900;
}

.notification-card time {
  flex: 0 0 auto;
  color: #94a3b8;
  font-size: 12px;
  font-weight: 800;
}

.notification-card p {
  margin: 8px 0 0;
  overflow-wrap: anywhere;
  color: #475569;
  font-size: 14px;
  font-weight: 800;
  line-height: 1.65;
}

.notification-card small {
  display: block;
  margin-top: 8px;
  overflow-wrap: anywhere;
  color: #94a3b8;
  font-size: 12px;
  font-weight: 800;
}

.notifications-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 18px;
  color: #64748b;
  font-size: 13px;
  font-weight: 800;
}

@media (max-width: 760px) {
  .notifications-panel {
    padding: 20px;
  }

  .notifications-panel__header,
  .notifications-pagination,
  .notification-card__title-row {
    align-items: stretch;
    flex-direction: column;
  }

  .notifications-header-actions {
    flex-wrap: wrap;
  }

  .notification-card {
    grid-template-columns: 1fr;
  }
}
</style>
