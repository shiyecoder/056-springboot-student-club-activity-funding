<template>
  <div>
    <div class="sec-h">
      <h2>年审填报<span class="sub">年度 {{ year }}：{{ rows.length }} 个社团待/已年审</span></h2>
      <el-select v-model="year" style="width:140px" @change="load">
        <el-option v-for="y in years" :key="y" :label="y + ' 年度'" :value="y" />
      </el-select>
    </div>

    <div class="card">
      <el-table :data="rows" v-loading="loading" border>
        <el-table-column label="封面" width="70">
          <template #default="{ row }">
            <img v-if="row.coverUrl" :src="row.coverUrl" class="thumb" alt="" />
            <div v-else class="img-empty"></div>
          </template>
        </el-table-column>
        <el-table-column prop="clubName" label="社团名称" min-width="150" />
        <el-table-column prop="clubType" label="类别" />
        <el-table-column prop="memberCount" label="成员数" align="right" />
        <el-table-column label="经费额度" align="right">
          <template #default="{ row }"><span class="mono">{{ money(row.fundLimit) }}</span></template>
        </el-table-column>
        <el-table-column label="星级" align="center">
          <template #default="{ row }">{{ row.starLevel ? row.starLevel + ' 星' : '未评' }}</template>
        </el-table-column>
        <el-table-column prop="reviewYear" label="年审年度" align="center" />
        <el-table-column label="年审意见">
          <template #default="{ row }">
            <span class="tip">{{ row.reviewRemark || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态">
          <template #default="{ row }"><StatusTag :text="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="open(row)">填报年审</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, prev, pager, next" :total="total"
                       v-model:current-page="q.page" :page-size="q.size" @current-change="load()" />
      </div>
    </div>

    <el-dialog v-model="dlg" title="社团年度审查" width="520px">
      <el-form :model="form" label-width="88px">
        <el-form-item label="社团">
          <el-input :model-value="cur.clubName" disabled />
        </el-form-item>
        <el-form-item label="年审结论">
          <el-radio-group v-model="form.result">
            <el-radio value="PASS">通过</el-radio>
            <el-radio value="WARN">预警</el-radio>
            <el-radio value="REJECT">不予通过</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="星级评定">
          <el-rate v-model="form.starLevel" :max="5" show-score />
        </el-form-item>
        <el-form-item label="审查意见">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请填写年度审查意见，不少于 5 个字" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="submit">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../../api/http'
import StatusTag from '../../components/StatusTag.vue'
import { money } from '../../utils/format'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const dlg = ref(false)
const cur = ref({})
const nowY = new Date().getFullYear()
const year = ref(nowY)
const years = [nowY, nowY - 1, nowY - 2]
const q = reactive({ page: 1, size: 10 })
const form = reactive({ result: 'PASS', starLevel: 4, remark: '' })

async function load() {
  loading.value = true
  try {
    const d = await http.get('/club/page', { params: { ...q, reviewYear: year.value } })
    rows.value = d.records || []
    total.value = d.total || 0
  } finally { loading.value = false }
}

function open(row) {
  cur.value = row
  form.result = 'PASS'
  form.starLevel = row.starLevel || 4
  form.remark = ''
  dlg.value = true
}

async function submit() {
  if (!form.remark || form.remark.trim().length < 5) return ElMessage.warning('审查意见不少于 5 个字')
  await http.post('/club/' + cur.value.id + '/annual-review', {
    result: form.result, starLevel: form.starLevel, remark: form.remark, reviewYear: year.value, year: year.value
  })
  ElMessage.success('年审已提交')
  dlg.value = false
  load()
}

onMounted(load)
</script>
