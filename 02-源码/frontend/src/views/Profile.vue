<template>
  <div>
    <div class="sec-h">
      <h2>个人中心<span class="sub">修改资料、头像与密码</span></h2>
    </div>

    <div class="grid grid-2u">
      <div class="card">
        <div class="card-h"><h3>基本资料</h3></div>
        <div class="prof-head">
          <el-upload :action="'/api/file/upload?bizType=avatar'" :headers="headers" :show-file-list="false"
                     :on-success="onUp" accept="image/*">
            <div class="avatar-box">
              <img v-if="form.avatar" :src="form.avatar" alt="" />
              <div v-else class="ph">上传头像</div>
              <div class="mask">更换</div>
            </div>
          </el-upload>
          <div class="meta">
            <div class="name">{{ form.realName || '-' }}</div>
            <div class="tip">{{ store.roleName }} · {{ form.deptName || '未设置单位' }}</div>
            <div class="tip">账号 {{ form.username }}</div>
          </div>
        </div>
        <el-form :model="form" label-width="90px" style="margin-top:16px">
          <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
          <el-form-item label="所属单位"><el-input v-model="form.deptName" /></el-form-item>
          <el-form-item label="联系电话"><el-input v-model="form.phone" /></el-form-item>
          <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
          <el-form-item>
            <el-button type="primary" @click="saveProfile">保存资料</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div>
        <div class="card">
          <div class="card-h"><h3>修改密码</h3></div>
          <el-form :model="pwd" label-width="90px">
            <el-form-item label="原密码">
              <el-input v-model="pwd.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码">
              <el-input v-model="pwd.newPassword" type="password" show-password placeholder="不少于 6 位" />
            </el-form-item>
            <el-form-item label="确认密码">
              <el-input v-model="pwd.confirm" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="savePwd">修改密码</el-button>
            </el-form-item>
          </el-form>
        </div>

        <div class="card">
          <div class="card-h"><h3>账号信息</h3></div>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="用户编号">{{ store.userId }}</el-descriptions-item>
            <el-descriptions-item label="角色">{{ store.roleName }}（{{ store.roleCode }}）</el-descriptions-item>
            <el-descriptions-item label="所属单位">{{ form.deptName || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../api/http'
import { store, setAvatar } from '../store/user'

const form = reactive({ username: '', realName: '', deptName: '', phone: '', email: '', avatar: '' })
const pwd = reactive({ oldPassword: '', newPassword: '', confirm: '' })
const headers = { Authorization: 'Bearer ' + (localStorage.getItem('cfs_token') || '') }

function onUp(res) {
  if (res && res.code === 200 && res.data) {
    form.avatar = res.data.url
    setAvatar(res.data.url)
    ElMessage.success('头像已更新')
    saveProfile()
  }
}

async function saveProfile() {
  await http.put('/auth/profile', form)
  ElMessage.success('资料已保存')
}

async function savePwd() {
  if (!pwd.oldPassword || !pwd.newPassword) return ElMessage.warning('请填写原密码与新密码')
  if (pwd.newPassword.length < 6) return ElMessage.warning('新密码不少于 6 位')
  if (pwd.newPassword !== pwd.confirm) return ElMessage.warning('两次输入的新密码不一致')
  await http.post('/auth/password', { oldPassword: pwd.oldPassword, newPassword: pwd.newPassword })
  ElMessage.success('密码已修改')
  pwd.oldPassword = ''
  pwd.newPassword = ''
  pwd.confirm = ''
}

onMounted(async () => {
  try {
    const d = await http.get('/auth/me')
    Object.assign(form, d || {})
  } catch (e) { /* 提示 */ }
})
</script>

<style scoped>
.prof-head { display: flex; align-items: center; gap: 18px; }
.avatar-box {
  width: 84px; height: 84px; border-radius: 50%; overflow: hidden; position: relative;
  background: var(--primary-soft); display: flex; align-items: center; justify-content: center;
}
.avatar-box img { width: 100%; height: 100%; object-fit: cover; }
.avatar-box .ph { font-size: 11.5px; color: var(--text-3); }
.avatar-box .mask {
  position: absolute; inset: 0; background: rgba(31, 41, 51, .55); color: #fff;
  font-size: 11.5px; display: flex; align-items: center; justify-content: center;
  opacity: 0; transition: opacity .2s;
}
.avatar-box:hover .mask { opacity: 1; }
.meta .name { font-size: 16px; font-weight: 700; margin-bottom: 4px; }
.meta .tip { line-height: 1.7; }
</style>
