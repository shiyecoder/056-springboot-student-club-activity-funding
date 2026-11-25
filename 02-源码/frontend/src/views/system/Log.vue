<template>
  <div>
    <div class="sec-h">
      <h2>操作日志<span class="sub">共 {{ total }} 条，关键写操作全部留痕</span></h2>
    </div>

    <div class="card">
      <div class="filter-bar">
        <el-input v-model="q.keyword" placeholder="操作人 / 内容" clearable @keyup.enter="load(1)" />
        <el-select v-model="q.module" placeholder="模块" clearable>
          <el-option v-for="m in modules" :key="m" :label="m" :value="m" />
        </el-select>
        <el-button type="primary" @click="load(1)">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" border>
        <el-table-column prop="id" label="编号" width="80" align="center" />
        <el-table-column prop="operatorName" label="操作人" min-width="110" />
        <el-table-column prop="module" label="模块" min-width="110" />
        <el-table-column prop="action" label="操作" min-width="120" />
        <el-table-column prop="content" label="内容" min-width="240">
          <template #default="{ row }"><span class="tip">{{ row.content || '—' }}</span></template>
        </el-table-column>
        <el-table-column prop="bizNo" label="业务单号" min-width="140" />
        <el-table-column prop="ip" label="IP" min-width="120" />
        <el-table-column label="时间" min-width="160">
          <template #default="{ row }">{{ $fmt(row.createTime) }}</template>
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

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const modules = ['社团', '成员', '活动', '经费', '系统', '审批']
const q = reactive({ page: 1, size: 10, keyword: '', module: '' })

async function load(p) {
  if (p) q.page = p
  loading.value = true
  try {
    const d = await http.get('/system/log/page', { params: q })
    rows.value = d.records || []
    total.value = d.total || 0
  } finally { loading.value = false }
}
function reset() { q.keyword = ''; q.module = ''; load(1) }

onMounted(() => load())
</script>
