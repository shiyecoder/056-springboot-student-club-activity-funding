<template>
  <div>
    <div class="sec-h">
      <h2>经费公开<span class="sub">已发布公示 {{ total }} 条，公示期内可提异议</span></h2>
    </div>

    <div class="card">
      <el-table :data="rows" border>
        <el-table-column label="社团" min-width="140">
          <template #default="{ row }">
            <img v-if="row.noticeUrl" :src="row.noticeUrl" class="thumb" style="display:inline-block;vertical-align:middle;margin-right:8px" alt="" />
            <span>{{ row.clubName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="收入" align="right">
          <template #default="{ row }"><span class="mono">{{ money(row.incomeAmount) }}</span></template>
        </el-table-column>
        <el-table-column label="支出" align="right">
          <template #default="{ row }"><span class="mono">{{ money(row.expenseAmount) }}</span></template>
        </el-table-column>
        <el-table-column label="结余" align="right">
          <template #default="{ row }"><b class="mono">{{ money(row.balanceAmount) }}</b></template>
        </el-table-column>
        <el-table-column prop="activityCount" label="活动数" align="right" />
        <el-table-column label="公示期" min-width="190">
          <template #default="{ row }">{{ $fmtDate(row.publishStart) }} ~ {{ $fmtDate(row.publishEnd) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><StatusTag :text="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openObj(row)">提异议</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination background layout="total, prev, pager, next" :total="total"
                       v-model:current-page="q.page" :page-size="q.size" @current-change="load()" />
      </div>
    </div>

    <el-dialog v-model="dlg" title="提交经费异议" width="500px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="公示社团"><el-input :model-value="cur.clubName" disabled /></el-form-item>
        <el-form-item label="异议类型">
          <el-select v-model="form.itemType" style="width:100%">
            <el-option label="金额存疑" value="金额存疑" />
            <el-option label="科目不符" value="科目不符" />
            <el-option label="票据缺失" value="票据缺失" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="涉及科目"><el-input v-model="form.itemName" placeholder="如：场地布置" /></el-form-item>
        <el-form-item label="涉及金额">
          <el-input-number v-model="form.amount" :min="0" :step="10" />
        </el-form-item>
        <el-form-item label="异议理由">
          <el-input v-model="form.reason" type="textarea" :rows="3" placeholder="请说明你的疑问，不少于 5 个字" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="submit">提交异议</el-button>
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
const dlg = ref(false)
const cur = ref({})
const q = reactive({ page: 1, size: 10, status: '公示中' })
const form = reactive({ itemType: '金额存疑', itemName: '', amount: 0, reason: '' })

async function load(p) {
  if (p) q.page = p
  try {
    const d = await http.get('/fund-disclosure/page', { params: q })
    rows.value = d.records || []
    total.value = d.total || 0
  } catch (e) { /* 提示 */ }
}

function openObj(row) {
  cur.value = row
  Object.assign(form, { itemType: '金额存疑', itemName: '', amount: 0, reason: '' })
  dlg.value = true
}

async function submit() {
  if (!form.reason || form.reason.trim().length < 5) return ElMessage.warning('异议理由不少于 5 个字')
  try {
    await http.post('/fund-objection', { ...form, disclosureId: cur.value.id, clubId: cur.value.clubId })
    ElMessage.success('异议已提交，社团负责人将在公示期内答复')
    dlg.value = false
  } catch (e) { /* 提示 */ }
}

onMounted(() => load())
</script>
