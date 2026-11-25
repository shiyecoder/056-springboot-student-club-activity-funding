<template>
  <div>
    <div class="sec-h">
      <h2>可行性规则<span class="sub">五因子加权评分规则，可配置权重与等级阈值</span></h2>
      <el-button type="primary" @click="openEdit">编辑当前规则</el-button>
    </div>

    <div class="grid grid-2u">
      <div class="card">
        <div class="card-h">
          <h3>当前生效规则</h3>
          <span class="tip" v-if="cur">v{{ cur.ruleVersion }} · {{ cur.ruleName }}</span>
        </div>
        <div v-if="cur" class="weights">
          <div v-for="w in weights" :key="w.name" class="w-row">
            <div class="w-lab"><span>{{ w.name }}</span><b>{{ w.value }}%</b></div>
            <div class="bar"><i :style="{ width: w.value + '%' }"></i></div>
          </div>
          <div class="total">
            <span>权重合计</span>
            <b :style="{ color: totalWeight === 100 ? '#2B7955' : '#B02A37' }">{{ totalWeight }}%</b>
          </div>
        </div>
        <el-empty v-else :image-size="60" description="暂无生效规则" />

        <el-divider />
        <div class="levels">
          <div v-for="l in levels" :key="l.name" class="lv">
            <span class="lv-name">{{ l.name }}</span>
            <span class="lv-range">{{ l.range }}</span>
            <span class="tip">{{ l.desc }}</span>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-h"><h3>规则版本</h3><span class="tip">{{ rules.length }} 个版本</span></div>
        <el-table :data="rules" size="small" border>
          <el-table-column prop="ruleVersion" label="版本" width="70" />
          <el-table-column prop="ruleName" label="规则名称" min-width="150" />
          <el-table-column label="权重" min-width="150">
            <template #default="{ row }">
              <span class="tip">
                {{ row.weightBudget }}/{{ row.weightVenue }}/{{ row.weightCapacity }}/{{ row.weightHistory }}/{{ row.weightClub }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="80">
            <template #default="{ row }"><StatusTag :text="row.status" /></template>
          </el-table-column>
          <el-table-column label="生效时间" min-width="150">
            <template #default="{ row }">{{ $fmt(row.effectiveTime) }}</template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <el-dialog v-model="dlg" title="编辑可行性规则" width="600px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="规则名称"><el-input v-model="form.ruleName" /></el-form-item>
        <el-form-item label="版本号">
          <el-input-number v-model="form.ruleVersion" :min="1" :step="1" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="预算权重"><el-input-number v-model="form.weightBudget" :min="0" :max="100" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="场地权重"><el-input-number v-model="form.weightVenue" :min="0" :max="100" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="容量权重"><el-input-number v-model="form.weightCapacity" :min="0" :max="100" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="历史权重"><el-input-number v-model="form.weightHistory" :min="0" :max="100" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="资质权重"><el-input-number v-model="form.weightClub" :min="0" :max="100" /></el-form-item></el-col>
        </el-row>
        <div class="tip" style="margin:0 0 12px 110px">
          五项权重合计必须等于 100%，否则无法保存（后端强校验）。
        </div>
        <el-form-item label="等级阈值">
          <div class="tips">
            <div>推荐立项 ≥ {{ form.levelA }} 分</div>
            <div>可行 ≥ {{ form.levelB }} 分</div>
            <div>谨慎 ≥ {{ form.levelC }} 分，其余为不建议</div>
          </div>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="8"><el-form-item label="A 线" label-width="52px"><el-input-number v-model="form.levelA" :min="0" :max="100" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="B 线" label-width="52px"><el-input-number v-model="form.levelB" :min="0" :max="100" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="C 线" label-width="52px"><el-input-number v-model="form.levelC" :min="0" :max="100" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="说明本次规则调整的原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../../api/http'
import StatusTag from '../../components/StatusTag.vue'

const cur = ref(null)
const rules = ref([])
const dlg = ref(false)
const form = reactive({
  id: null, ruleName: '', ruleVersion: 1, weightBudget: 25, weightVenue: 20, weightCapacity: 20,
  weightHistory: 20, weightClub: 15, levelA: 80, levelB: 65, levelC: 50, remark: ''
})

const weights = computed(() => cur.value ? [
  { name: '预算合理性', value: Number(cur.value.weightBudget || 0) },
  { name: '场地可行性', value: Number(cur.value.weightVenue || 0) },
  { name: '容量匹配度', value: Number(cur.value.weightCapacity || 0) },
  { name: '历史执行', value: Number(cur.value.weightHistory || 0) },
  { name: '社团资质', value: Number(cur.value.weightClub || 0) }
] : [])

const totalWeight = computed(() => weights.value.reduce((a, b) => a + b.value, 0))
const levels = computed(() => cur.value ? [
  { name: '推荐立项', range: '≥ ' + cur.value.levelA + ' 分', desc: '可直接立项，优先排期' },
  { name: '可行', range: '≥ ' + cur.value.levelB + ' 分', desc: '条件基本具备，需补充材料' },
  { name: '谨慎', range: '≥ ' + cur.value.levelC + ' 分', desc: '存在明显短板，建议调整方案' },
  { name: '不建议', range: '< ' + cur.value.levelC + ' 分', desc: '不建议立项，需重新论证' }
] : [])

async function load() {
  try { cur.value = await http.get('/stat/rule/current') } catch (e) { /* 提示 */ }
  try { rules.value = await http.get('/stat/rule/list') } catch (e) { /* 提示 */ }
}

function openEdit() {
  if (cur.value) Object.assign(form, cur.value)
  form.ruleVersion = Number(form.ruleVersion || 1) + 1
  dlg.value = true
}

async function save() {
  const sum = Number(form.weightBudget) + Number(form.weightVenue) + Number(form.weightCapacity)
    + Number(form.weightHistory) + Number(form.weightClub)
  if (sum !== 100) return ElMessage.warning('五项权重合计必须等于 100%，当前为 ' + sum + '%')
  try {
    await http.post('/stat/rule', form)
    ElMessage.success('规则已保存并生效')
    dlg.value = false
    load()
  } catch (e) { /* 提示 */ }
}

onMounted(load)
</script>

<style scoped>
.weights { display: flex; flex-direction: column; gap: 10px; }
.w-row .w-lab { display: flex; justify-content: space-between; font-size: 12px; margin-bottom: 4px; }
.bar { height: 8px; border-radius: 4px; background: #F1F2F3; overflow: hidden; }
.bar i { display: block; height: 100%; background: linear-gradient(90deg, #8C2F39, #C25B63); }
.total { display: flex; justify-content: space-between; font-size: 12.5px; padding-top: 6px; border-top: 1px dashed var(--line); }
.levels { display: flex; flex-direction: column; gap: 8px; }
.lv { display: grid; grid-template-columns: 76px 90px 1fr; align-items: center; font-size: 12px; }
.lv-name { font-weight: 600; }
.lv-range { color: var(--primary); font-variant-numeric: tabular-nums; }
.tips div { font-size: 12px; color: var(--text-2); line-height: 1.9; }
</style>
