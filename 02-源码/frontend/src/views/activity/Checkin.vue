<template>
  <div>
    <div class="sec-h">
      <h2>签到核验<span class="sub">选择进行中的活动核对到场情况</span></h2>
    </div>
    <div class="card">
      <div class="filter-bar">
        <el-select v-model="actId" placeholder="选择活动" filterable style="width:340px" @change="loadBoard">
          <el-option v-for="a in acts" :key="a.id" :label="a.activityName" :value="a.id" />
        </el-select>
        <el-input v-model="code" placeholder="输入签到码" style="width:180px" />
        <el-button type="primary" @click="doCheckin">核验签到</el-button>
      </div>

      <div v-if="board" class="grid grid-4" style="margin-bottom:12px">
        <div class="stat"><div class="tip">报名人数</div><div class="num">{{ board.signupTotal || 0 }}</div></div>
        <div class="stat"><div class="tip">已签到</div><div class="num">{{ board.signedTotal || 0 }}</div></div>
        <div class="stat"><div class="tip">未签到</div><div class="num">{{ (board.signupTotal || 0) - (board.signedTotal || 0) }}</div></div>
        <div class="stat"><div class="tip">签到率</div><div class="num">{{ board.attendRate || 0 }}<em>%</em></div></div>
      </div>

      <el-table :data="list" v-loading="loading" border>
        <el-table-column prop="signupNo" label="报名单号" />
        <el-table-column prop="studentName" label="姓名" min-width="100" />
        <el-table-column prop="studentId" label="学号" />
        <el-table-column prop="signupType" label="报名方式" />
        <el-table-column label="报名时间">
          <template #default="{ row }">{{ $fmt(row.signupTime) }}</template>
        </el-table-column>
        <el-table-column label="签到时间">
          <template #default="{ row }">{{ $fmt(row.checkinTime) }}</template>
        </el-table-column>
        <el-table-column label="签到状态">
          <template #default="{ row }"><StatusTag :text="row.checkinStatus || '未到'" /></template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="row.checkinStatus === '已签到'"
                       @click="signOne(row)">补签</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../../api/http'
import StatusTag from '../../components/StatusTag.vue'

const acts = ref([])
const actId = ref(null)
const board = ref(null)
const list = ref([])
const loading = ref(false)
const code = ref('')

async function loadBoard() {
  if (!actId.value) return
  board.value = null
  list.value = []
  loading.value = true
  try {
    const b = await http.get('/activity/' + actId.value + '/checkin-board')
    board.value = b
    list.value = b.signupList || b.records || []
  } catch (e) { /* 提示 */ } finally { loading.value = false }
}

async function doCheckin() {
  if (!actId.value) return ElMessage.warning('请先选择活动')
  if (!code.value) return ElMessage.warning('请输入签到码')
  try {
    await http.post('/activity/' + actId.value + '/checkin', { checkinCode: code.value, code: code.value })
    ElMessage.success('核验成功')
    code.value = ''
    loadBoard()
  } catch (e) { /* 提示 */ }
}

async function signOne(row) {
  try {
    await http.post('/activity/' + actId.value + '/checkin', { signupId: row.id })
    ElMessage.success('已补签')
    loadBoard()
  } catch (e) { /* 提示 */ }
}

onMounted(async () => {
  try {
    const d = await http.get('/activity/page', { params: { page: 1, size: 100 } })
    acts.value = d.records || []
    const run = acts.value.find((a) => a.status === '进行中') || acts.value[0]
    if (run) { actId.value = run.id; loadBoard() }
  } catch (e) { /* 提示 */ }
})
</script>

<style scoped>
.stat { background: #fff; border-radius: 8px; box-shadow: var(--shadow); padding: 12px 14px; }
.stat .num { font-size: 22px; font-weight: 700; margin-top: 4px; font-variant-numeric: tabular-nums; }
.stat .num em { font-style: normal; font-size: 12px; color: var(--text-3); }
</style>
