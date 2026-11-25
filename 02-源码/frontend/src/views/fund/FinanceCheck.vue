<template>
  <div>
    <div class="sec-h">
      <h2>财务复核<span class="sub">待复核 {{ total }} 张 · 复核通过后自动生成经费流水</span></h2>
      <el-button @click="load(1)">刷新</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" border>
      <el-table-column label="票据" width="70">
        <template #default="{ row }">
          <el-image v-if="first(row)" :src="first(row)" class="thumb" fit="cover"
                    :preview-src-list="list(row)" preview-teleported />
          <div v-else class="img-empty"></div>
        </template>
      </el-table-column>
      <el-table-column prop="expenseNo" label="报销单号" min-width="130" />
      <el-table-column prop="clubName" label="社团" min-width="130" />
      <el-table-column prop="activityName" label="关联活动" min-width="150" />
      <el-table-column label="金额" align="right">
        <template #default="{ row }"><span class="mono" style="font-weight:600">{{ money(row.amount) }}</span></template>
      </el-table-column>
      <el-table-column prop="invoiceCount" label="票据" align="right" />
      <el-table-column prop="payType" label="支付方式" />
      <el-table-column label="提交时间">
        <template #default="{ row }">{{ $fmt(row.submitTime || row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="状态">
        <template #default="{ row }"><StatusTag :text="row.status" /></template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="open(row, 'PASS')">复核通过</el-button>
          <el-button link type="danger" @click="open(row, 'REJECT')">退回</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination background layout="total, prev, pager, next" :total="total"
                     v-model:current-page="q.page" :page-size="q.size" @current-change="load()" />
    </div>

    <el-dialog v-model="dlg" :title="mode === 'PASS' ? '财务复核通过' : '退回报销单'" width="480px">
      <el-form label-width="88px">
        <el-form-item label="报销单号"><el-input :model-value="cur.expenseNo" disabled /></el-form-item>
        <el-form-item label="金额"><el-input :model-value="money(cur.amount) + ' 元'" disabled /></el-form-item>
        <el-form-item label="复核意见">
          <el-input v-model="remark" type="textarea" :rows="3" placeholder="请填写复核意见" />
        </el-form-item>
        <div class="tip">复核通过后将生成一条经费支出流水，并回写社团可用额度。</div>
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
const remark = ref('')
const q = reactive({ page: 1, size: 10, status: '待财务复核' })

function list(row) { return String(row.invoiceUrls || '').split(',').filter((x) => x.trim()) }
function first(row) { const l = list(row); return l.length ? l[0] : '' }

async function load(p) {
  if (p) q.page = p
  loading.value = true
  try {
    const d = await http.get('/expense/page', { params: q })
    rows.value = d.records || []
    total.value = d.total || 0
  } finally { loading.value = false }
}

function open(row, m) { cur.value = row; mode.value = m; remark.value = ''; dlg.value = true }

async function submit() {
  if (mode.value === 'REJECT' && (!remark.value || remark.value.trim().length < 5)) {
    return ElMessage.warning('退回需填写不少于 5 字的理由')
  }
  try {
    await http.post('/expense/' + cur.value.id + '/finance-check', { result: mode.value, remark: remark.value })
    ElMessage.success('复核完成')
    dlg.value = false
    load()
  } catch (e) { /* 提示 */ }
}

onMounted(() => load())
</script>
