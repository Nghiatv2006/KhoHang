import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/login',
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/LoginView.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/privacy-policy',
    name: 'PrivacyPolicy',
    component: () => import('../views/PrivacyPolicyView.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/dashboard',
    component: () => import('../layouts/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'Dashboard',
        component: () => import('../views/DashboardView.vue'),
      },
      {
        path: '/products',
        name: 'Products',
        component: () => import('../views/ProductsView.vue'),
      },
      {
        path: '/global-inventory',
        name: 'GlobalInventory',
        component: () => import('../views/GlobalInventoryView.vue'),
      },
      {
        path: '/partners',
        name: 'Partners',
        component: () => import('../views/PartnersView.vue'),
      },
      {
        path: '/users',
        name: 'Users',
        component: () => import('../views/UsersView.vue'),
      },
      {
        path: '/branches',
        name: 'Branches',
        component: () => import('../views/BranchesView.vue'),
      },
      {
        path: '/profile',
        name: 'Profile',
        component: () => import('../views/ProfileView.vue'),
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// Navigation guard
router.beforeEach((to, _from, next) => {
  const userStr = localStorage.getItem('wh_user')
  const user = userStr ? JSON.parse(userStr) : null
  const isLoggedIn = !!user
  
  if (to.meta.requiresAuth && !isLoggedIn) {
    next('/login')
  } else if (to.path === '/login' && isLoggedIn) {
    next('/dashboard')
  } else if (to.path === '/products') {
    // Chỉ ADMIN mới được vào Products
    const hasCrud = user && user.role === 'ADMIN'
    if (!hasCrud) {
      next('/global-inventory')
    } else {
      next()
    }
  } else {
    next()
  }
})

export default router
