import { createRouter, createWebHistory } from 'vue-router'
import HomePage from '@/pages/HomePage.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/home',
    },
    {
      path: '/home',
      name: 'home',
      component: HomePage,
    },
    {
      path: '/about',
      name: 'about',
      component: () => import('@/pages/AboutPage.vue'),
    },
  ],
})

export default router
