<template>
  <div>
    <div class="sec-h">
      <h2>字典管理<span class="sub">系统枚举统一维护</span></h2>
    </div>

    <div class="grid grid-2u">
      <div class="card">
        <div class="card-h">
          <h3>字典项</h3>
          <el-button type="primary" size="small" @click="openAdd">新增字典项</el-button>
        </div>
        <el-table :data="items" v-loading="loading" border size="small">
          <el-table-column prop="dictLabel" label="显示名" min-width="120" />
          <el-table-column prop="dictValue" label="字典值" min-width="120" />
          <el-table-column prop="sortNo" label="排序" width="70" align="center" />
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <span class="tip">{{ Number(row.status) === 1 ? '启用' : '停用' }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="card">
        <div class="card-h"><h3>字典类型</h3><span class="tip">点击切换左侧字典项</span></div>
        <el-table :data="types" border size="small" highlight-current-row @row-click="pick">
          <el-table-column prop="dictName" label="类型名称" min-width="130" />
          <el-table-column prop="dictType" label="类型编码" min-width="140" />
          <el-table-column prop="remark" label="说明" min-width="120">
            <template #default="{ row }"><span class="tip">{{ row.remark || '—' }}</span></template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <el-dialog v-model="dlg" title="新增字典项" width="460px">
      <el-form :model="form" label-width="88px">
        <el-form-item label="所属类型"><el-input :model-value="curType" disabled /></el-form-item>
        <el-form-item label="显示名"><el-input v-model="form.dictLabel" /></el-form-item>
        <el-form-item label="字典值"><el-input v-model="form.dictValue" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortNo" :min="1" /></el-form-item>
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
import { ElMessage } from 'element-plus'
import http from '../../api/http'

const types = ref([])
const items = ref([])
const curType = ref('')
const loading = ref(false)
const dlg = ref(false)
const form = reactive({ dictLabel: '', dictValue: '', sortNo: 1 })

async function loadTypes() {
  try {
    types.value = (await http.get('/system/dict/types')) || []
    if (types.value.length) pick(types.value[0])
  } catch (e) { /* 提示 */ }
}

async function pick(row) {
  curType.value = row.dictType
  loading.value = true
  try { items.value = (await http.get('/system/dict/data', { params: { type: row.dictType } })) || [] } catch (e) { /* 提示 */ } finally { loading.value = false }
}

function openAdd() {
  if (!curType.value) return ElMessage.warning('请先选择字典类型')
  Object.assign(form, { dictLabel: '', dictValue: '', sortNo: 1 })
  dlg.value = true
}

async function save() {
  if (!form.dictLabel || !form.dictValue) return ElMessage.warning('请填写显示名与字典值')
  try {
    await http.post('/system/dict/data', { ...form, dictType: curType.value, status: 1 })
    ElMessage.success('已新增')
    dlg.value = false
    const t = types.value.find((x) => x.dictType === curType.value)
    if (t) pick(t)
  } catch (e) { /* 提示 */ }
}

onMounted(loadTypes)
</script>
