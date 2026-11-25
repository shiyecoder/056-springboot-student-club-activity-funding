<template>
  <div class="lg">
    <div class="lg-bg"></div>
    <div class="lg-shade"></div>

    <div class="lg-left">
      <div class="lg-brand"><span class="dot"></span>学生社团活动与经费管理系统</div>
      <h1>让每一次社团活动<br />都有据可依、有账可查</h1>
      <p class="lead">
        覆盖社团注册年审、活动立项与可行性评估、报名签到、预算报销、经费公示与异议处理，
        一套系统贯穿学生社团的全生命周期。
      </p>
      <ul class="lg-points">
        <li><b>五因子可行性评估</b><span>预算 / 场地 / 容量 / 历史 / 社团资质加权打分，结果可解释</span></li>
        <li><b>经费全链路留痕</b><span>预算 → 报销 → 复核 → 流水 → 公示，每一步可追溯</span></li>
        <li><b>角色化工作台</b><span>校团委、财务、社团负责人、成员各得其所</span></li>
      </ul>
      <div class="lg-foot">大学生社团管理平台 · 毕业设计演示系统</div>
    </div>

    <div class="lg-right">
      <div class="lg-card">
        <div class="lg-card-h">
          <h2>登录</h2>
          <p>请使用校园统一账号进入系统</p>
        </div>
        <el-form :model="form" @submit.prevent>
          <el-form-item>
            <el-input v-model="form.username" size="large" placeholder="用户名" :prefix-icon="User" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.password" size="large" type="password" show-password
                      placeholder="密码" :prefix-icon="Lock" @keyup.enter="submit" />
          </el-form-item>
          <el-button type="primary" size="large" class="lg-btn" :loading="loading" @click="submit">
            登 录
          </el-button>
        </el-form>
        <div class="lg-demo">
          <span class="tip">演示账号（点击填入）</span>
          <div class="chips">
            <button v-for="a in demos" :key="a.u" class="chip" @click="fill(a)">
              {{ a.n }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import http from '../api/http'
import { setLogin } from '../store/user'

const router = useRouter()
const loading = ref(false)
const form = reactive({ username: 'admin', password: 'admin123' })

const demos = [
  { n: '管理员', u: 'admin', p: 'admin123' },
  { n: '校团委', u: 'league01', p: '123456' },
  { n: '财务', u: 'finance01', p: '123456' },
  { n: '社团负责人', u: 'leader01', p: '123456' },
  { n: '社团成员', u: 'member01', p: '123456' }
]

function fill(a) {
  form.username = a.u
  form.password = a.p
}

async function submit() {
  if (!form.username || !form.password) return ElMessage.warning('请输入用户名与密码')
  loading.value = true
  try {
    const data = await http.post('/auth/login', { username: form.username, password: form.password })
    setLogin(data)
    ElMessage.success('欢迎回来，' + (data.realName || data.username))
    router.push(data.roleCode === 'MEMBER' ? '/portal/home' : '/dashboard')
  } catch (e) {
    /* 拦截器已提示 */
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.lg { position: fixed; inset: 0; display: grid; grid-template-columns: 1.05fr .95fr; overflow: hidden; }
.lg-bg {
  position: absolute; inset: 0;
  background: url('../assets/login-bg.jpg') center / cover no-repeat;
}
/* 三层叠加：实景 → 酒红渐变 → 左下暗化，保证文案对比度 */
.lg-shade {
  position: absolute; inset: 0;
  background:
    linear-gradient(105deg, rgba(60, 14, 20, .95) 0%, rgba(96, 26, 35, .88) 42%, rgba(120, 38, 46, .55) 68%, rgba(30, 8, 11, .78) 100%),
    radial-gradient(1200px 600px at 8% 100%, rgba(20, 6, 8, .6), transparent 70%);
}
.lg-left, .lg-right { position: relative; z-index: 2; }
.lg-left {
  color: #fff; padding: 56px 64px; display: flex; flex-direction: column; justify-content: center;
}
.lg-brand {
  display: flex; align-items: center; gap: 9px; font-size: 14px; font-weight: 600;
  letter-spacing: .5px; margin-bottom: 34px; color: rgba(255, 255, 255, .92);
}
.lg-brand .dot { width: 9px; height: 9px; border-radius: 50%; background: #E8B04B; }
.lg-left h1 {
  font-size: 32px; line-height: 1.4; font-weight: 700; margin: 0 0 16px; letter-spacing: 1px;
}
.lg-left .lead {
  font-size: 13px; line-height: 1.95; color: rgba(255, 255, 255, .76); margin: 0 0 28px; max-width: 480px;
}
.lg-points { list-style: none; padding: 0; margin: 0 0 auto; }
.lg-points li {
  padding: 10px 0 10px 16px; border-left: 2px solid rgba(232, 176, 75, .55); margin-bottom: 12px;
}
.lg-points b { display: block; font-size: 13px; font-weight: 600; margin-bottom: 3px; }
.lg-points span { font-size: 11.5px; color: rgba(255, 255, 255, .62); }
.lg-foot { font-size: 11px; color: rgba(255, 255, 255, .38); margin-top: 26px; }

.lg-right { display: flex; align-items: center; justify-content: center; padding: 40px; }
.lg-card {
  width: 100%; max-width: 380px; background: rgba(255, 255, 255, .97);
  border-radius: 12px; padding: 32px 30px 26px;
  box-shadow: 0 18px 50px rgba(20, 6, 8, .35);
}
.lg-card-h { margin-bottom: 22px; }
.lg-card-h h2 { font-size: 20px; font-weight: 700; margin: 0 0 6px; color: #1F2933; }
.lg-card-h p { font-size: 12px; color: #98A2AB; margin: 0; }
.lg-btn { width: 100%; height: 42px; margin-top: 4px; font-size: 14px; letter-spacing: 4px; }
.lg-demo { margin-top: 22px; border-top: 1px dashed #E3E6E8; padding-top: 14px; }
.chips { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 8px; }
.chip {
  border: 1px solid #E3E6E8; background: #fff; color: #5B6770; font-size: 11.5px;
  padding: 4px 9px; border-radius: 12px; cursor: pointer;
}
.chip:hover { border-color: #8C2F39; color: #8C2F39; background: #F6E9EA; }
</style>
