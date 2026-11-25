<template>
  <div>
    <div class="sec-h">
      <h2>活动广场<span class="sub">共 {{ total }} 场公开活动</span></h2>
      <div class="filter-bar" style="margin:0">
        <el-select v-model="q.status" placeholder="状态" clearable style="width:140px" @change="load(1)">
          <el-option label="已立项" value="已立项" />
          <el-option label="进行中" value="进行中" />
          <el-option label="已结束" value="已结束" />
        </el-select>
        <el-select v-model="q.activityType" placeholder="类型" clearable style="width:150px" @change="load(1)">
          <el-option v-for="t in types" :key="t" :label="t" :value="t" />
        </el-select>
      </div>
    </div>

    <div class="acts">
      <div v-for="a in rows" :key="a.id" class="act">
        <img v-if="a.coverUrl" :src="a.coverUrl" alt="" />
        <div v-else class="ph"></div>
        <div class="act-body">
          <div class="act-h">
            <span class="nm">{{ a.activityName }}</span>
            <StatusTag :text="a.status" />
          </div>
          <div class="tip">{{ a.clubName }} · {{ a.activityType }}</div>
          <div class="tip">{{ $fmt(a.startTime) }}</div>
          <div class="tip">{{ a.placeName }} · 预计 {{ a.expectCount }} 人</div>
          <div class="act-foot">
            <span class="tip">已报名 {{ a.signupCount || 0 }} 人</span>
            <el-button type="primary" size="small" :disabled="a.status !== '已立项'"
                       @click="signup(a)">我要报名</el-button>
          </div>
        </div>
      </div>
    </div>
    <el-empty v-if="!rows.length" description="暂无活动" />

    <div class="pager">
      <el-pagination background layout="total, prev, pager, next" :total="total"
                     v-model:current-page="q.page" :page-size="q.size" @current-change="load()" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../../api/http'
import StatusTag from '../../components/StatusTag.vue'

const rows = ref([])
const total = ref(0)
const types = ['讲座沙龙', '竞赛比赛', '志愿服务', '文艺演出', '社会实践', '招新宣传']
const q = reactive({ page: 1, size: 9, status: '已立项', activityType: '' })

async function load(p) {
  if (p) q.page = p
  try {
    const d = await http.get('/portal/activity/page', { params: q })
    rows.value = d.records || []
    total.value = d.total || 0
  } catch (e) { /* 提示 */ }
}

async function signup(a) {
  try {
    await http.post('/activity-signup', { activityId: a.id })
    ElMessage.success('报名成功，请在「我的报名」查看')
    load()
  } catch (e) { /* 提示 */ }
}

onMounted(() => load())
</script>

<style scoped>
.acts { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.act { background: #fff; border-radius: 8px; overflow: hidden; box-shadow: var(--shadow); }
.act img, .act .ph { width: 100%; height: 128px; object-fit: cover; display: block; background: var(--bg); }
.act-body { padding: 10px 12px 12px; }
.act-h { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin-bottom: 6px; }
.act-h .nm { font-size: 13px; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.act-body .tip { line-height: 1.75; }
.act-foot {
  display: flex; align-items: center; justify-content: space-between;
  margin-top: 10px; padding-top: 10px; border-top: 1px dashed var(--line);
}
</style>
