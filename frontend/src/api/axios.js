import axios from 'axios'
import { useAuthStore } from '../store/authStore'

const api = axios.create({
  // In production: VITE_API_BASE_URL = https://your-backend.railway.app
  // In local dev:  Vite proxy handles /api → localhost:8081
  baseURL: import.meta.env.VITE_API_BASE_URL
    ? `${import.meta.env.VITE_API_BASE_URL}/api/v1`
    : '/api/v1',
  headers: { 'Content-Type': 'application/json' },
})

// Attach JWT to every protected request
api.interceptors.request.use(config => {
  const token = useAuthStore.getState().token
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Auto-logout on 401
api.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      useAuthStore.getState().logout()
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default api
