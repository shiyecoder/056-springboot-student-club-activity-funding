<template>
  <div>
    <div class="sec-h">
      <h2>预算编制<span class="sub">按活动逐项编制，提交后进入团委审核</span></h2>
      <el-button type="primary" @click="open()">新增预算科目</el-button>
    </div>

    <div class="card">
      <div class="filter-bar">
        <el-select v-model="q.clubId" placeholder="所属社团" clearable filterable>
          <el-option v-for="c in clubs" :key="c.id" :label="c.clubName" :value="c.id" />
        </el-select>
        <el-select v-model="q.status" placeholder="状态" clearable>
          <el-option label="待审核" value="待审核" />
          <el-option label="已通过" value="已通过" />
          <el-option label="已调减" value="已调减" />
        </el-select>
        <el-button type="primary" @click="load(1)">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" border>
        <el-table-column prop="budgetNo" label="预算单号" min-width="130" />
        <el-table-column prop="clubName" label="社团" min-width="130" />
        <el-table-column prop="activityName" label="关联活动" min-width="160" />
        <el-table-column prop="itemName" label="预算科目" min-width="120" />
        <el-table-column prop="itemType" label="科目类型" />
        <el-table-column label="申请金额" align="right">
          <template #default="{ row }"><span class="mono">{{ money(row.applyAmount) }}</span></template>
        </el-table-column>
        <el-table-column label="核定金额" align="right">
          <template #default="{ row }"><span class="mono">{{ money(row.approveAmount) }}</span></template>
        </el-table-column>
        <el-table-column label="测算依据" min-width="160">
          <template #default="{ row }"><span class="tip">{{ row.amountBasis || '—' }}</span></template>
        </el-table-column>
        <el-table-column label="状态">
          <template #default="{ row }"><StatusTag :text="row.status" /></template>
        </el-table-column>
        <el-table-column label="提交时间">
          <template #default="{ row }">{{ $fmt(row.createTime) }}</template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination background layout="total, sizes, prev, pager, next" :total="total"
                       v-model:current-page="q.page" v-model:page-size="q.size"
                       :page-sizes="[10, 20, 50]" @current-change="load()" @size-change="load(1)" />
      </div>
    </div>

    <el-dialog v-model="dlg" title="新增预算科目" width="520px">
      <el-form :model="form" label-width="92px">
        <el-form-item label="所属社团">
          <el-select v-model="form.clubId" filterable style="width:100%">
            <el-option v-for="c in clubs" :key="c.id" :label="c.clubName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联活动">
          <el-select v-model="form.activityId" filterable style="width:100%">
            <el-option v-for="a in acts" :key="a.id" :label="a.activityName" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="预算科目"><el-input v-model="form.itemName" placeholder="如：场地布置" /></el-form-item>
        <el-form-item label="科目类型">
          <el-select v-model="form.itemType" style="width:100%">
            <el-option label="物料" value="物料" />
            <el-option label="场地" value="场地" />
            <el-option label="宣传" value="宣传" />
            <el-option label="交通" value="交通" />
            <el-option label="劳务" value="劳务" />
          </el-select>
        </el-form-item>
        <el-form-item label="申请金额">
          <el-input-number v-model="form.applyAmount" :min="0" :step="100" />
        </el-form-item>
        <el-form-item label="测算依据">
          <el-input v-model="form.amountBasis" type="textarea" :rows="2" placeholder="如：单价 × 数量，说明测算过程" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="save">提交</el-button>
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
const clubs = ref([])
const acts = ref([])
const total = ref(0)
const loading = ref(false)
const dlg = ref(false)
const q = reactive({ page: 1, size: 10, clubId: null, status: '' })
const form = reactive({ clubId: null, activityId: null, itemName: '', itemType: '物料', applyAmount: 0, amountBasis: '' })

async function load(p) {
  if (p) q.page = p
  loading.value = true
  try {
    const d = await http.get('/budget/page', { params: q })
    rows.value = d.records || []
    total.value = d.total || 0
  } finally { loading.value = false }
}
function reset() { q.clubId = null; q.status = ''; load(1) }
function open() {
  Object.assign(form, { clubId: null, activityId: null, itemName: '', itemType: '物料', applyAmount: 0, amountBasis: '' })
  dlg.value = true
}
async function save() {
  if (!form.clubId || !form.itemName) return ElMessage.warning('请填写社团与预算科目')
  if (!form.applyAmount) return ElMessage.warning('请填写申请金额')
  await http.post('/budget', form)
  ElMessage.success('预算已提交审核')
  dlg.value = false
  load()
}

onMounted(async () => {
  try { clubs.value = await http.get('/club/options') } catch (e) { /* 忽略 */ }
  try {
    const d = await http.get('/activity/page', { params: { page: 1, size: 100 } })
    acts.value = d.records || []
  } catch (e) { /* 忽略 */ }
  load()
})
</script>
