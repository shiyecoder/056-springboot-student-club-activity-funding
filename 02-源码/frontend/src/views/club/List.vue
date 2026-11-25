<template>
  <div>
    <div class="sec-h">
      <h2>社团列表<span class="sub">共 {{ total }} 个社团</span></h2>
      <el-button type="primary" @click="openCreate">新增社团</el-button>
    </div>

    <div class="card">
      <div class="filter-bar">
        <el-input v-model="q.keyword" placeholder="社团名称 / 编号" clearable @keyup.enter="load(1)" />
        <el-select v-model="q.clubType" placeholder="社团类别" clearable>
          <el-option v-for="t in types" :key="t" :label="t" :value="t" />
        </el-select>
        <el-select v-model="q.status" placeholder="状态" clearable>
          <el-option label="正常" value="正常" />
          <el-option label="待注册" value="待注册" />
          <el-option label="整改中" value="整改中" />
          <el-option label="已注销" value="已注销" />
        </el-select>
        <el-button type="primary" @click="load(1)">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" border>
        <el-table-column label="封面" width="70">
          <template #default="{ row }">
            <img v-if="row.coverUrl" :src="row.coverUrl" class="thumb" alt="" />
            <div v-else class="img-empty"></div>
          </template>
        </el-table-column>
        <el-table-column prop="clubNo" label="社团编号" />
        <el-table-column prop="clubName" label="社团名称" min-width="140" />
        <el-table-column prop="clubType" label="类别" />
        <el-table-column prop="deptName" label="挂靠单位" />
        <el-table-column prop="presidentName" label="负责人" />
        <el-table-column prop="memberCount" label="成员数" align="right" />
        <el-table-column label="经费额度" align="right">
          <template #default="{ row }"><span class="mono">{{ money(row.fundLimit) }}</span></template>
        </el-table-column>
        <el-table-column label="星级" align="center">
          <template #default="{ row }">{{ row.starLevel ? row.starLevel + ' 星' : '-' }}</template>
        </el-table-column>
        <el-table-column label="状态">
          <template #default="{ row }"><StatusTag :text="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push('/club/review')">年审</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination background layout="total, sizes, prev, pager, next" :total="total"
                       v-model:current-page="q.page" v-model:page-size="q.size"
                       :page-sizes="[10, 20, 50]" @current-change="load()" @size-change="load(1)" />
      </div>
    </div>

    <el-dialog v-model="dlg" :title="form.id ? '编辑社团' : '新增社团'" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="社团名称"><el-input v-model="form.clubName" /></el-form-item>
        <el-form-item label="类别">
          <el-select v-model="form.clubType" placeholder="请选择">
            <el-option v-for="t in types" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="挂靠单位"><el-input v-model="form.deptName" /></el-form-item>
        <el-form-item label="经费额度">
          <el-input-number v-model="form.fundLimit" :min="0" :step="1000" style="width:180px" />
        </el-form-item>
        <el-form-item label="成立日期">
          <el-date-picker v-model="form.foundedDate" type="date" value-format="YYYY-MM-DD" style="width:180px" />
        </el-form-item>
        <el-form-item label="封面">
          <el-upload :action="'/api/file/upload?bizType=club'" :headers="headers" :show-file-list="false"
                     :on-success="onUp" accept="image/*">
            <img v-if="form.coverUrl" :src="form.coverUrl" class="thumb" style="width:88px;height:64px" alt="" />
            <el-button v-else>上传封面</el-button>
          </el-upload>
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../../api/http'
import StatusTag from '../../components/StatusTag.vue'
import { money } from '../../utils/format'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const dlg = ref(false)
const types = ['学术科技', '文艺体育', '公益实践', '创新创业']
const q = reactive({ page: 1, size: 10, keyword: '', clubType: '', status: '' })
const form = reactive({ id: null, clubName: '', clubType: '', deptName: '', fundLimit: 0, foundedDate: '', coverUrl: '' })
const headers = { Authorization: 'Bearer ' + (localStorage.getItem('cfs_token') || '') }

async function load(p) {
  if (p) q.page = p
  loading.value = true
  try {
    const d = await http.get('/club/page', { params: q })
    rows.value = d.records || []
    total.value = d.total || 0
  } finally { loading.value = false }
}

function reset() {
  q.keyword = ''
  q.clubType = ''
  q.status = ''
  load(1)
}

function openCreate() {
  Object.assign(form, { id: null, clubName: '', clubType: '', deptName: '', fundLimit: 0, foundedDate: '', coverUrl: '' })
  dlg.value = true
}

function openEdit(row) {
  Object.assign(form, row)
  dlg.value = true
}

function onUp(res) {
  if (res && res.code === 200 && res.data) form.coverUrl = res.data.url
}

async function save() {
  if (!form.clubName) return ElMessage.warning('请填写社团名称')
  if (form.id) await http.put('/club/' + form.id, form)
  else await http.post('/club', form)
  ElMessage.success('保存成功')
  dlg.value = false
  load()
}

onMounted(() => load())
</script>
