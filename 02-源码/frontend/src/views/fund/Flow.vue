<template>
  <div>
    <div class="sec-h">
      <h2>经费流水<span class="sub">收入与支出逐笔留痕</span></h2>
    </div>

    <div class="grid grid-4" style="margin-bottom:12px">
      <div v-for="s in stats" :key="s.label" class="card" style="margin:0;padding:12px 14px">
        <div class="tip">{{ s.label }}</div>
        <div class="num" :style="{ color: s.color }">
          {{ s.plain ? s.value : money(s.value) }}<em>{{ s.unit }}</em>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="filter-bar">
        <el-select v-model="q.clubId" placeholder="所属社团" clearable filterable>
          <el-option v-for="c in clubs" :key="c.id" :label="c.clubName" :value="c.id" />
        </el-select>
        <el-select v-model="q.direction" placeholder="收支方向" clearable>
          <el-option label="收入" value="收入" />
          <el-option label="支出" value="支出" />
        </el-select>
        <el-select v-model="q.bizType" placeholder="业务类型" clearable>
          <el-option v-for="t in bizTypes" :key="t" :label="t" :value="t" />
        </el-select>
        <el-button type="primary" @click="load(1)">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" border>
        <el-table-column prop="flowNo" label="流水号" min-width="120" />
        <el-table-column prop="clubName" label="社团" min-width="130" />
        <el-table-column prop="bizType" label="业务类型" />
        <el-table-column label="方向">
          <template #default="{ row }"><StatusTag :text="row.direction" /></template>
        </el-table-column>
        <el-table-column label="金额" align="right">
          <template #default="{ row }">
            <span class="mono" :style="{ color: row.direction === '支出' ? '#B02A37' : '#2B7955', fontWeight: 600 }">
              {{ row.direction === '支出' ? '-' : '+' }}{{ money(row.amount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="余额" align="right">
          <template #default="{ row }"><span class="mono">{{ money(row.balanceAfter) }}</span></template>
        </el-table-column>
        <el-table-column label="发生日期">
          <template #default="{ row }">{{ $fmtDate(row.occurDate) }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="摘要" min-width="160">
          <template #default="{ row }"><span class="tip">{{ row.remark || '—' }}</span></template>
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
import { computed, onMounted, reactive, ref } from 'vue'
import http from '../../api/http'
import StatusTag from '../../components/StatusTag.vue'
import { money } from '../../utils/format'

const rows = ref([])
const clubs = ref([])
const sum = ref({})
const total = ref(0)
const loading = ref(false)
// 业务类型选项由接口的 typeDistribution 派生，避免写死臆测值（学校拨款/会费收入/赞助收入/各类费用）
const bizTypes = ref([])
const q = reactive({ page: 1, size: 10, clubId: null, direction: '', bizType: '' })

const stats = computed(() => [
  { label: '累计收入', value: sum.value.incomeTotal, unit: '元', color: '#2B7955' },
  { label: '累计支出', value: sum.value.expenseTotal, unit: '元', color: '#B02A37' },
  { label: '当前余额', value: sum.value.balance, unit: '元', color: '#1F2933' },
  { label: '流水笔数', value: total.value, unit: '笔', plain: true, color: '#8C2F39' }
])

async function loadSum() {
  const s = await http.get('/fund-flow/summary', { params: q.clubId ? { clubId: q.clubId } : {} })
  sum.value = s || {}
  const names = (s?.typeDistribution || []).map((t) => t.name).filter(Boolean)
  if (names.length) bizTypes.value = names.sort()
}

async function load(p) {
  if (p) q.page = p
  loading.value = true
  try {
    const d = await http.get('/fund-flow/page', { params: q })
    rows.value = d.records || []
    total.value = d.total || 0
    // 统计卡跟随社团筛选，避免「表按社团过滤、卡还是全局」的口径不一致
    await loadSum()
  } finally { loading.value = false }
}
function reset() { q.clubId = null; q.direction = ''; q.bizType = ''; load(1) }

onMounted(async () => {
  try { clubs.value = await http.get('/club/options') } catch (e) { /* 忽略 */ }
  load()
})
</script>

<style scoped>
.num { font-size: 20px; font-weight: 700; margin-top: 4px; font-variant-numeric: tabular-nums; }
.num em { font-style: normal; font-size: 11px; color: var(--text-3); margin-left: 3px; }
</style>
