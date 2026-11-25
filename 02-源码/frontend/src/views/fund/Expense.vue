<template>
  <div>
    <div class="sec-h">
      <h2>报销单<span class="sub">共 {{ total }} 张</span></h2>
      <el-button type="primary" @click="open()">新建报销</el-button>
    </div>

    <div class="card">
      <div class="filter-bar">
        <el-input v-model="q.keyword" placeholder="报销单号 / 事由" clearable @keyup.enter="load(1)" />
        <el-select v-model="q.clubId" placeholder="所属社团" clearable filterable>
          <el-option v-for="c in clubs" :key="c.id" :label="c.clubName" :value="c.id" />
        </el-select>
        <el-select v-model="q.status" placeholder="状态" clearable>
          <el-option label="待初审" value="待初审" />
          <el-option label="待财务复核" value="待财务复核" />
          <el-option label="已支付" value="已支付" />
          <el-option label="已驳回" value="已驳回" />
        </el-select>
        <el-button type="primary" @click="load(1)">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" border>
        <el-table-column label="票据" width="84">
          <template #default="{ row }">
            <el-image v-if="firstInvoice(row)" :src="firstInvoice(row)" class="thumb" fit="cover"
                      :preview-src-list="invoiceList(row)" preview-teleported />
            <div v-else class="img-empty"></div>
          </template>
        </el-table-column>
        <el-table-column prop="expenseNo" label="报销单号" min-width="130" />
        <el-table-column prop="clubName" label="社团" min-width="130" />
        <el-table-column prop="activityName" label="关联活动" min-width="150" />
        <el-table-column label="金额" align="right">
          <template #default="{ row }"><span class="mono">{{ money(row.amount) }}</span></template>
        </el-table-column>
        <el-table-column prop="invoiceCount" label="票据张数" align="right" />
        <el-table-column prop="payType" label="支付方式" />
        <el-table-column label="提交时间">
          <template #default="{ row }">{{ $fmt(row.submitTime || row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="状态">
          <template #default="{ row }"><StatusTag :text="row.status" /></template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination background layout="total, sizes, prev, pager, next" :total="total"
                       v-model:current-page="q.page" v-model:page-size="q.size"
                       :page-sizes="[10, 20, 50]" @current-change="load()" @size-change="load(1)" />
      </div>
    </div>

    <el-dialog v-model="dlg" title="新建报销单" width="560px">
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
        <el-form-item label="报销金额">
          <el-input-number v-model="form.amount" :min="0" :step="50" />
        </el-form-item>
        <el-form-item label="支付方式">
          <el-select v-model="form.payType" style="width:160px">
            <el-option label="银行转账" value="银行转账" />
            <el-option label="现金" value="现金" />
            <el-option label="校园卡" value="校园卡" />
          </el-select>
        </el-form-item>
        <el-form-item label="票据图片">
          <el-upload :action="'/api/file/upload?bizType=invoice'" :headers="headers" list-type="picture-card"
                     :on-success="onUp" :on-remove="onDel" accept="image/*">
            <el-icon><Plus /></el-icon>
          </el-upload>
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
const q = reactive({ page: 1, size: 10, keyword: '', clubId: null, status: '' })
const form = reactive({ clubId: null, activityId: null, amount: 0, payType: '银行转账', invoiceUrls: [] })
const headers = { Authorization: 'Bearer ' + (localStorage.getItem('cfs_token') || '') }

function invoiceList(row) {
  const s = row.invoiceUrls || ''
  return String(s).split(',').filter((x) => x.trim())
}
function firstInvoice(row) {
  const l = invoiceList(row)
  return l.length ? l[0] : ''
}

async function load(p) {
  if (p) q.page = p
  loading.value = true
  try {
    const d = await http.get('/expense/page', { params: q })
    rows.value = d.records || []
    total.value = d.total || 0
  } finally { loading.value = false }
}
function reset() { q.keyword = ''; q.clubId = null; q.status = ''; load(1) }
function open() {
  Object.assign(form, { clubId: null, activityId: null, amount: 0, payType: '银行转账', invoiceUrls: [] })
  dlg.value = true
}
function onUp(res) {
  if (res && res.code === 200 && res.data) form.invoiceUrls.push(res.data.url)
}
function onDel(file) {
  const url = file && file.response && file.response.data ? file.response.data.url : ''
  form.invoiceUrls = form.invoiceUrls.filter((u) => u !== url)
}
async function save() {
  if (!form.clubId) return ElMessage.warning('请选择社团')
  if (!form.amount) return ElMessage.warning('请填写报销金额')
  if (!form.invoiceUrls.length) return ElMessage.warning('请上传至少一张票据')
  await http.post('/expense', { ...form, invoiceCount: form.invoiceUrls.length, invoiceUrls: form.invoiceUrls.join(',') })
  ElMessage.success('报销单已提交')
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
