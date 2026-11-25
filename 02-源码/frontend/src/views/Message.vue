<template>
  <div>
    <div class="sec-h">
      <h2>消息中心<span class="sub">未读 {{ total }} 条</span></h2>
      <el-button @click="readAll" :disabled="!total">全部标记已读</el-button>
    </div>

    <div class="card">
      <el-table :data="rows" v-loading="loading" border>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="Number(row.status) === 0 ? 'danger' : 'info'" effect="plain" size="small">
              {{ Number(row.status) === 0 ? '未读' : '已读' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column prop="content" label="内容" min-width="320">
          <template #default="{ row }"><span class="tip">{{ row.content || '—' }}</span></template>
        </el-table-column>
        <el-table-column prop="msgType" label="类型" width="100" />
        <el-table-column label="时间" min-width="160">
          <template #default="{ row }">{{ $fmt(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="Number(row.status) !== 0" @click="read(row)">标为已读</el-button>
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
import http from '../api/http'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const q = reactive({ page: 1, size: 10 })

async function load(p) {
  if (p) q.page = p
  loading.value = true
  try {
    const d = await http.get('/system/message/page', { params: q })
    rows.value = d.records || []
    total.value = d.total || 0
  } finally { loading.value = false }
}

async function read(row) {
  await http.post('/system/message/' + row.id + '/read')
  load()
}

async function readAll() {
  await http.post('/system/message/read-all')
  ElMessage.success('已全部标记为已读')
  load()
}

onMounted(() => load())
</script>
