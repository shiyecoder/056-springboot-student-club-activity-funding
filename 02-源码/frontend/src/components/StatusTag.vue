<template>
  <span class="st" :style="{ color: c.fg, background: c.bg }">{{ label }}</span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({ text: { type: String, default: '' } })

/**
 * 状态字典：覆盖社团、活动、预算、报销、公示、异议、审批全量枚举，
 * 避免看板/表格里露出后端英文枚举（NEW-037 踩过的坑）。
 */
const MAP = {
  // 社团
  '正常': ['#2B7955', '#EAF5EF'], '预警': ['#B02A37', '#FBEDEF'], '已注销': ['#98A2AB', '#F2F3F4'],
  '待注册': ['#3A6EA5', '#EDF3FA'], '整改中': ['#E8B04B', '#FDF6E7'],
  '待年审': ['#3A6EA5', '#EDF3FA'], '年审中': ['#3A6EA5', '#EDF3FA'],
  // 通用审批
  '待审批': ['#3A6EA5', '#EDF3FA'], '已通过': ['#2B7955', '#EAF5EF'], '已驳回': ['#B02A37', '#FBEDEF'],
  '已立项': ['#8C2F39', '#F6E9EA'], '进行中': ['#C25B63', '#FBEEF0'], '已结束': ['#6B7A85', '#F1F2F3'],
  '已结项': ['#2B7955', '#EAF5EF'], '已取消': ['#98A2AB', '#F2F3F4'], '草稿': ['#98A2AB', '#F2F3F4'],
  // 报名/签到
  '待审核': ['#3A6EA5', '#EDF3FA'], '已报名': ['#3A6EA5', '#EDF3FA'], '已签到': ['#2B7955', '#EAF5EF'],
  '迟到': ['#E8B04B', '#FDF6E7'], '未到': ['#B02A37', '#FBEDEF'], '已取消': ['#98A2AB', '#F2F3F4'],
  '未签到': ['#B02A37', '#FBEDEF'], '已请假': ['#98A2AB', '#F2F3F4'], '缺席': ['#B02A37', '#FBEDEF'],
  '在册': ['#2B7955', '#EAF5EF'], '已退出': ['#98A2AB', '#F2F3F4'], '已除名': ['#B02A37', '#FBEDEF'],
  // 经费
  '待初审': ['#3A6EA5', '#EDF3FA'], '待财务复核': ['#C25B63', '#FBEEF0'], '已支付': ['#2B7955', '#EAF5EF'],
  '已入账': ['#2B7955', '#EAF5EF'], '已冲正': ['#6B7A85', '#F1F2F3'], '待答复': ['#B02A37', '#FBEDEF'],
  '待审核': ['#3A6EA5', '#EDF3FA'], '已调减': ['#E8B04B', '#FDF6E7'],
  '待公示': ['#3A6EA5', '#EDF3FA'], '公示中': ['#C25B63', '#FBEEF0'], '已归档': ['#6B7A85', '#F1F2F3'],
  '已答复': ['#2B7955', '#EAF5EF'], '已发布': ['#2B7955', '#EAF5EF'], '未发布': ['#98A2AB', '#F2F3F4'],
  '待发布': ['#3A6EA5', '#EDF3FA'],
  // 可行性
  '推荐立项': ['#2B7955', '#EAF5EF'], '可行': ['#2B7955', '#EAF5EF'], '谨慎': ['#E8B04B', '#FDF6E7'],
  '不建议': ['#B02A37', '#FBEDEF'],
  // 收入/支出
  '收入': ['#2B7955', '#EAF5EF'], '支出': ['#B02A37', '#FBEDEF']
}

// 可行性等级后端存 A/B/C/D，展示时统一转成中文等级名
const ALIAS = { A: '推荐立项', B: '可行', C: '谨慎', D: '不建议' }
const label = computed(() => ALIAS[props.text] || props.text)

const c = computed(() => {
  const hit = MAP[ALIAS[props.text] || props.text]
  if (!hit) return { fg: '#5B6770', bg: '#F1F2F3' }
  return { fg: hit[0], bg: hit[1] }
})
</script>

<style scoped>
.st { display: inline-block; padding: 1px 8px; border-radius: 11px; font-size: 11.5px; line-height: 18px; white-space: nowrap; }
</style>
