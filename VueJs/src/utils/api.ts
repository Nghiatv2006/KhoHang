import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Add response interceptor to handle session expiration (e.g. 401 Unauthorized)
api.interceptors.response.use(
  response => response,
  error => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('wh_token')
      localStorage.removeItem('wh_user')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default api
