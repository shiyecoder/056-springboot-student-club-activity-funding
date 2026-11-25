<template>
  <div>
    <div class="sec-h">
      <h2>经费公示<span class="sub">按学期公示收支，公示期内可提异议</span></h2>
    </div>

    <div class="card">
      <div class="filter-bar">
        <el-select v-model="q.clubId" placeholder="所属社团" clearable filterable>
          <el-option v-for="c in clubs" :key="c.id" :label="c.clubName" :value="c.id" />
        </el-select>
        <el-select v-model="q.status" placeholder="状态" clearable>
          <el-option label="待公示" value="待公示" />
          <el-option label="公示中" value="公示中" />
          <el-option label="已归档" value="已归档" />
        </el-select>
        <el-button type="primary" @click="load(1)">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" border>
        <el-table-column label="公示配图" width="80">
          <template #default="{ row }">
            <img v-if="row.noticeUrl" :src="row.noticeUrl" class="thumb" alt="" />
            <div v-else class="img-empty"></div>
          </template>
        </el-table-column>
        <el-table-column prop="disclosureNo" label="公示编号" />
        <el-table-column prop="clubName" label="社团" min-width="130" />
        <el-table-column label="收入" align="right">
          <template #default="{ row }"><span class="mono">{{ money(row.incomeAmount) }}</span></template>
        </el-table-column>
        <el-table-column label="支出" align="right">
          <template #default="{ row }"><span class="mono">{{ money(row.expenseAmount) }}</span></template>
        </el-table-column>
        <el-table-column label="结余" align="right">
          <template #default="{ row }"><span class="mono" style="font-weight:600">{{ money(row.balanceAmount) }}</span></template>
        </el-table-column>
        <el-table-column prop="activityCount" label="活动数" align="right" />
        <el-table-column label="公示期" min-width="180">
          <template #default="{ row }">
            {{ $fmtDate(row.publishStart) }} ~ {{ $fmtDate(row.publishEnd) }}
          </template>
        </el-table-column>
        <el-table-column label="异议" align="center">
          <template #default="{ row }">{{ row.objectionCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="状态">
          <template #default="{ row }"><StatusTag :text="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="publish(row)" :disabled="row.status !== '待公示'">发布</el-button>
            <el-button link type="primary" @click="$router.push('/fund/objection')">异议</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination background layout="total, sizes, prev, pager, next" :total="total"
                       v-model:current-page="q.page" v-model:page-size="q.size"
                       :page-sizes="[10, 20, 50]" @current-change="load()" @size-change="load(1)" />
      </div>
    </div>
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
const total = ref(0)
const loading = ref(false)
const q = reactive({ page: 1, size: 10, clubId: null, status: '' })

async function load(p) {
  if (p) q.page = p
  loading.value = true
  try {
    const d = await http.get('/fund-disclosure/page', { params: q })
    rows.value = d.records || []
    total.value = d.total || 0
  } finally { loading.value = false }
}
function reset() { q.clubId = null; q.status = ''; load(1) }

async function publish(row) {
  try {
    await http.post('/fund-disclosure/' + row.id + '/publish')
    ElMessage.success('已发布，公示期内学生可提异议')
    load()
  } catch (e) { /* 提示 */ }
}

onMounted(async () => {
  try { clubs.value = await http.get('/club/options') } catch (e) { /* 忽略 */ }
  load()
})
</script>
