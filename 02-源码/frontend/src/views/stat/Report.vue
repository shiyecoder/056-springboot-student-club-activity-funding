<template>
  <div>
    <div class="sec-h">
      <h2>统计报表<span class="sub">按时间区间汇总社团活动与经费</span></h2>
      <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD"
                      start-placeholder="开始日期" end-placeholder="结束日期" @change="load" />
    </div>

    <div class="grid grid-5" style="margin-bottom:12px">
      <div v-for="s in cards" :key="s.label" class="card kpi" style="margin:0">
        <div class="tip">{{ s.label }}</div>
        <div class="num">{{ s.value }}<em v-if="s.unit">{{ s.unit }}</em></div>
        <div class="tip" style="margin-top:2px">{{ s.hint }}</div>
      </div>
    </div>

    <div class="grid grid-2">
      <div class="card">
        <div class="card-h"><h3>近 6 个月活动与参与人次</h3></div>
        <Chart :option="optTrend" :height="250" />
        <div class="cal-note">{{ caliber }}</div>
      </div>
      <div class="card">
        <div class="card-h"><h3>经费支出结构</h3></div>
        <Chart :option="optExpense" :height="250" />
        <div class="cal-note">
          按经费流水的业务类型汇总：支出侧细分场地费 / 物料费 / 宣传费 / 交通费 / 劳务费，收入侧为学校拨款 / 会费 / 赞助。
        </div>
      </div>
    </div>

    <div class="grid grid-2">
      <div class="card">
        <div class="card-h"><h3>社团状态分布</h3></div>
        <el-table :data="clubStatus" size="small" border>
          <el-table-column prop="name" label="状态" />
          <el-table-column prop="value" label="数量" align="right" />
          <el-table-column label="占比" align="right">
            <template #default="{ row }">{{ pct(row.value, clubTotal) }}%</template>
          </el-table-column>
        </el-table>
      </div>
      <div class="card">
        <div class="card-h"><h3>经费公示状态分布</h3></div>
        <el-table :data="disStatus" size="small" border>
          <el-table-column prop="name" label="状态" />
          <el-table-column prop="value" label="数量" align="right" />
          <el-table-column label="占比" align="right">
            <template #default="{ row }">{{ pct(row.value, disTotal) }}%</template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import http from '../../api/http'
import Chart from '../../components/Chart.vue'
import { money } from '../../utils/format'

const data = ref({})
const range = ref([])
const overview = computed(() => data.value.overview || {})
const charts = computed(() => data.value.charts || {})
const caliber = computed(() => {
  const c = data.value.caliber
  if (!c || typeof c !== 'object') return '统计口径：全部在册数据'
  return `统计区间 ${c.dateRange}；签到率口径：${c.attendRate}；经费执行率口径：${c.fundExecRate}`
})
const clubStatus = computed(() => data.value.clubStatus || [])
const disStatus = computed(() => data.value.disclosureStatus || [])
const clubTotal = computed(() => clubStatus.value.reduce((a, b) => a + Number(b.value || 0), 0) || 1)
const disTotal = computed(() => disStatus.value.reduce((a, b) => a + Number(b.value || 0), 0) || 1)

const cards = computed(() => [
  { label: '社团总数', value: overview.value.clubTotal || 0, unit: '个', hint: '不含已注销' },
  { label: '在册成员', value: overview.value.memberTotal || 0, unit: '人', hint: '状态为在册' },
  { label: '活动场次', value: overview.value.monthActivity || 0, unit: '场', hint: '区间内活动' },
  { label: '平均签到率', value: overview.value.attendRate || 0, unit: '%', hint: '已结束活动' },
  { label: '经费执行率', value: overview.value.fundExecRate || 0, unit: '%', hint: '支出 / 额度' }
])

function pct(v, t) { return Math.round((Number(v || 0) * 1000) / t) / 10 }

const optTrend = computed(() => {
  const d = charts.value.monthlyTrend || []
  return {
    tooltip: { trigger: 'axis' },
    legend: { right: 0, top: 0, itemWidth: 8, itemHeight: 8, textStyle: { fontSize: 11 } },
    grid: { left: 40, right: 40, top: 30, bottom: 24 },
    xAxis: { type: 'category', data: d.map((x) => x.month), axisLine: { lineStyle: { color: '#E3E6E8' } }, axisLabel: { fontSize: 11, color: '#98A2AB' } },
    yAxis: [
      { type: 'value', splitLine: { lineStyle: { color: '#F0F2F3' } }, axisLabel: { fontSize: 11, color: '#98A2AB' } },
      { type: 'value', splitLine: { show: false }, axisLabel: { fontSize: 11, color: '#98A2AB' } }
    ],
    series: [
      { name: '活动场次', type: 'bar', barWidth: 18, itemStyle: { borderRadius: [3, 3, 0, 0] }, data: d.map((x) => x.activity) },
      { name: '参与人次', type: 'line', yAxisIndex: 1, smooth: true, symbolSize: 5, data: d.map((x) => x.people) }
    ]
  }
})

const optExpense = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}<br/>{c} 元 ({d}%)' },
  legend: { orient: 'vertical', right: 0, top: 'center', itemWidth: 8, itemHeight: 8, textStyle: { fontSize: 11 } },
  series: [{
    type: 'pie', radius: '62%', center: ['36%', '50%'], label: { show: false },
    data: (charts.value.expenseStructure || []).map((d) => ({ name: d.name, value: Number(d.value) }))
  }]
}))

async function load() {
  const params = {}
  if (range.value && range.value.length === 2) {
    params.startDate = range.value[0]
    params.endDate = range.value[1]
  }
  try { data.value = await http.get('/stat/report', { params }) } catch (e) { /* 提示 */ }
}

onMounted(load)
</script>

<style scoped>
.grid-5 { display: grid; grid-template-columns: repeat(5, 1fr); gap: 12px; }
.kpi { padding: 12px 14px; }
.kpi .num { font-size: 20px; font-weight: 700; margin-top: 4px; font-variant-numeric: tabular-nums; }
.kpi .num em { font-style: normal; font-size: 11px; color: var(--text-3); margin-left: 2px; }
/* 统计口径说明较长，放图表下方做脚注，不要挤在标题行（否则会被压成竖排小字） */
.cal-note { margin-top: 8px; font-size: 11px; line-height: 1.7; color: var(--text-3); }
</style>
