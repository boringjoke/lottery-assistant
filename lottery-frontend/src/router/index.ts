import { createRouter, createWebHistory } from 'vue-router'
import AdminLotterySyncView from '@/views/AdminLotterySyncView.vue'
import FavoritesView from '@/views/FavoritesView.vue'
import LoginView from '@/views/LoginView.vue'
import LotteryAssistantView from '@/views/LotteryAssistantView.vue'
import NotificationsView from '@/views/NotificationsView.vue'
import ProfileView from '@/views/ProfileView.vue'

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
      path: '/profile',
      name: 'profile',
      component: ProfileView,
    },
    {
      path: '/profile/favorites',
      name: 'profile-favorites',
      component: FavoritesView,
    },
    {
      path: '/profile/notifications',
      name: 'profile-notifications',
      component: NotificationsView,
    },
    {
      path: '/admin/lottery-sync',
      name: 'admin-lottery-sync',
      component: AdminLotterySyncView,
    },
  ],
})

export default router
