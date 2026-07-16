import { createRouter, createWebHistory } from 'vue-router'
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
  ],
})

export default router
