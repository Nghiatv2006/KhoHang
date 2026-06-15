<script setup lang="ts">
import { ref } from 'vue'
import Login from './components/Login.vue'
import Dashboard from './components/Dashboard.vue'

const isLoggedIn = ref(false)
const userRole = ref('')

const handleLoginSuccess = (role: string) => {
  isLoggedIn.value = true
  userRole.value = role
}

const handleLogout = () => {
  isLoggedIn.value = false
  userRole.value = ''
}
</script>

<template>
  <Login v-if="!isLoggedIn" @login-success="handleLoginSuccess" />
  <div v-else class="min-h-screen bg-background">
    <Dashboard @logout="handleLogout" />
  </div>
</template>
