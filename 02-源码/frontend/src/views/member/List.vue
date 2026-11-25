<template>
  <div>
    <div class="sec-h">
      <h2>成员名册<span class="sub">共 {{ total }} 名成员</span></h2>
    </div>

    <div class="card">
      <div class="filter-bar">
        <el-input v-model="q.keyword" placeholder="学号 / 姓名" clearable @keyup.enter="load(1)" />
        <el-select v-model="q.clubId" placeholder="所属社团" clearable filterable>
          <el-option v-for="c in clubs" :key="c.id" :label="c.clubName" :value="c.id" />
        </el-select>
        <el-select v-model="q.memberRole" placeholder="社团职务" clearable>
          <el-option label="社长" value="社长" />
          <el-option label="副社长" value="副社长" />
          <el-option label="部长" value="部长" />
          <el-option label="干事" value="干事" />
          <el-option label="成员" value="成员" />
        </el-select>
        <el-select v-model="q.status" placeholder="状态" clearable>
          <el-option label="在册" value="在册" />
          <el-option label="已退出" value="已退出" />
          <el-option label="已除名" value="已除名" />
        </el-select>
        <el-button type="primary" @click="load(1)">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" border>
        <el-table-column label="头像" width="62">
          <template #default="{ row }">
            <img v-if="row.avatarUrl" :src="row.avatarUrl" class="avatar-sm" alt="" />
            <div v-else class="avatar-sm" style="background:var(--primary-soft)"></div>
          </template>
        </el-table-column>
        <el-table-column prop="memberNo" label="成员编号" />
        <el-table-column label="姓名" min-width="100">
          <template #default="{ row }">{{ row.studentName || row.realName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="clubName" label="所属社团" min-width="140" />
        <el-table-column prop="deptName" label="院系" />
        <el-table-column prop="memberRole" label="职务" />
        <el-table-column prop="activityCount" label="参与活动" align="right" />
        <el-table-column label="志愿服务" align="right">
          <template #default="{ row }">{{ row.serviceHours || 0 }} h</template>
        </el-table-column>
        <el-table-column label="入社日期">
          <template #default="{ row }">{{ $fmtDate(row.joinDate) }}</template>
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
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import http from '../../api/http'
import StatusTag from '../../components/StatusTag.vue'

const rows = ref([])
const clubs = ref([])
const total = ref(0)
const loading = ref(false)
const q = reactive({ page: 1, size: 10, keyword: '', clubId: null, memberRole: '', status: '' })

async function load(p) {
  if (p) q.page = p
  loading.value = true
  try {
    const d = await http.get('/club-member/page', { params: q })
    rows.value = d.records || []
    total.value = d.total || 0
  } finally { loading.value = false }
}

function reset() {
  q.keyword = ''
  q.clubId = null
  q.memberRole = ''
  q.status = ''
  load(1)
}

onMounted(async () => {
  try { clubs.value = await http.get('/club/options') } catch (e) { /* 忽略 */ }
  load()
})
</script>
