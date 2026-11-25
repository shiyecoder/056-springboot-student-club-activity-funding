<template>
  <div>
    <div class="sec-h">
      <h2>活动申报<span class="sub">提交后进入团委立项审批</span></h2>
    </div>
    <div class="grid grid-2u">
      <div class="card">
        <div class="card-h"><h3>活动基本信息</h3><span class="tip">带 * 为必填</span></div>
        <el-form :model="form" label-width="96px">
          <el-form-item label="所属社团" required>
            <el-select v-model="form.clubId" placeholder="请选择社团" filterable style="width:100%">
              <el-option v-for="c in clubs" :key="c.id" :label="c.clubName" :value="c.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="活动名称" required><el-input v-model="form.activityName" maxlength="60" show-word-limit /></el-form-item>
          <el-form-item label="活动类型">
            <el-select v-model="form.activityType" style="width:100%">
              <el-option v-for="t in typeList" :key="t" :label="t" :value="t" />
            </el-select>
          </el-form-item>
          <el-form-item label="开始时间" required>
            <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" />
          </el-form-item>
          <el-form-item label="结束时间" required>
            <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" />
          </el-form-item>
          <el-form-item label="场地">
            <el-input v-model="form.placeName" placeholder="如：大学生活动中心 301" />
          </el-form-item>
          <el-form-item label="场地容量">
            <el-input-number v-model="form.placeCapacity" :min="0" :step="10" />
          </el-form-item>
          <el-form-item label="预计人数">
            <el-input-number v-model="form.expectCount" :min="0" :step="10" />
          </el-form-item>
          <el-form-item label="申请预算">
            <el-input-number v-model="form.budgetTotal" :min="0" :step="100" />
          </el-form-item>
          <el-form-item label="活动方案">
            <el-input v-model="form.planDesc" type="textarea" :rows="3" placeholder="简述活动流程、人员分工与安全预案" />
          </el-form-item>
          <el-form-item label="活动封面">
            <el-upload :action="'/api/file/upload?bizType=activity'" :headers="headers" :show-file-list="false"
                       :on-success="onUp" accept="image/*">
              <img v-if="form.coverUrl" :src="form.coverUrl" class="thumb" style="width:96px;height:72px" alt="" />
              <el-button v-else>上传封面</el-button>
            </el-upload>
          </el-form-item>
        </el-form>
        <div style="text-align:right">
          <el-button @click="reset">重置</el-button>
          <el-button type="primary" :loading="saving" @click="submit">提交申报</el-button>
        </div>
      </div>

      <div class="card">
        <div class="card-h"><h3>申报提示</h3></div>
        <ul class="tips">
          <li><b>提前 24 小时</b>：开始时间必须晚于当前时间 24 小时以上，便于审批与场地协调。</li>
          <li><b>预算额度</b>：申请预算不得超过所属社团年度核定额度的剩余部分。</li>
          <li><b>场地冲突</b>：同一场地在时间重叠区间内不允许重复立项。</li>
          <li><b>可行性评估</b>：提交后系统按预算 / 场地 / 容量 / 历史 / 社团资质五因子加权打分，团委参考结果审批。</li>
          <li><b>状态流转</b>：待审批 → 已立项 → 进行中 → 已结束 → 已结项，驳回后可修改重新提交。</li>
        </ul>
        <div class="card-h" style="margin-top:16px"><h3>最近申报</h3></div>
        <el-table :data="recent" size="small" border>
          <el-table-column prop="activityName" label="活动" min-width="140" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }"><StatusTag :text="row.status" /></template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../../api/http'
import StatusTag from '../../components/StatusTag.vue'

const clubs = ref([])
const recent = ref([])
const saving = ref(false)
const typeList = ['讲座沙龙', '竞赛比赛', '志愿服务', '文艺演出', '社会实践', '招新宣传']
const headers = { Authorization: 'Bearer ' + (localStorage.getItem('cfs_token') || '') }
const empty = () => ({
  clubId: null, activityName: '', activityType: '讲座沙龙', startTime: '', endTime: '',
  placeName: '', placeCapacity: 100, expectCount: 80, budgetTotal: 0, planDesc: '', coverUrl: ''
})
const form = reactive(empty())

function onUp(res) {
  if (res && res.code === 200 && res.data) form.coverUrl = res.data.url
}
function reset() { Object.assign(form, empty()) }

async function submit() {
  if (!form.clubId || !form.activityName) return ElMessage.warning('请填写社团与活动名称')
  if (!form.startTime || !form.endTime) return ElMessage.warning('请选择活动起止时间')
  saving.value = true
  try {
    await http.post('/activity', form)
    ElMessage.success('申报已提交，等待团委立项审批')
    reset()
    loadRecent()
  } finally { saving.value = false }
}

async function loadRecent() {
  try {
    const d = await http.get('/activity/page', { params: { page: 1, size: 5 } })
    recent.value = d.records || []
  } catch (e) { /* 忽略 */ }
}

onMounted(async () => {
  try { clubs.value = await http.get('/club/options') } catch (e) { /* 忽略 */ }
  loadRecent()
})
</script>

<style scoped>
.tips { padding-left: 18px; margin: 0; color: var(--text-2); font-size: 12px; line-height: 2; }
.tips b { color: var(--primary); }
</style>
