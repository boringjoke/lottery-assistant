import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { fetchCurrentUser } from '@/api/auth'
import type { CurrentUser } from '@/types/auth'

export function useAdminSyncAuth(loadDashboard: () => Promise<void>) {
  const route = useRoute()
  const router = useRouter()
  const authChecking = ref(true)
  const permissionDenied = ref(false)
  const currentUser = ref<CurrentUser | null>(null)

  /**
   * 管理页进入时先恢复登录态；只有 ADMIN 角色才加载同步管理数据。
   */
  async function initializeAdminPage() {
    authChecking.value = true
    permissionDenied.value = false

    try {
      currentUser.value = await fetchCurrentUser()
      if (!currentUser.value.roles.includes('ADMIN')) {
        permissionDenied.value = true
        return
      }

      await loadDashboard()
    } catch {
      await router.push({
        path: '/login',
        query: { redirect: route.fullPath },
      })
    } finally {
      authChecking.value = false
    }
  }

  return {
    authChecking,
    permissionDenied,
    currentUser,
    initializeAdminPage,
  }
}
