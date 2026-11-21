<template>
  <div ref="el" class="base-chart" :style="{ height: height || '300px' }"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps<{ option: any; height?: string }>()
const el = ref<HTMLElement | null>(null)
let chart: echarts.ECharts | null = null

const resize = () => { chart?.resize() }

function render() {
  if (!el.value) return
  if (!chart) chart = echarts.init(el.value)
  chart.setOption(props.option || {}, true)
}

onMounted(() => {
  render()
  window.addEventListener('resize', resize)
})

watch(() => props.option, () => render(), { deep: true })

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  chart?.dispose()
  chart = null
})
</script>

<style scoped>
.base-chart {
  width: 100%;
  min-height: 240px;
}
</style>
