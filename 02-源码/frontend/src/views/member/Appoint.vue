<template>
  <div>
    <div class="sec-h">
      <h2>干部任免<span class="sub">社长 / 副社长 / 部长任免需团委备案</span></h2>
    </div>

    <div class="grid grid-4" style="margin-bottom:12px">
      <div v-for="s in stats" :key="s.label" class="card" style="margin:0;padding:12px 14px">
        <div class="tip">{{ s.label }}</div>
        <div style="font-size:20px;font-weight:700;margin-top:4px" class="mono">{{ s.value }}</div>
      </div>
    </div>

    <div class="card">
      <div class="filter-bar">
        <el-select v-model="q.clubId" placeholder="所属社团" clearable filterable @change="load(1)">
          <el-option v-for="c in clubs" :key="c.id" :label="c.clubName" :value="c.id" />
        </el-select>
        <el-select v-model="q.memberRole" placeholder="当前职务" clearable @change="load(1)">
          <el-option v-for="r in roles" :key="r" :label="r" :value="r" />
        </el-select>
        <el-button @click="load(1)">刷新</el-button>
        <div class="spacer"></div>
        <span class="tip">任免会写入操作日志，可追溯</span>
      </div>

      <el-table :data="rows" v-loading="loading" border>
        <el-table-column label="头像" width="62">
          <template #default="{ row }">
            <img v-if="row.avatarUrl" :src="row.avatarUrl" class="avatar-sm" alt="" />
            <div v-else class="avatar-sm" style="background:var(--primary-soft)"></div>
          </template>
        </el-table-column>
        <el-table-column label="姓名" min-width="100">
          <template #default="{ row }">{{ row.studentName || row.realName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="clubName" label="所属社团" min-width="150" />
        <el-table-column prop="deptName" label="院系" />
        <el-table-column label="当前职务">
          <template #default="{ row }"><StatusTag :text="row.memberRole" /></template>
        </el-table-column>
        <el-table-column prop="activityCount" label="参与活动" align="right" />
        <el-table-column label="入社日期">
          <template #default="{ row }">{{ $fmtDate(row.joinDate) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openAppoint(row)">任免</el-button>
            <el-button link type="danger" @click="openQuit(row)">办理退出</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, prev, pager, next" :total="total"
                       v-model:current-page="q.page" :page-size="q.size" @current-change="load()" />
      </div>
    </div>

    <el-dialog v-model="dlg" title="干部任免" width="480px">
      <el-form label-width="88px">
        <el-form-item label="成员"><el-input :model-value="cur.studentName || cur.realName" disabled /></el-form-item>
        <el-form-item label="新任职务">
          <el-select v-model="form.memberRole" placeholder="请选择职务">
            <el-option v-for="r in roles" :key="r" :label="r" :value="r" />
          </el-select>
        </el-form-item>
        <el-form-item label="任免说明">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请说明任免原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="doAppoint">确认任免</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dlgQuit" title="办理退社" width="460px">
      <el-form label-width="88px">
        <el-form-item label="成员"><el-input :model-value="cur.studentName || cur.realName" disabled /></el-form-item>
        <el-form-item label="退出原因">
          <el-input v-model="quit.reason" type="textarea" :rows="3" placeholder="请填写退出原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlgQuit = false">取消</el-button>
        <el-button type="danger" @click="doQuit">确认退出</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../../api/http'
import StatusTag from '../../components/StatusTag.vue'

const rows = ref([])
const clubs = ref([])
const report = ref({})
const total = ref(0)
const loading = ref(false)
const dlg = ref(false)
const dlgQuit = ref(false)
const cur = ref({})
const roles = ['社长', '副社长', '部长', '干事', '成员']
const q = reactive({ page: 1, size: 10, clubId: null, memberRole: '' })
const form = reactive({ memberRole: '', remark: '' })
const quit = reactive({ reason: '' })

const stats = computed(() => [
  { label: '在册成员', value: report.value.total || 0 },
  { label: '社团干部', value: report.value.leaderCount || 0 },
  { label: '本年新增', value: report.value.newCount || 0 },
  { label: '本年退出', value: report.value.quitCount || 0 }
])

async function load(p) {
  if (p) q.page = p
  loading.value = true
  try {
    const d = await http.get('/club-member/page', { params: q })
    rows.value = d.records || []
    total.value = d.total || 0
  } finally { loading.value = false }
}

function openAppoint(row) {
  cur.value = row
  form.memberRole = row.memberRole || '成员'
  form.remark = ''
  dlg.value = true
}

function openQuit(row) {
  cur.value = row
  quit.reason = ''
  dlgQuit.value = true
}

async function doAppoint() {
  if (!form.memberRole) return ElMessage.warning('请选择职务')
  await http.post('/club-member/' + cur.value.id + '/appoint', { memberRole: form.memberRole, remark: form.remark })
  ElMessage.success('任免已生效')
  dlg.value = false
  load()
}

async function doQuit() {
  if (!quit.reason || quit.reason.trim().length < 2) return ElMessage.warning('请填写退出原因')
  await http.post('/club-member/' + cur.value.id + '/quit', { reason: quit.reason })
  ElMessage.success('已办理退社')
  dlgQuit.value = false
  load()
}

onMounted(async () => {
  try { clubs.value = await http.get('/club/options') } catch (e) { /* 忽略 */ }
  try { report.value = await http.get('/club-member/report') } catch (e) { /* 忽略 */ }
  load()
})
</script>
