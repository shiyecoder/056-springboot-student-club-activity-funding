<template>
  <div>
    <div class="sec-h">
      <h2>立项审批台<span class="sub">待审批 {{ total }} 项</span></h2>
      <el-button @click="load(1)">刷新</el-button>
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
      <el-table-column label="起止时间" min-width="180">
        <template #default="{ row }">
          <div>{{ $fmt(row.startTime) }}</div>
          <div class="tip">至 {{ $fmt(row.endTime) }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="placeName" label="场地" />
      <el-table-column label="预算" align="right">
        <template #default="{ row }"><span class="mono">{{ money(row.budgetTotal) }}</span></template>
      </el-table-column>
      <el-table-column label="可行性得分" align="center" width="110">
        <template #default="{ row }">
          <span :style="{ fontWeight: 600, color: scoreColor(row.feasibilityScore) }">
            {{ row.feasibilityScore || '-' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="状态">
        <template #default="{ row }"><StatusTag :text="row.status" /></template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEval(row)">评估</el-button>
          <el-button link type="success" @click="approve(row)">通过</el-button>
          <el-button link type="danger" @click="openReject(row)">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination background layout="total, prev, pager, next" :total="total"
                     v-model:current-page="q.page" :page-size="q.size" @current-change="load()" />
    </div>

    <!-- 创新点：可行性评估明细 + 逐项得分 + 未满足条件 + 人工修正留痕 -->
    <el-drawer v-model="drw" title="可行性评估明细" size="620px">
      <div v-if="feas">
        <div class="score-head">
          <div class="score-num">{{ feas.totalScore }}</div>
          <div>
            <StatusTag :text="feas.level" />
            <div class="tip" style="margin-top:6px">{{ feas.suggestion }}</div>
          </div>
        </div>
        <div class="bars">
          <div v-for="f in factors" :key="f.name" class="bar-row">
            <div class="bar-lab">
              <span>{{ f.name }}</span>
              <span class="tip">权重 {{ f.weight }}% · 得分 {{ f.score }}</span>
            </div>
            <div class="bar"><i :style="{ width: f.pct + '%' }"></i></div>
          </div>
        </div>
        <div v-if="unsat.length" class="unsat">
          <div class="card-h"><h3>未满足条件</h3></div>
          <el-tag v-for="(u, i) in unsat" :key="i" type="danger" effect="plain" style="margin:0 6px 6px 0">{{ u }}</el-tag>
        </div>
        <div class="tip" style="margin:12px 0 6px">
          规则版本 {{ feas.ruleVersion || '-' }} · 计算时间 {{ $fmt(feas.calcTime) }}
        </div>
        <el-divider />
        <div class="card-h"><h3>人工修正</h3><span class="tip">修正会写入留痕，可追溯</span></div>
        <el-form label-width="80px">
          <el-form-item label="修正后等级">
            <el-select v-model="correct.level" placeholder="如需调整请选择">
              <el-option label="推荐立项" value="推荐立项" />
              <el-option label="可行" value="可行" />
              <el-option label="谨慎" value="谨慎" />
              <el-option label="不建议" value="不建议" />
            </el-select>
          </el-form-item>
          <el-form-item label="修正理由">
            <el-input v-model="correct.reason" type="textarea" :rows="2" placeholder="请填写修正理由，将记录到留痕" />
          </el-form-item>
          <el-form-item>
            <el-button @click="recalc">重新计算</el-button>
            <el-button type="primary" @click="doCorrect">提交修正</el-button>
          </el-form-item>
        </el-form>
      </div>
      <el-empty v-else description="暂无评估结果" />
    </el-drawer>

    <el-dialog v-model="dlgReject" title="驳回活动申报" width="460px">
      <el-input v-model="rejectReason" type="textarea" :rows="3" placeholder="请填写驳回原因，不少于 5 个字" />
      <template #footer>
        <el-button @click="dlgReject = false">取消</el-button>
        <el-button type="danger" @click="doReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../../api/http'
import StatusTag from '../../components/StatusTag.vue'
import { money } from '../../utils/format'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const drw = ref(false)
const dlgReject = ref(false)
const feas = ref(null)
const cur = ref({})
const rejectReason = ref('')
const q = reactive({ page: 1, size: 10, status: '待审批' })
const correct = reactive({ level: '', reason: '' })

// 五因子权重与得分映射（与后端 FeasibilityService 一致）
const factors = computed(() => {
  if (!feas.value) return []
  const list = [
    { name: '预算合理性', key: 'scoreBudget', weight: 25 },
    { name: '场地可行性', key: 'scoreVenue', weight: 20 },
    { name: '容量匹配度', key: 'scoreCapacity', weight: 20 },
    { name: '历史执行', key: 'scoreHistory', weight: 20 },
    { name: '社团资质', key: 'scoreClub', weight: 15 }
  ]
  return list.map((f) => {
    const s = Number(feas.value[f.key] || 0)
    return { name: f.name, weight: f.weight, score: s, pct: Math.min(100, Math.round(s)) }
  })
})
const unsat = computed(() => {
  const u = feas.value && feas.value.unsatisfied
  if (!u) return []
  if (Array.isArray(u)) return u
  return String(u).split(/[;；,，]/).filter((x) => x.trim())
})

function scoreColor(s) {
  const n = Number(s || 0)
  if (n >= 80) return '#2B7955'
  if (n >= 60) return '#E8B04B'
  return '#B02A37'
}

async function load(p) {
  if (p) q.page = p
  loading.value = true
  try {
    const d = await http.get('/activity/page', { params: q })
    rows.value = d.records || []
    total.value = d.total || 0
  } finally { loading.value = false }
}

async function openEval(row) {
  cur.value = row
  correct.level = ''
  correct.reason = ''
  feas.value = null
  drw.value = true
  try { feas.value = await http.get('/activity/' + row.id + '/feasibility') } catch (e) { /* 提示 */ }
}

async function recalc() {
  try {
    feas.value = await http.post('/activity/' + cur.value.id + '/feasibility')
    ElMessage.success('已重新计算')
    load()
  } catch (e) { /* 提示 */ }
}

async function doCorrect() {
  if (!correct.level) return ElMessage.warning('请选择修正后的等级')
  if (!correct.reason || correct.reason.trim().length < 5) return ElMessage.warning('修正理由不少于 5 个字')
  await http.post('/activity/' + cur.value.id + '/feasibility/correct', {
    level: correct.level, reason: correct.reason
  })
  ElMessage.success('修正已留痕')
  drw.value = false
  load()
}

async function approve(row) {
  try {
    await http.post('/activity/' + row.id + '/approve', { result: 'PASS' })
    ElMessage.success('已通过立项')
    load()
  } catch (e) { /* 提示 */ }
}

function openReject(row) {
  cur.value = row
  rejectReason.value = ''
  dlgReject.value = true
}

async function doReject() {
  if (!rejectReason.value || rejectReason.value.trim().length < 5) return ElMessage.warning('驳回原因不少于 5 个字')
  try {
    await http.post('/activity/' + cur.value.id + '/approve', { result: 'REJECT', reason: rejectReason.value })
    ElMessage.success('已驳回')
    dlgReject.value = false
    load()
  } catch (e) { /* 提示 */ }
}

onMounted(() => load())
</script>

<style scoped>
.score-head { display: flex; align-items: center; gap: 16px; margin-bottom: 18px; }
.score-num {
  font-size: 40px; font-weight: 700; color: var(--primary);
  font-variant-numeric: tabular-nums; line-height: 1;
}
.bars { display: flex; flex-direction: column; gap: 12px; }
.bar-lab { display: flex; justify-content: space-between; font-size: 12px; margin-bottom: 5px; }
.bar { height: 8px; border-radius: 4px; background: #F1F2F3; overflow: hidden; }
.bar i { display: block; height: 100%; border-radius: 4px; background: linear-gradient(90deg, #8C2F39, #C25B63); }
.unsat { margin-top: 18px; }
</style>
