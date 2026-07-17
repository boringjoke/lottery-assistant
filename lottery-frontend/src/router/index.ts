import { createRouter, createWebHistory } from 'vue-router'
import AdminLotterySyncView from '@/views/AdminLotterySyncView.vue'
import LoginView from '@/views/LoginView.vue'
import LotteryAssistantView from '@/views/LotteryAssistantView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/lottery-assistant?tab=overview',
    },
    {
      path: '/lottery-assistant',
      name: 'lottery-assistant',
      component: LotteryAssistantView,
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView,
    },
    {
      path: '/admin/lottery-sync',
      name: 'admin-lottery-sync',
      component: AdminLotterySyncView,
    },
  ],
})

export default router
