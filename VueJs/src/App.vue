<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import Login from './components/Login.vue'
import Dashboard from './components/Dashboard.vue'
import { api } from './api'

const savedUser = localStorage.getItem('user_profile')
const isLoggedIn = ref(!!savedUser)
const userRole = ref(savedUser ? JSON.parse(savedUser).role : '')

const verifySession = async () => {
  if (!isLoggedIn.value) return;
  try {
    const res = await api.get('/api/users/me');
    if (!res.ok) {
      handleLogout();
    }
  } catch (e) {
    handleLogout();
  }
}

onMounted(() => {
  verifySession();
  window.addEventListener('auth-failed', handleLogout);
})

onUnmounted(() => {
  window.removeEventListener('auth-failed', handleLogout);
})

const handleLoginSuccess = (role: string) => {
  isLoggedIn.value = true
  userRole.value = role
}

const handleLogout = async () => {
  isLoggedIn.value = false
  userRole.value = ''
  localStorage.removeItem('user_profile')
  // Gọi API backend để xoá HttpOnly Cookie
  try {
    await fetch('/api/auth/logout', { method: 'POST' })
  } catch (err) {}
}
</script>

<template>
  <Login v-if="!isLoggedIn" @login-success="handleLoginSuccess" />
  <div v-else class="min-h-screen bg-background">
    <Dashboard @logout="handleLogout" />
  </div>
</template>
