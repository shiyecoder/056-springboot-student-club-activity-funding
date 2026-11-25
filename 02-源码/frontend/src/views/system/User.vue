<template>
  <div>
    <div class="sec-h">
      <h2>用户管理<span class="sub">共 {{ total }} 个账号</span></h2>
      <el-button type="primary" @click="open()">新增用户</el-button>
    </div>

    <div class="card">
      <div class="filter-bar">
        <el-input v-model="q.keyword" placeholder="用户名 / 姓名" clearable @keyup.enter="load(1)" />
        <el-select v-model="q.roleId" placeholder="角色" clearable>
          <el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="r.id" />
        </el-select>
        <el-select v-model="q.status" placeholder="状态" clearable>
          <el-option label="启用" value="1" />
          <el-option label="停用" value="0" />
        </el-select>
        <el-button type="primary" @click="load(1)">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" border>
        <el-table-column label="头像" width="62">
          <template #default="{ row }">
            <img v-if="row.avatar" :src="row.avatar" class="avatar-sm" alt="" />
            <div v-else class="avatar-sm" style="background:var(--primary-soft)"></div>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="realName" label="姓名" />
        <el-table-column prop="roleName" label="角色" />
        <el-table-column prop="deptName" label="所属单位" min-width="140" />
        <el-table-column prop="phone" label="联系电话" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="Number(row.status) === 1 ? 'success' : 'info'" effect="plain" size="small">
              {{ Number(row.status) === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="150">
          <template #default="{ row }">{{ $fmt(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="open(row)">编辑</el-button>
            <el-button link type="warning" @click="resetPwd(row)">重置密码</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination background layout="total, sizes, prev, pager, next" :total="total"
                       v-model:current-page="q.page" v-model:page-size="q.size"
                       :page-sizes="[10, 20, 50]" @current-change="load()" @size-change="load(1)" />
      </div>
    </div>

    <el-dialog v-model="dlg" :title="form.id ? '编辑用户' : '新增用户'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="用户名"><el-input v-model="form.username" :disabled="!!form.id" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleId" style="width:100%">
            <el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属单位"><el-input v-model="form.deptName" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item v-if="!form.id" label="初始密码">
          <el-input v-model="form.password" placeholder="默认 123456" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../../api/http'

const rows = ref([])
const roles = ref([])
const total = ref(0)
const loading = ref(false)
const dlg = ref(false)
const q = reactive({ page: 1, size: 10, keyword: '', roleId: null, status: '' })
const form = reactive({ id: null, username: '', realName: '', roleId: null, deptName: '', phone: '', password: '123456', status: 1 })

async function load(p) {
  if (p) q.page = p
  loading.value = true
  try {
    const d = await http.get('/system/user/page', { params: q })
    rows.value = d.records || []
    total.value = d.total || 0
  } finally { loading.value = false }
}
function reset() { q.keyword = ''; q.roleId = null; q.status = ''; load(1) }
function open(row) {
  if (row) Object.assign(form, row)
  else Object.assign(form, { id: null, username: '', realName: '', roleId: null, deptName: '', phone: '', password: '123456', status: 1 })
  dlg.value = true
}
async function save() {
  if (!form.username || !form.realName) return ElMessage.warning('请填写用户名与姓名')
  if (form.id) await http.put('/system/user/' + form.id, form)
  else await http.post('/system/user', form)
  ElMessage.success('保存成功')
  dlg.value = false
  load()
}
async function resetPwd(row) {
  await ElMessageBox.confirm('确认将「' + row.username + '」的密码重置为 123456？', '重置密码', { type: 'warning' })
  await http.post('/system/user/' + row.id + '/reset-password')
  ElMessage.success('已重置为 123456')
}

onMounted(async () => {
  try { roles.value = await http.get('/system/role/list') } catch (e) { /* 忽略 */ }
  load()
})
</script>
