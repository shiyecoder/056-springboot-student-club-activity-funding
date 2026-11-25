<template>
  <div class="portal">
    <header class="p-head">
      <div class="p-brand">
        <span class="dot"></span>
        <span>学生社团活动与经费管理系统</span>
        <em>学生门户</em>
      </div>
      <nav class="p-nav">
        <router-link v-for="n in navs" :key="n.path" :to="n.path" class="p-link"
                     :class="{ on: $route.path === n.path }">{{ n.name }}</router-link>
      </nav>
      <div class="p-user">
        <el-badge :value="unread" :hidden="unread === 0" :max="99">
          <el-button text @click="$router.push('/portal/profile')">
            <el-icon><Bell /></el-icon>
          </el-button>
        </el-badge>
        <img v-if="avatar" :src="avatar" class="avatar-sm" alt="" />
        <span>{{ realName }}</span>
        <el-button link type="danger" @click="logout">退出</el-button>
      </div>
    </header>
    <main class="p-body"><router-view /></main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '../api/http'
import { store, clearLogin } from '../store/user'

const router = useRouter()
const unread = ref(0)
const navs = [
  { name: '首页', path: '/portal/home' },
  { name: '我的社团', path: '/portal/club' },
  { name: '活动广场', path: '/portal/activity' },
  { name: '我的报名', path: '/portal/signup' },
  { name: '经费公开', path: '/portal/fund' },
  { name: '个人中心', path: '/portal/profile' }
]
const realName = computed(() => store.realName)
const avatar = computed(() => store.avatar)

onMounted(async () => {
  try {
    const p = await http.get('/system/message/page', { params: { page: 1, size: 1, status: 0 } })
    unread.value = p.total || 0
  } catch (e) { /* 忽略 */ }
})

async function logout() {
  try { await http.post('/auth/logout') } catch (e) { /* 忽略 */ }
  clearLogin()
  router.push('/login')
}
</script>

<style scoped>
.portal { min-height: 100vh; background: var(--bg); }
.p-head {
  height: 64px; background: #fff; border-bottom: 1px solid var(--line);
  display: flex; align-items: center; gap: 24px; padding: 0 32px;
  position: sticky; top: 0; z-index: 10;
}
.p-brand { display: flex; align-items: center; gap: 8px; font-size: 14px; font-weight: 600; }
.p-brand .dot { width: 8px; height: 8px; border-radius: 50%; background: var(--primary); }
.p-brand em { font-style: normal; font-size: 11px; font-weight: 400; color: var(--primary); background: var(--primary-soft); padding: 2px 7px; border-radius: 10px; }
.p-nav { display: flex; gap: 4px; flex: 1; }
.p-link { padding: 7px 14px; border-radius: 6px; color: var(--text-2); text-decoration: none; font-size: 13px; }
.p-link:hover { background: var(--bg); color: var(--text); }
.p-link.on { background: var(--primary-soft); color: var(--primary); font-weight: 600; }
.p-user { display: flex; align-items: center; gap: 10px; font-size: 12.5px; color: var(--text-2); }
.p-body { padding: 16px 32px 28px; max-width: 1440px; margin: 0 auto; }
</style>
