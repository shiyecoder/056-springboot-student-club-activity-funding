import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const http = axios.create({ baseURL: '/api', timeout: 20000 })

http.interceptors.request.use((cfg) => {
  const token = localStorage.getItem('cfs_token')
  if (token) cfg.headers.Authorization = 'Bearer ' + token
  return cfg
})

http.interceptors.response.use(
  (res) => {
    const body = res.data
    // 后端统一响应：code=200 为成功
    if (body && typeof body.code !== 'undefined') {
      if (body.code === 200) return body.data
      if (body.code === 401) {
        localStorage.removeItem('cfs_token')
        router.push('/login')
      }
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body
  },
  (err) => {
    const st = err.response && err.response.status
    if (st === 401 || st === 403) {
      ElMessage.error(st === 401 ? '登录已过期，请重新登录' : '没有该操作的权限')
      if (st === 401) {
        localStorage.removeItem('cfs_token')
        router.push('/login')
      }
    } else {
      ElMessage.error('网络或服务异常：' + (err.message || ''))
    }
    return Promise.reject(err)
  }
)

export default http
