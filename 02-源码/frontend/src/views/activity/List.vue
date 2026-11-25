<template>
  <div>
    <div class="sec-h">
      <h2>活动列表<span class="sub">共 {{ total }} 场活动</span></h2>
      <el-button type="primary" @click="$router.push('/activity/apply')">申报活动</el-button>
    </div>
    <div class="card">
      <div class="filter-bar">
        <el-input v-model="q.keyword" placeholder="活动名称 / 编号" clearable @keyup.enter="load(1)" />
        <el-select v-model="q.clubId" placeholder="所属社团" clearable filterable>
          <el-option v-for="c in clubs" :key="c.id" :label="c.clubName" :value="c.id" />
        </el-select>
        <el-select v-model="q.status" placeholder="状态" clearable>
          <el-option v-for="s in statusList" :key="s" :label="s" :value="s" />
        </el-select>
        <el-select v-model="q.activityType" placeholder="活动类型" clearable>
          <el-option v-for="t in typeList" :key="t" :label="t" :value="t" />
        </el-select>
        <el-button type="primary" @click="load(1)">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <el-table :data="rows" v-loading="loading" border>
        <el-table-column label="封面" width="70">
          <template #default="{ row }">
            <img v-if="row.coverUrl" :src="row.coverUrl" class="thumb" alt="" />
            <div v-else class="img-empty"></div>
          </template>
        </el-table-column>
        <el-table-column prop="activityNo" label="活动编号" min-width="110" />
        <el-table-column prop="activityName" label="活动名称" min-width="180" />
        <el-table-column prop="clubName" label="主办社团" min-width="130" />
        <el-table-column prop="activityType" label="类型" />
        <el-table-column label="开始时间" min-width="150">
          <template #default="{ row }">{{ $fmt(row.startTime) }}</template>
        </el-table-column>
        <el-table-column prop="placeName" label="场地" />
        <el-table-column label="预算" align="right">
          <template #default="{ row }"><span class="mono">{{ money(row.budgetTotal) }}</span></template>
        </el-table-column>
        <el-table-column label="签到率" align="right">
          <template #default="{ row }">
            {{ row.expectCount ? Math.round((row.actualCount || 0) * 100 / row.expectCount) + '%' : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="可行性" align="center">
          <template #default="{ row }">{{ row.feasibilityScore || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态">
          <template #default="{ row }"><StatusTag :text="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push('/activity/detail/' + row.id)">详情</el-button>
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
import http from '../../api/http'
import StatusTag from '../../components/StatusTag.vue'
import { money } from '../../utils/format'

const rows = ref([])
const clubs = ref([])
const total = ref(0)
const loading = ref(false)
const statusList = ['待审批', '已立项', '进行中', '已结束', '已结项', '已驳回']
const typeList = ['讲座沙龙', '竞赛比赛', '志愿服务', '文艺演出', '社会实践', '招新宣传']
const q = reactive({ page: 1, size: 10, keyword: '', clubId: null, status: '', activityType: '' })

async function load(p) {
  if (p) q.page = p
  loading.value = true
  try {
    const d = await http.get('/activity/page', { params: q })
    rows.value = d.records || []
    total.value = d.total || 0
  } finally { loading.value = false }
}
function reset() {
  q.keyword = ''; q.clubId = null; q.status = ''; q.activityType = ''
  load(1)
}
onMounted(async () => {
  try { clubs.value = await http.get('/club/options') } catch (e) { /* 忽略 */ }
  load()
})
</script>
