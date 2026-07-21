<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { fetchUnreadNotificationCount } from '@/api/notifications'
import loginHoverIconUrl from '@/assets/icons/login-hover.svg'
import loginNotHoverIconUrl from '@/assets/icons/login-not-hover.svg'
import logoutIconUrl from '@/assets/icons/logout.svg'
import manageIconUrl from '@/assets/icons/manage.svg'
import notificationIconUrl from '@/assets/icons/notification.svg'
import profileIconUrl from '@/assets/icons/profile.svg'
import favoriteIconUrl from '@/assets/icons/favorite.svg'
import type { CurrentUser } from '@/types/auth'

const props = defineProps<{
  user: CurrentUser | null
  loading?: boolean
  notificationUnreadCount?: number | null
}>()

const emit = defineEmits<{
  logout: []
}>()

const route = useRoute()
const router = useRouter()
const menuOpen = ref(false)
const unreadCount = ref(0)

const isAdmin = computed(() => props.user?.roles.includes('ADMIN') ?? false)
const userInitial = computed(() => props.user?.nickname?.trim().slice(0, 1) || '用')
const unreadCountText = computed(() => unreadCount.value > 99 ? '99+' : String(unreadCount.value))

watch(
  () => props.user,
  (user) => {
    menuOpen.value = false
    if (!user) {
      unreadCount.value = 0
      return
    }

    if (hasUnreadCountOverride()) {
      applyUnreadCountOverride()
      return
    }

    void loadUnreadCount()
  },
  { immediate: true },
)

watch(
  () => props.notificationUnreadCount,
  () => {
    if (hasUnreadCountOverride()) {
      applyUnreadCountOverride()
    }
  },
)

function hasUnreadCountOverride(): boolean {
  return props.notificationUnreadCount !== null && props.notificationUnreadCount !== undefined
}

function applyUnreadCountOverride() {
  unreadCount.value = Math.max(props.notificationUnreadCount ?? 0, 0)
}

async function loadUnreadCount() {
  try {
    unreadCount.value = await fetchUnreadNotificationCount()
  } catch {
    unreadCount.value = 0
  }
}

function goLogin() {
  void router.push({
    path: '/login',
    query: { redirect: route.fullPath || '/lottery-assistant?tab=overview' },
  })
}

function goProfile() {
  menuOpen.value = false
  void router.push('/profile')
}

function goFavorites() {
  menuOpen.value = false
  void router.push('/profile/favorites')
}

function goNotifications() {
  menuOpen.value = false
  void router.push('/profile/notifications')
}

function goAdminSync() {
  menuOpen.value = false
  void router.push('/admin/lottery-sync')
}

function handleLogout() {
  menuOpen.value = false
  emit('logout')
}
</script>

<template>
  <div class="user-account-menu">
    <button
      v-if="!user"
      class="login-entry-button"
      type="button"
      :disabled="loading"
      @click="goLogin"
    >
      <span class="login-entry-button__icon" aria-hidden="true">
        <img class="login-entry-button__icon-default" :src="loginNotHoverIconUrl" alt="" />
        <img class="login-entry-button__icon-hover" :src="loginHoverIconUrl" alt="" />
      </span>
      <span>登录</span>
    </button>

    <template v-else>
      <button
        class="account-trigger"
        type="button"
        :disabled="loading"
        :aria-expanded="menuOpen"
        aria-haspopup="menu"
        @click="menuOpen = !menuOpen"
      >
        <span class="account-trigger__avatar" aria-hidden="true">
          <img v-if="user.avatarUrl" :src="user.avatarUrl" alt="" />
          <span v-else>{{ userInitial }}</span>
        </span>
        <span class="account-trigger__name">{{ user.nickname }}</span>
        <span v-if="unreadCount > 0" class="account-trigger__badge" aria-label="未读通知">
          {{ unreadCountText }}
        </span>
        <span class="account-trigger__arrow" :class="{ rotated: menuOpen }" aria-hidden="true"></span>
      </button>

      <div v-if="menuOpen" class="account-menu-mask" @click="menuOpen = false"></div>
      <div v-if="menuOpen" class="account-menu" role="menu">
        <button type="button" role="menuitem" @click="goProfile">
          <img class="account-menu__icon" :src="profileIconUrl" alt="" aria-hidden="true" />
          <span>个人中心</span>
        </button>
        <button type="button" role="menuitem" @click="goFavorites">
          <img class="account-menu__icon" :src="favoriteIconUrl" alt="" aria-hidden="true" />
          <span>我的收藏</span>
        </button>
        <button type="button" role="menuitem" @click="goNotifications">
          <img class="account-menu__icon" :src="notificationIconUrl" alt="" aria-hidden="true" />
          <span>我的通知</span>
          <span v-if="unreadCount > 0" class="account-menu__badge">{{ unreadCountText }}</span>
        </button>
        <button v-if="isAdmin" type="button" role="menuitem" @click="goAdminSync">
          <img class="account-menu__icon" :src="manageIconUrl" alt="" aria-hidden="true" />
          <span>数据同步管理</span>
        </button>
        <button class="account-menu__logout" type="button" role="menuitem" :disabled="loading" @click="handleLogout">
          <img class="account-menu__icon" :src="logoutIconUrl" alt="" aria-hidden="true" />
          <span>退出登录</span>
        </button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.user-account-menu {
  position: relative;
  z-index: 20;
}

.login-entry-button {
  display: inline-flex;
  height: 34px;
  align-items: center;
  gap: 8px;
  border: 1px solid #bfdbfe;
  border-radius: 10px;
  background: #eff6ff;
  color: #1d4ed8;
  padding: 0 13px 0 10px;
  font-size: 14px;
  font-weight: 900;
  cursor: pointer;
  box-shadow: 0 4px 12px rgb(37 99 235 / 0.08);
  transition:
    background 0.18s,
    border-color 0.18s,
    box-shadow 0.18s,
    color 0.18s,
    transform 0.18s;
}

.login-entry-button:hover:not(:disabled) {
  border-color: #2563eb;
  background: #2563eb;
  color: #ffffff;
  box-shadow: 0 10px 24px rgb(37 99 235 / 0.2);
  transform: translateY(-1px);
}

.login-entry-button:disabled,
.account-trigger:disabled,
.account-menu button:disabled {
  cursor: not-allowed;
  opacity: 0.62;
}

.login-entry-button:focus-visible,
.account-trigger:focus-visible,
.account-menu button:focus-visible {
  outline: 3px solid #bfdbfe;
  outline-offset: 2px;
}

.login-entry-button__icon {
  position: relative;
  display: inline-flex;
  width: 18px;
  height: 18px;
  flex: 0 0 18px;
}

.login-entry-button__icon img {
  position: absolute;
  inset: 0;
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
  transition: opacity 0.18s;
}

.login-entry-button__icon-hover {
  opacity: 0;
}

.login-entry-button:hover:not(:disabled) .login-entry-button__icon-default {
  opacity: 0;
}

.login-entry-button:hover:not(:disabled) .login-entry-button__icon-hover {
  opacity: 1;
}

.account-trigger {
  display: inline-flex;
  height: 36px;
  align-items: center;
  gap: 8px;
  border: 1px solid #dbeafe;
  border-radius: 999px;
  background: #ffffff;
  color: #0f172a;
  padding: 3px 8px 3px 4px;
  box-shadow: 0 6px 18px rgb(15 23 42 / 0.06);
  cursor: pointer;
}

.account-trigger__avatar {
  display: inline-flex;
  width: 28px;
  height: 28px;
  flex: 0 0 28px;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 50%;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 13px;
  font-weight: 900;
}

.account-trigger__avatar img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.account-trigger__name {
  max-width: 120px;
  overflow: hidden;
  font-size: 14px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.account-trigger__badge {
  display: inline-flex;
  min-width: 18px;
  height: 18px;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: #dc2626;
  color: #ffffff;
  padding: 0 5px;
  font-size: 11px;
  font-weight: 900;
}

.account-trigger__arrow {
  position: relative;
  width: 18px;
  height: 18px;
  flex: 0 0 18px;
  border-radius: 6px;
  background: #f1f5f9;
  transition: transform 0.2s;
}

.account-trigger__arrow::before {
  position: absolute;
  top: 5px;
  left: 6px;
  width: 5px;
  height: 5px;
  border-right: 2px solid #94a3b8;
  border-bottom: 2px solid #94a3b8;
  content: "";
  transform: rotate(45deg);
}

.account-trigger__arrow.rotated {
  transform: rotate(180deg);
}

.account-menu-mask {
  position: fixed;
  inset: 0;
  z-index: 30;
}

.account-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  z-index: 40;
  width: 184px;
  overflow: hidden;
  border: 1px solid #f1f5f9;
  border-radius: 14px;
  background: #ffffff;
  box-shadow: 0 16px 40px rgb(15 23 42 / 0.12);
}

.account-menu button {
  display: flex;
  width: 100%;
  height: 40px;
  align-items: center;
  gap: 10px;
  border: 0;
  background: #ffffff;
  color: #334155;
  padding: 0 12px;
  font-size: 14px;
  font-weight: 800;
  text-align: left;
  cursor: pointer;
}

.account-menu button span:nth-child(2) {
  flex: 1;
}

.account-menu__badge {
  display: inline-flex;
  min-width: 20px;
  height: 20px;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: #dc2626;
  color: #ffffff;
  padding: 0 6px;
  font-size: 11px;
  font-weight: 900;
}

.account-menu button:hover:not(:disabled) {
  background: #eff6ff;
  color: #1d4ed8;
}

.account-menu button:last-child:hover:not(:disabled) {
  background: #fee2e2;
  color: #b91c1c;
}

.account-menu__logout {
  border-top: 1px solid #fee2e2 !important;
  color: #dc2626 !important;
}

.account-menu__icon {
  display: block;
  width: 16px;
  height: 16px;
  flex: 0 0 16px;
  object-fit: contain;
}
</style>
