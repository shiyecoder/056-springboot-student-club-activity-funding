<template>
  <div>
    <div class="sec-h">
      <h2>公示异议<span class="sub">待答复 {{ total }} 条</span></h2>
      <el-button @click="load(1)">刷新</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" border>
      <el-table-column prop="objectionNo" label="异议单号" />
      <el-table-column prop="clubName" label="社团" min-width="130" />
      <el-table-column prop="itemType" label="异议类型" />
      <el-table-column prop="itemName" label="涉及科目" min-width="120" />
      <el-table-column label="金额" align="right">
        <template #default="{ row }"><span class="mono">{{ money(row.amount) }}</span></template>
      </el-table-column>
      <el-table-column prop="reason" label="异议理由" min-width="200">
        <template #default="{ row }"><span class="tip">{{ row.reason || '—' }}</span></template>
      </el-table-column>
      <el-table-column label="提交时间">
        <template #default="{ row }">{{ $fmt(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="状态">
        <template #default="{ row }"><StatusTag :text="row.handleStatus || row.status" /></template>
      </el-table-column>
      <el-table-column label="答复" min-width="180">
        <template #default="{ row }"><span class="tip">{{ row.replyContent || '—' }}</span></template>
      </el-table-column>
      <el-table-column label="操作" width="110" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :disabled="(row.handleStatus || '') === '已答复'"
                     @click="open(row)">答复</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination background layout="total, prev, pager, next" :total="total"
                     v-model:current-page="q.page" :page-size="q.size" @current-change="load()" />
    </div>

    <el-dialog v-model="dlg" title="答复异议" width="500px">
      <el-form label-width="88px">
        <el-form-item label="异议单号"><el-input :model-value="cur.objectionNo" disabled /></el-form-item>
        <el-form-item label="异议理由"><el-input :model-value="cur.reason" type="textarea" :rows="2" disabled /></el-form-item>
        <el-form-item label="答复内容">
          <el-input v-model="reply" type="textarea" :rows="4" placeholder="请填写答复内容，不少于 5 个字" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="submit">提交答复</el-button>
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
const cur = ref({})
const reply = ref('')
const q = reactive({ page: 1, size: 10 })

async function load(p) {
  if (p) q.page = p
  loading.value = true
  try {
    const d = await http.get('/fund-objection/page', { params: q })
    rows.value = d.records || []
    total.value = d.total || 0
  } finally { loading.value = false }
}

function open(row) { cur.value = row; reply.value = ''; dlg.value = true }

async function submit() {
  if (!reply.value || reply.value.trim().length < 5) return ElMessage.warning('答复内容不少于 5 个字')
  try {
    await http.post('/fund-objection/' + cur.value.id + '/reply', { replyContent: reply.value })
    ElMessage.success('已答复')
    dlg.value = false
    load()
  } catch (e) { /* 提示 */ }
}

onMounted(() => load())
</script>
