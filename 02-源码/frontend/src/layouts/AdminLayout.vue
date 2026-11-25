<template>
  <div class="app">
    <aside class="side">
      <div class="brand"><span class="dot"></span>社团活动与经费</div>
      <div class="menu-wrap">
        <el-menu :default-active="active" router unique-opened :collapse="false">
          <template v-for="m in menus" :key="m.id">
            <el-sub-menu v-if="m.children && m.children.length && m.menuType !== 'MENU'"
                         :index="String(m.id)">
              <template #title><span>{{ m.name }}</span></template>
              <el-menu-item v-for="c in m.children" :key="c.id" :index="c.path">
                {{ c.name }}
              </el-menu-item>
            </el-sub-menu>
            <el-menu-item v-else :index="m.path">{{ m.name }}</el-menu-item>
          </template>
        </el-menu>
      </div>
    </aside>

    <div class="main">
      <header class="topbar">
        <div class="crumb">
          学生社团活动与经费管理系统 <span style="color:var(--text-3)">/</span> <b>{{ title }}</b>
        </div>
        <div class="right">
          <el-badge :value="unread" :hidden="unread === 0" :max="99">
            <el-button text @click="$router.push('/message')">
              <el-icon><Bell /></el-icon>
            </el-button>
          </el-badge>
          <el-dropdown trigger="click" @command="onCmd">
            <div class="user" style="display:flex;align-items:center;gap:8px;cursor:pointer">
              <img v-if="avatar" :src="avatar" class="avatar-sm" alt="" />
              <div v-else class="avatar-sm" style="background:var(--primary-soft)"></div>
              <div style="line-height:1.25">
                <div style="font-size:13px;font-weight:600">{{ realName }}</div>
                <div style="font-size:11px;color:var(--text-3)">{{ roleName }}</div>
              </div>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>
      <main class="content"><router-view /></main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http from '../api/http'
import { store, clearLogin } from '../store/user'

const route = useRoute()
const router = useRouter()
const unread = ref(0)

const title = computed(() => route.meta.title || '')
const realName = computed(() => store.realName)
const roleName = computed(() => store.roleName)
const avatar = computed(() => store.avatar)

// 菜单按角色动态获取（规范第 8 条）：登录响应已带 menus，这里以接口为准做一次刷新
const menus = ref(store.menus.length ? store.menus : [])

function flatten(list, out) {
  list.forEach((m) => {
    if (m.menuType === 'MENU' && m.path) out.push(m.path)
    if (m.children) flatten(m.children, out)
  })
  return out
}

const active = computed(() => route.path)

onMounted(async () => {
  try {
    const list = await http.get('/auth/menus')
    if (Array.isArray(list) && list.length) {
      menus.value = list
      localStorage.setItem('cfs_menus', JSON.stringify(list))
    }
  } catch (e) { /* 菜单拉取失败时沿用登录返回的缓存 */ }
  try {
    const p = await http.get('/system/message/page', { params: { page: 1, size: 1, status: 0 } })
    unread.value = p.total || 0
  } catch (e) { /* 忽略角标异常 */ }
})

async function onCmd(cmd) {
  if (cmd === 'profile') return router.push('/profile')
  try { await http.post('/auth/logout') } catch (e) { /* 忽略 */ }
  clearLogin()
  router.push('/login')
}
</script>
