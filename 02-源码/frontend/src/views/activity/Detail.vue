<template>
  <div v-if="a" class="detail">
    <div class="sec-h">
      <h2>
        {{ a.activityName }}
        <span class="sub">{{ a.activityNo }}</span>
      </h2>
      <div>
        <StatusTag :text="a.status" />
        <el-button style="margin-left:10px" @click="$router.back()">返回</el-button>
        <el-button v-if="canFlow" type="primary" @click="openFlow">状态流转</el-button>
      </div>
    </div>

    <div class="grid grid-2u">
      <div class="card">
        <div class="card-h"><h3>活动信息</h3></div>
        <div class="cover">
          <img v-if="a.coverUrl" :src="a.coverUrl" alt="" />
          <div v-else class="img-empty" style="width:100%;height:100%"></div>
        </div>
        <el-descriptions :column="2" border size="small" style="margin-top:12px">
          <el-descriptions-item label="主办社团">{{ a.clubName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="活动类型">{{ a.activityType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ $fmt(a.startTime) }}</el-descriptions-item>
          <el-descriptions-item label="结束时间">{{ $fmt(a.endTime) }}</el-descriptions-item>
          <el-descriptions-item label="场地">{{ a.placeName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="场地容量">{{ a.placeCapacity || '-' }}</el-descriptions-item>
          <el-descriptions-item label="预计 / 实到">
            {{ a.expectCount || 0 }} / {{ a.actualCount || 0 }}
          </el-descriptions-item>
          <el-descriptions-item label="申请预算">{{ money(a.budgetTotal) }} 元</el-descriptions-item>
          <el-descriptions-item label="已支出">{{ money(a.spentAmount) }} 元</el-descriptions-item>
          <el-descriptions-item label="可行性得分">{{ a.feasibilityScore || '-' }}</el-descriptions-item>
          <el-descriptions-item label="活动方案" :span="2">{{ a.planDesc || '—' }}</el-descriptions-item>
          <el-descriptions-item v-if="a.rejectReason" label="驳回原因" :span="2">
            <span style="color:var(--danger)">{{ a.rejectReason }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <div>
        <div class="card">
          <div class="card-h"><h3>可行性评估</h3><span class="tip" v-if="f">规则 v{{ f.ruleVersion || '-' }}</span></div>
          <div v-if="f" class="eval">
            <div class="eval-score">
              <span class="num">{{ f.totalScore }}</span>
              <div><StatusTag :text="f.level" /><div class="tip" style="margin-top:4px">{{ f.suggestion }}</div></div>
            </div>
            <div class="mini-bars">
              <div v-for="x in miniFactors" :key="x.name" class="mini">
                <span>{{ x.name }}</span>
                <div class="bar"><i :style="{ width: x.pct + '%' }"></i></div>
                <b>{{ x.score }}</b>
              </div>
            </div>
          </div>
          <el-empty v-else :image-size="60" description="暂无评估" />
        </div>

        <div class="card">
          <div class="card-h">
            <h3>报名与签到</h3>
            <span class="tip">报名 {{ signupTotal }} · 签到 {{ signedTotal }}</span>
          </div>
          <el-table :data="signups" size="small" border max-height="260">
            <el-table-column prop="studentName" label="姓名" />
            <el-table-column prop="signupNo" label="报名单号" min-width="130" />
            <el-table-column label="签到">
              <template #default="{ row }"><StatusTag :text="row.checkinStatus || '未到'" /></template>
            </el-table-column>
          </el-table>
        </div>

        <div class="card" v-if="budgets.length">
          <div class="card-h"><h3>预算明细</h3><span class="tip">{{ budgets.length }} 项</span></div>
          <el-table :data="budgets" size="small" border max-height="220">
            <el-table-column prop="itemName" label="预算科目" min-width="120" />
            <el-table-column label="申请" align="right">
              <template #default="{ row }">{{ money(row.applyAmount) }}</template>
            </el-table-column>
            <el-table-column label="核定" align="right">
              <template #default="{ row }">{{ money(row.approveAmount) }}</template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>

    <el-dialog v-model="dlg" title="活动状态流转" width="460px">
      <el-form label-width="80px">
        <el-form-item label="目标状态">
          <el-select v-model="flow.action" placeholder="请选择下一步">
            <el-option v-for="x in actions" :key="x" :label="x" :value="x" />
          </el-select>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="flow.remark" type="textarea" :rows="2" placeholder="可填写流转说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="doFlow">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../../api/http'
import StatusTag from '../../components/StatusTag.vue'
import { money } from '../../utils/format'

const route = useRoute()
const a = ref(null)
const f = ref(null)
const signups = ref([])
const budgets = ref([])
const signupTotal = ref(0)
const signedTotal = ref(0)
const dlg = ref(false)
const flow = reactive({ action: '', remark: '' })

const canFlow = computed(() => a.value && ['待审批', '已立项', '进行中', '已结束'].includes(a.value.status))
const actions = computed(() => (a.value && a.value.actions) || ['START', 'FINISH', 'CLOSE'])

const miniFactors = computed(() => {
  if (!f.value) return []
  return [
    { name: '预算', key: 'scoreBudget', score: Number(f.value.scoreBudget || 0) },
    { name: '场地', key: 'scoreVenue', score: Number(f.value.scoreVenue || 0) },
    { name: '容量', key: 'scoreCapacity', score: Number(f.value.scoreCapacity || 0) },
    { name: '历史', key: 'scoreHistory', score: Number(f.value.scoreHistory || 0) },
    { name: '资质', key: 'scoreClub', score: Number(f.value.scoreClub || 0) }
  ].map((x) => ({ ...x, pct: Math.min(100, Math.round(x.score)) }))
})

async function load() {
  const id = route.params.id
  try {
    const d = await http.get('/activity/' + id)
    a.value = d
    f.value = d.feasibility || null
    signups.value = d.signupList || []
    budgets.value = d.budgets || []
    signupTotal.value = d.signupTotal || 0
    signedTotal.value = d.signedTotal || 0
  } catch (e) { /* 提示 */ }
}

function openFlow() {
  flow.action = ''
  flow.remark = ''
  dlg.value = true
}

async function doFlow() {
  if (!flow.action) return ElMessage.warning('请选择目标状态')
  try {
    await http.post('/activity/' + a.value.id + '/transition', { action: flow.action, remark: flow.remark })
    ElMessage.success('状态已流转')
    dlg.value = false
    load()
  } catch (e) { /* 提示 */ }
}

onMounted(load)
</script>

<style scoped>
.cover { height: 180px; border-radius: 6px; overflow: hidden; background: var(--bg); }
.cover img { width: 100%; height: 100%; object-fit: cover; display: block; }
.eval-score { display: flex; align-items: center; gap: 14px; margin-bottom: 12px; }
.eval-score .num { font-size: 34px; font-weight: 700; color: var(--primary); line-height: 1; }
.mini-bars { display: flex; flex-direction: column; gap: 8px; }
.mini { display: grid; grid-template-columns: 40px 1fr 34px; align-items: center; gap: 8px; font-size: 11.5px; }
.mini .bar { height: 7px; border-radius: 4px; background: #F1F2F3; overflow: hidden; }
.mini .bar i { display: block; height: 100%; background: linear-gradient(90deg, #8C2F39, #C25B63); }
.mini b { text-align: right; font-variant-numeric: tabular-nums; }
</style>
