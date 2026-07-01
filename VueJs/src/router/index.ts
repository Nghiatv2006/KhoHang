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
    component: () => import('../views/AuthView.vue'),
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
        path: '/backup-restore',
        name: 'BackupRestore',
        component: () => import('../views/BackupRestoreView.vue'),
      },
      {
        path: '/branches',
        name: 'Branches',
        component: () => import('../views/BranchesView.vue'),
      },
      {
        path: '/inventory',
        name: 'Inventory',
        component: () => import('../views/InventoryView.vue'),
      },
      {
        path: '/stocktakes',
        name: 'Stocktakes',
        component: () => import('../views/StocktakesView.vue'),
      },
      {
        path: '/profile',
        name: 'Profile',
        component: () => import('../views/ProfileView.vue'),
      },
      {
        path: '/receipts',
        redirect: '/imports',
      },
      {
        path: '/imports',
        name: 'Imports',
        component: () => import('../views/ReceiptsView.vue'),
        props: { receiptType: 'IMPORT' },
      },
      {
        path: '/invoices',
        name: 'Invoices',
        component: () => import('../views/ReceiptsView.vue'),
        props: { receiptType: 'EXPORT' },
      },
      {
        path: '/transfers',
        name: 'Transfers',
        component: () => import('../views/ReceiptsView.vue'),
        props: { receiptType: 'TRANSFER' },
      },
      {
        path: '/disposals',
        name: 'disposals',
        component: () => import('../views/ReceiptsView.vue'),
        props: { receiptType: 'ADJUST_OUT' },
      },
      {
        path: '/audit-logs',
        name: 'AuditLogs',
        component: () => import('../views/AuditLogView.vue'),
        meta: { roles: ['ADMIN', 'MANAGER'] },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.onError((error, to) => {
  if (error.message.includes('Failed to fetch dynamically imported module') || error.message.includes('Importing a module script failed')) {
    window.location.href = to.fullPath
  }
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
  } else if (to.path === '/users') {
    // Chỉ ADMIN và MANAGER mới được vào Users
    const canView = user && ['ADMIN', 'MANAGER'].includes(user.role)
    if (!canView) {
      next('/dashboard')
    } else {
      next()
    }
  } else if (to.path === '/backup-restore') {
    // ADMIN và MANAGER đều được vào Backup & Restore
    const canView = user && ['ADMIN', 'MANAGER'].includes(user.role)
    if (!canView) {
      next('/dashboard')
    } else {
      next()
    }
  } else if (to.path === '/audit-logs') {
    // Chỉ ADMIN và MANAGER mới được xem Nhật ký
    const canView = user && ['ADMIN', 'MANAGER'].includes(user.role)
    if (!canView) {
      next('/dashboard')
    } else {
      next()
    }
  } else {
    next()
  }
})

export default router
