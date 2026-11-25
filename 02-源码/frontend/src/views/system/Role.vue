<template>
  <div>
    <div class="sec-h">
      <h2>角色管理<span class="sub">共 {{ roles.length }} 个角色</span></h2>
      <el-button type="primary" @click="open()">新增角色</el-button>
    </div>

    <div class="card">
      <el-table :data="roles" v-loading="loading" border>
        <el-table-column prop="roleCode" label="角色编码" />
        <el-table-column prop="roleName" label="角色名称" min-width="140" />
        <el-table-column prop="remark" label="说明" min-width="220">
          <template #default="{ row }"><span class="tip">{{ row.remark || '—' }}</span></template>
        </el-table-column>
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
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="open(row)">编辑</el-button>
            <el-button link type="primary" @click="assign(row)">分配菜单</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dlg" :title="form.id ? '编辑角色' : '新增角色'" width="480px">
      <el-form :model="form" label-width="86px">
        <el-form-item label="角色编码"><el-input v-model="form.roleCode" :disabled="!!form.id" placeholder="如 LEAGUE" /></el-form-item>
        <el-form-item label="角色名称"><el-input v-model="form.roleName" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dlgMenu" :title="'为「' + cur.roleName + '」分配菜单'" width="520px">
      <el-tree ref="treeRef" :data="menuTree" node-key="id" show-checkbox default-expand-all
               :props="{ label: 'name', children: 'children' }" />
      <template #footer>
        <el-button @click="dlgMenu = false">取消</el-button>
        <el-button type="primary" @click="saveMenu">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../../api/http'

const roles = ref([])
const menuTree = ref([])
const loading = ref(false)
const dlg = ref(false)
const dlgMenu = ref(false)
const treeRef = ref(null)
const cur = ref({})
const form = reactive({ id: null, roleCode: '', roleName: '', remark: '', status: 1 })

async function load() {
  loading.value = true
  try { roles.value = await http.get('/system/role/list') } finally { loading.value = false }
}

function open(row) {
  if (row) Object.assign(form, row)
  else Object.assign(form, { id: null, roleCode: '', roleName: '', remark: '', status: 1 })
  dlg.value = true
}

async function save() {
  if (!form.roleCode || !form.roleName) return ElMessage.warning('请填写角色编码与名称')
  if (form.id) await http.put('/system/role/' + form.id, form)
  else await http.post('/system/role', form)
  ElMessage.success('保存成功')
  dlg.value = false
  load()
}

async function assign(row) {
  cur.value = row
  dlgMenu.value = true
  if (!menuTree.value.length) {
    const tree = await http.get('/system/menu/tree')
    // 隐藏「菜单管理」本身，避免把菜单管理暴露给非管理员角色（规范第 12 条）
    menuTree.value = hideMenuMgmt(tree || [])
  }
  const checked = await http.get('/system/role/' + row.id + '/menus')
  await nextTick()
  if (treeRef.value) {
    treeRef.value.setCheckedKeys([])
    ;(checked || []).forEach((id) => treeRef.value.setChecked(id, true, false))
  }
}

function hideMenuMgmt(list) {
  // 后端菜单字段是 name（不是 menuName），这里统一归一化，el-tree 用 name 作标签
  return list
    .filter((m) => m.path !== '/system/menu')
    .map((m) => ({
      ...m,
      name: m.name || m.menuName,
      children: m.children ? hideMenuMgmt(m.children) : []
    }))
}

async function saveMenu() {
  const ids = treeRef.value.getCheckedKeys().concat(treeRef.value.getHalfCheckedKeys())
  await http.post('/system/role/' + cur.value.id + '/menus', { menuIds: ids })
  ElMessage.success('菜单权限已保存')
  dlgMenu.value = false
}

onMounted(load)
</script>
