<template>
  <div class="dash">
    <div class="kpis">
      <div v-for="k in kpis" :key="k.label" class="kpi">
        <div class="lab">{{ k.label }}</div>
        <div class="val">{{ k.value }}<em v-if="k.unit">{{ k.unit }}</em></div>
        <div class="hint">{{ k.hint }}</div>
      </div>
    </div>

    <div class="charts">
      <div class="card c">
        <div class="card-h"><h3>社团类别占比</h3><span class="tip">在册 {{ totalClub }} 个</span></div>
        <Chart :option="optClubType" :height="200" />
      </div>
      <div class="card c">
        <div class="card-h"><h3>近 6 个月活动与参与人次</h3><span class="tip">按月统计</span></div>
        <Chart :option="optTrend" :height="200" />
      </div>
      <div class="card c">
        <div class="card-h"><h3>经费支出结构</h3><span class="tip">按业务类型</span></div>
        <Chart :option="optExpense" :height="200" />
      </div>
      <div class="card c">
        <div class="card-h"><h3>社团预算执行率 Top8</h3><span class="tip">已用额度 / 核定额度</span></div>
        <Chart :option="optExec" :height="200" />
      </div>
      <div class="card c">
        <div class="card-h"><h3>报名 → 签到转化漏斗</h3><span class="tip">全量报名单</span></div>
        <Chart :option="optFunnel" :height="200" />
      </div>
      <div class="card c">
        <div class="card-h"><h3>活动可行性等级分布</h3><span class="tip">五因子加权评估结果</span></div>
        <Chart :option="optLevel" :height="200" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import http from '../api/http'
import Chart from '../components/Chart.vue'
import { money } from '../utils/format'

const ov = ref({})
const ch = ref({})
const totalClub = computed(() => ov.value.clubTotal || 0)

const kpis = computed(() => [
  { label: '在册社团', value: ov.value.clubTotal || 0, unit: '个', hint: '不含已注销' },
  { label: '在册成员', value: ov.value.memberTotal || 0, unit: '人', hint: '状态为在册' },
  { label: '本月活动', value: ov.value.monthActivity || 0, unit: '场', hint: '按开始时间统计' },
  { label: '待审批活动', value: ov.value.pendingActivity || 0, unit: '项', hint: '需团委立项' },
  { label: '平均签到率', value: ov.value.attendRate || 0, unit: '%', hint: '已结束活动' },
  { label: '经费执行率', value: ov.value.fundExecRate || 0, unit: '%', hint: '支出 / 核定额度' },
  { label: '待付报销', value: money(ov.value.pendingExpense), unit: '元', hint: '待初审+待复核' },
  { label: '待答复异议', value: ov.value.objectionCount || 0, unit: '条', hint: '公示期异议' },
  { label: '核定经费', value: money(ov.value.fundLimitTotal), unit: '元', hint: '全社团合计' },
  { label: '累计支出', value: money(ov.value.fundSpentTotal), unit: '元', hint: '全部支出流水' }
])

const optClubType = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: { orient: 'vertical', right: 4, top: 'center', itemWidth: 8, itemHeight: 8, textStyle: { fontSize: 11 } },
  series: [{
    type: 'pie', radius: ['46%', '70%'], center: ['36%', '50%'],
    label: { show: false },
    data: (ch.value.clubType || []).map((d) => ({ name: d.name, value: d.value }))
  }]
}))

const optTrend = computed(() => {
  const d = ch.value.monthlyTrend || []
  return {
    tooltip: { trigger: 'axis' },
    legend: { right: 0, top: 0, itemWidth: 8, itemHeight: 8, textStyle: { fontSize: 11 } },
    grid: { left: 34, right: 34, top: 30, bottom: 22 },
    xAxis: { type: 'category', data: d.map((x) => x.month), axisLine: { lineStyle: { color: '#E3E6E8' } }, axisLabel: { fontSize: 11, color: '#98A2AB' } },
    yAxis: [
      { type: 'value', splitLine: { lineStyle: { color: '#F0F2F3' } }, axisLabel: { fontSize: 11, color: '#98A2AB' } },
      { type: 'value', splitLine: { show: false }, axisLabel: { fontSize: 11, color: '#98A2AB' } }
    ],
    series: [
      { name: '活动场次', type: 'bar', barWidth: 16, itemStyle: { borderRadius: [3, 3, 0, 0] }, data: d.map((x) => x.activity) },
      { name: '参与人次', type: 'line', yAxisIndex: 1, smooth: true, symbolSize: 5, lineStyle: { width: 2 }, data: d.map((x) => x.people) }
    ]
  }
})

const optExpense = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}<br/>{c} 元 ({d}%)' },
  legend: { orient: 'vertical', right: 0, top: 'center', itemWidth: 8, itemHeight: 8, textStyle: { fontSize: 11 } },
  series: [{
    type: 'pie', radius: '62%', center: ['34%', '50%'],
    label: { show: false },
    data: (ch.value.expenseStructure || []).map((d) => ({ name: d.name, value: Number(d.value) }))
  }]
}))

const optExec = computed(() => {
  const d = (ch.value.clubExecRate || []).slice(0, 8).reverse()
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 78, right: 42, top: 12, bottom: 8 },
    xAxis: { type: 'value', max: 100, splitLine: { lineStyle: { color: '#F0F2F3' } }, axisLabel: { fontSize: 10, color: '#98A2AB' } },
    yAxis: { type: 'category', data: d.map((x) => x.name), axisLine: { lineStyle: { color: '#E3E6E8' } }, axisLabel: { fontSize: 10.5, color: '#5B6770' } },
    series: [{
      type: 'bar', barWidth: 10, itemStyle: { borderRadius: [0, 3, 3, 0] },
      label: { show: true, position: 'right', fontSize: 10, color: '#5B6770', formatter: '{c}%' },
      data: d.map((x) => x.value)
    }]
  }
})

const optFunnel = computed(() => {
  const d = ch.value.funnel || []
  return {
    tooltip: { trigger: 'item' },
    series: [{
      type: 'funnel', left: '12%', right: '12%', top: 8, bottom: 8, minSize: '32%',
      label: { fontSize: 11, color: '#5B6770', position: 'inside' },
      itemStyle: { borderWidth: 0 },
      data: d.map((x) => ({ name: x.name, value: x.value }))
    }]
  }
})

const LEVEL_CN = { A: '推荐立项', B: '可行', C: '谨慎', D: '不建议' }

const optLevel = computed(() => {
  // 后端等级枚举为 A/B/C/D，图表标签统一转中文
  const d = (ch.value.feasibilityLevel || []).map((x) => ({ name: LEVEL_CN[x.name] || x.name, value: x.value }))
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 40, right: 16, top: 16, bottom: 24 },
    xAxis: { type: 'category', data: d.map((x) => x.name), axisLine: { lineStyle: { color: '#E3E6E8' } }, axisLabel: { fontSize: 10.5, color: '#5B6770' } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#F0F2F3' } }, axisLabel: { fontSize: 10.5, color: '#98A2AB' } },
    series: [{
      type: 'bar', barWidth: 26, itemStyle: { borderRadius: [4, 4, 0, 0] },
      label: { show: true, position: 'top', fontSize: 10.5, color: '#5B6770' },
      data: d.map((x) => x.value)
    }]
  }
})

onMounted(async () => {
  try { ov.value = await http.get('/dashboard/overview') } catch (e) { /* 拦截器提示 */ }
  try { ch.value = await http.get('/dashboard/charts') } catch (e) { /* 拦截器提示 */ }
})
</script>

<style scoped>
/* 看板必须一屏：1440x900 下可用高度 = 900 - 56(顶栏) - 36(内容内边距) */
.dash { display: flex; flex-direction: column; gap: 12px; }
.kpis { display: grid; grid-template-columns: repeat(5, 1fr); gap: 10px; }
.kpi {
  background: #fff; border-radius: 8px; box-shadow: var(--shadow);
  padding: 9px 12px 10px; border-left: 3px solid var(--primary-soft);
}
.kpi:nth-child(1), .kpi:nth-child(6) { border-left-color: var(--primary); }
.kpi:nth-child(2), .kpi:nth-child(7) { border-left-color: var(--second); }
.kpi:nth-child(3), .kpi:nth-child(8) { border-left-color: var(--accent); }
.kpi .lab { font-size: 11px; color: var(--text-3); margin-bottom: 3px; }
.kpi .val { font-size: 19px; font-weight: 700; line-height: 1.15; font-variant-numeric: tabular-nums; color: var(--text); }
.kpi .val em { font-style: normal; font-size: 11px; font-weight: 400; color: var(--text-3); margin-left: 2px; }
.kpi .hint { font-size: 10.5px; color: var(--text-3); margin-top: 2px; }

.charts { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.c { margin-bottom: 0; padding: 12px 14px 8px; }
.c .card-h { margin-bottom: 4px; }
.c .card-h h3 { font-size: 12.5px; }
.c .tip { font-size: 10.5px; }
</style>
