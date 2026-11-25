<template>
  <div ref="el" class="chart" :style="{ height: height + 'px' }"></div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  option: { type: Object, required: true },
  height: { type: Number, default: 180 }
})

const el = ref(null)
let inst = null

// 统一图表主题：主色 #8C2F39 → #C25B63 → #E8B04B → #6B7A85 → #BFC6CB
const PALETTE = ['#8C2F39', '#C25B63', '#E8B04B', '#6B7A85', '#BFC6CB']

function render() {
  if (!inst) return
  inst.setOption({ color: PALETTE, ...props.option }, true)
}

function resize() { if (inst) inst.resize() }

onMounted(() => {
  inst = echarts.init(el.value)
  render()
  window.addEventListener('resize', resize)
})

watch(() => props.option, render, { deep: true })

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  if (inst) { inst.dispose(); inst = null }
})
</script>

<style scoped>
.chart { width: 100%; }
</style>
