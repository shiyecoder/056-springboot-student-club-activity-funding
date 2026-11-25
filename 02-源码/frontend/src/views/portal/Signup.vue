<template>
  <div>
    <div class="sec-h">
      <h2>我的报名<span class="sub">共 {{ rows.length }} 条</span></h2>
    </div>

    <div class="card">
      <el-table :data="rows" border>
        <el-table-column prop="signupNo" label="报名单号" min-width="130" />
        <el-table-column prop="activityName" label="活动名称" min-width="180" />
        <el-table-column prop="clubName" label="主办社团" min-width="130" />
        <el-table-column label="活动时间" min-width="160">
          <template #default="{ row }">{{ $fmt(row.startTime) }}</template>
        </el-table-column>
        <el-table-column prop="signupType" label="报名方式" />
        <el-table-column label="报名时间" min-width="160">
          <template #default="{ row }">{{ $fmt(row.signupTime) }}</template>
        </el-table-column>
        <el-table-column label="签到状态" width="100">
          <template #default="{ row }"><StatusTag :text="row.checkinStatus || '未到'" /></template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" :disabled="Number(row.cancelFlag) === 1 || !!row.checkinStatus"
                       @click="cancel(row)">取消报名</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!rows.length" description="还没有报名任何活动" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../../api/http'
import StatusTag from '../../components/StatusTag.vue'

const rows = ref([])

async function load() {
  try { rows.value = (await http.get('/portal/signup/mine')) || [] } catch (e) { /* 提示 */ }
}

async function cancel(row) {
  await ElMessageBox.confirm('确认取消「' + row.activityName + '」的报名？', '取消报名', { type: 'warning' })
  try {
    await http.post('/activity-signup/' + row.id + '/cancel')
    ElMessage.success('已取消报名')
    load()
  } catch (e) { /* 提示 */ }
}

onMounted(load)
</script>
