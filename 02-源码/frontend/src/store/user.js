/**
 * 轻量登录态。reactive 只作为页面响应式快照，
 * 真正的判断依据是 localStorage（路由守卫也读 localStorage，避免刷新后被弹回登录页）。
 */
import { reactive } from 'vue'

export const store = reactive({
  token: localStorage.getItem('cfs_token') || '',
  userId: Number(localStorage.getItem('cfs_userId') || 0),
  username: localStorage.getItem('cfs_username') || '',
  realName: localStorage.getItem('cfs_realName') || '',
  avatar: localStorage.getItem('cfs_avatar') || '',
  roleCode: localStorage.getItem('cfs_roleCode') || '',
  roleName: localStorage.getItem('cfs_roleName') || '',
  deptName: localStorage.getItem('cfs_deptName') || '',
  menus: JSON.parse(localStorage.getItem('cfs_menus') || '[]')
})

export function setLogin(data) {
  store.token = data.token || ''
  store.userId = data.userId || 0
  store.username = data.username || ''
  store.realName = data.realName || ''
  store.avatar = data.avatar || ''
  store.roleCode = data.roleCode || ''
  store.roleName = data.roleName || ''
  store.deptName = data.deptName || ''
  store.menus = data.menus || []
  localStorage.setItem('cfs_token', store.token)
  localStorage.setItem('cfs_userId', store.userId)
  localStorage.setItem('cfs_username', store.username)
  localStorage.setItem('cfs_realName', store.realName)
  localStorage.setItem('cfs_avatar', store.avatar)
  localStorage.setItem('cfs_roleCode', store.roleCode)
  localStorage.setItem('cfs_roleName', store.roleName)
  localStorage.setItem('cfs_deptName', store.deptName)
  localStorage.setItem('cfs_menus', JSON.stringify(store.menus))
}

export function setAvatar(url) {
  store.avatar = url
  localStorage.setItem('cfs_avatar', url)
}

export function clearLogin() {
  ;['token', 'userId', 'username', 'realName', 'avatar', 'roleCode', 'roleName', 'deptName', 'menus']
    .forEach((k) => localStorage.removeItem('cfs_' + k))
  store.token = ''
  store.menus = []
}

export function isLogin() {
  return !!localStorage.getItem('cfs_token')
}

export function isPortal() {
  return (localStorage.getItem('cfs_roleCode') || '') === 'MEMBER'
}

export default store
