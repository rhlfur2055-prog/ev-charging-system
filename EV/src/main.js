import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import axios from 'axios'

// Global axios interceptor: attaches Bearer token (when present) on every API
// call. Existing components keep working — they don't have to change. The
// vite.config.js plugin already rewrites http://localhost:8080 -> VITE_API_URL
// at build time, so URLs in views remain untouched.
axios.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) {
    config.headers = config.headers || {}
    if (!config.headers.Authorization) {
      config.headers.Authorization = `Bearer ${token}`
    }
  }
  return config
})

// 401 → drop stale token and bounce to /login. Don't break the response
// pipeline for components that already handle errors themselves.
axios.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err?.response?.status === 401) {
      localStorage.removeItem('accessToken')
    }
    return Promise.reject(err)
  }
)

createApp(App)
  .use(router)
  .mount('#app')
