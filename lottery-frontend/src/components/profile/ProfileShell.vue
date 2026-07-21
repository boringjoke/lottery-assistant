<script setup lang="ts">
import { RouterLink } from 'vue-router'

import UserAccountMenu from '@/components/UserAccountMenu.vue'
import type { CurrentUser } from '@/types/auth'

type ProfileNavKey = 'profile' | 'favorites' | 'notifications'

defineProps<{
  currentUser: CurrentUser | null
  loading?: boolean
  activeNav: ProfileNavKey
  notificationUnreadCount?: number | null
}>()

const emit = defineEmits<{
  logout: []
}>()

const navItems: Array<{ key: ProfileNavKey, label: string, to: string }> = [
  { key: 'profile', label: '个人资料', to: '/profile' },
  { key: 'favorites', label: '我的收藏', to: '/profile/favorites' },
  { key: 'notifications', label: '我的通知', to: '/profile/notifications' },
]

function handleLogout() {
  emit('logout')
}
</script>

<template>
  <div class="profile-shell">
    <header class="profile-shell-topbar">
      <RouterLink class="profile-shell-brand" to="/lottery-assistant?tab=overview">
        <span class="profile-shell-brand__mark">≋</span>
        <span>彩票助手</span>
      </RouterLink>
      <UserAccountMenu
        :user="currentUser"
        :loading="loading"
        :notification-unread-count="notificationUnreadCount"
        @logout="handleLogout"
      />
    </header>

    <div class="profile-shell-layout">
      <aside class="profile-shell-sidebar">
        <div class="profile-shell-sidebar__title">个人中心</div>
        <nav class="profile-shell-nav" aria-label="个人中心导航">
          <RouterLink
            v-for="item in navItems"
            :key="item.key"
            class="profile-shell-nav__item"
            :class="{ active: activeNav === item.key }"
            :to="item.to"
          >
            <span class="profile-shell-nav__dot" aria-hidden="true"></span>
            <span>{{ item.label }}</span>
          </RouterLink>
        </nav>
      </aside>

      <main class="profile-shell-main">
        <slot />
      </main>
    </div>
  </div>
</template>

<style scoped>
.profile-shell {
  min-height: 100vh;
  background: #f8fafc;
  color: #0f172a;
}

.profile-shell-topbar {
  display: flex;
  height: 64px;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e2e8f0;
  background: #ffffff;
  padding: 0 24px;
}

.profile-shell-brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: #1e3a8a;
  font-size: 20px;
  font-weight: 900;
  text-decoration: none;
}

.profile-shell-brand__mark {
  display: inline-flex;
  width: 32px;
  height: 32px;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: linear-gradient(135deg, #3b82f6, #1d4ed8);
  color: #ffffff;
}

.profile-shell-layout {
  display: flex;
  min-height: calc(100vh - 64px);
}

.profile-shell-sidebar {
  width: 220px;
  flex: 0 0 220px;
  border-right: 1px solid #e2e8f0;
  background: #ffffff;
  padding: 24px 12px;
}

.profile-shell-sidebar__title {
  margin: 0 12px 16px;
  color: #94a3b8;
  font-size: 12px;
  font-weight: 900;
}

.profile-shell-nav {
  display: grid;
  gap: 6px;
}

.profile-shell-nav__item {
  display: flex;
  height: 42px;
  align-items: center;
  gap: 10px;
  border-radius: 12px;
  color: #64748b;
  padding: 0 12px;
  font-size: 14px;
  font-weight: 800;
  text-decoration: none;
}

.profile-shell-nav__item.active {
  background: #eff6ff;
  color: #2563eb;
}

.profile-shell-nav__dot {
  width: 8px;
  height: 8px;
  flex: 0 0 8px;
  border-radius: 999px;
  background: currentColor;
}

.profile-shell-main {
  flex: 1;
  min-width: 0;
  padding: 28px;
}

@media (max-width: 860px) {
  .profile-shell-topbar {
    height: auto;
    align-items: flex-start;
    flex-direction: column;
    gap: 14px;
    padding: 16px;
  }

  .profile-shell-layout {
    display: block;
  }

  .profile-shell-sidebar {
    width: auto;
    border-right: 0;
    border-bottom: 1px solid #e2e8f0;
  }

  .profile-shell-main {
    padding: 16px;
  }
}
</style>
