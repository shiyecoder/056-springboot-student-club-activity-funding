<template>
  <div>
    <div class="sec-h">
      <h2>预算审核<span class="sub">待审核 {{ total }} 条</span></h2>
      <el-button @click="load(1)">刷新</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" border>
      <el-table-column prop="budgetNo" label="预算单号" min-width="130" />
      <el-table-column prop="clubName" label="社团" min-width="130" />
      <el-table-column prop="activityName" label="关联活动" min-width="160" />
      <el-table-column prop="itemName" label="预算科目" min-width="120" />
      <el-table-column label="申请金额" align="right">
        <template #default="{ row }"><span class="mono">{{ money(row.applyAmount) }}</span></template>
      </el-table-column>
      <el-table-column label="测算依据" min-width="180">
        <template #default="{ row }"><span class="tip">{{ row.amountBasis || '—' }}</span></template>
      </el-table-column>
      <el-table-column label="状态">
        <template #default="{ row }"><StatusTag :text="row.status" /></template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="success" @click="open(row, 'PASS')">通过</el-button>
          <el-button link type="danger" @click="open(row, 'REJECT')">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination background layout="total, prev, pager, next" :total="total"
                     v-model:current-page="q.page" :page-size="q.size" @current-change="load()" />
    </div>

    <el-dialog v-model="dlg" :title="mode === 'PASS' ? '审核通过' : '驳回预算'" width="480px">
      <el-form label-width="92px">
        <el-form-item label="预算科目"><el-input :model-value="cur.itemName" disabled /></el-form-item>
        <el-form-item v-if="mode === 'PASS'" label="核定金额">
          <el-input-number v-model="form.approveAmount" :min="0" :step="100" />
          <div class="tip" style="margin-left:8px">可低于申请金额，调减需说明理由</div>
        </el-form-item>
        <el-form-item label="审核意见">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请填写审核意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="submit">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../../api/http'
import StatusTag from '../../components/StatusTag.vue'
import { money } from '../../utils/format'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const dlg = ref(false)
const mode = ref('PASS')
const cur = ref({})
const q = reactive({ page: 1, size: 10, status: '待审核' })
const form = reactive({ approveAmount: 0, remark: '' })

async function load(p) {
  if (p) q.page = p
  loading.value = true
  try {
    const d = await http.get('/budget/page', { params: q })
    rows.value = d.records || []
    total.value = d.total || 0
  } finally { loading.value = false }
}

function open(row, m) {
  cur.value = row
  mode.value = m
  form.approveAmount = Number(row.applyAmount || 0)
  form.remark = ''
  dlg.value = true
}

async function submit() {
  // 调减金额必须给出理由（后端同样会校验）
  if (mode.value === 'PASS' && form.approveAmount < Number(cur.value.applyAmount || 0)) {
    if (!form.remark || form.remark.trim().length < 5) return ElMessage.warning('调减金额需填写不少于 5 字的理由')
  }
  if (mode.value === 'REJECT' && (!form.remark || form.remark.trim().length < 5)) {
    return ElMessage.warning('驳回需填写不少于 5 字的理由')
  }
  try {
    await http.post('/budget/' + cur.value.id + '/approve', {
      result: mode.value, approveAmount: mode.value === 'PASS' ? form.approveAmount : 0, remark: form.remark
    })
    ElMessage.success('审核完成')
    dlg.value = false
    load()
  } catch (e) { /* 提示 */ }
}

onMounted(() => load())
</script>
