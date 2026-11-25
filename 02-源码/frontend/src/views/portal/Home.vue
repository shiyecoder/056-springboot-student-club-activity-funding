<template>
  <div>
    <div class="hero">
      <div class="hero-txt">
        <h1>你好，{{ store.realName }}</h1>
        <p>这里汇总了你所在社团的动态、可报名活动与最新经费公示。</p>
        <div class="hero-tags">
          <span v-for="r in roles" :key="r.clubName">{{ r.clubName }} · {{ r.memberRole }}</span>
        </div>
      </div>
      <div class="hero-stat">
        <div><b>{{ mySignupCount }}</b><span>我的报名</span></div>
        <div><b>{{ openCount }}</b><span>可报名活动</span></div>
        <div><b>{{ noticeCount }}</b><span>经费公示</span></div>
      </div>
    </div>

    <div class="sec-h" style="margin-top:16px">
      <h2>近期可报名活动<span class="sub">按开始时间排序</span></h2>
      <el-button link type="primary" @click="$router.push('/portal/activity')">全部活动</el-button>
    </div>
    <div class="acts">
      <div v-for="a in acts" :key="a.id" class="act" @click="$router.push('/portal/activity')">
        <img v-if="a.coverUrl" :src="a.coverUrl" alt="" />
        <div v-else class="ph"></div>
        <div class="act-body">
          <div class="act-h">
            <span class="nm">{{ a.activityName }}</span>
            <StatusTag :text="a.status" />
          </div>
          <div class="tip">{{ a.clubName }} · {{ a.activityType }}</div>
          <div class="tip">{{ $fmt(a.startTime) }} · {{ a.placeName }}</div>
        </div>
      </div>
      <el-empty v-if="!acts.length" description="暂无可报名活动" />
    </div>

    <div class="sec-h" style="margin-top:8px">
      <h2>最新经费公示</h2>
      <el-button link type="primary" @click="$router.push('/portal/fund')">查看全部</el-button>
    </div>
    <div class="card">
      <el-table :data="notices" size="small" border>
        <el-table-column prop="clubName" label="社团" min-width="140" />
        <el-table-column label="收入" align="right">
          <template #default="{ row }">{{ money(row.incomeAmount) }}</template>
        </el-table-column>
        <el-table-column label="支出" align="right">
          <template #default="{ row }">{{ money(row.expenseAmount) }}</template>
        </el-table-column>
        <el-table-column label="结余" align="right">
          <template #default="{ row }"><b>{{ money(row.balanceAmount) }}</b></template>
        </el-table-column>
        <el-table-column label="公示期" min-width="180">
          <template #default="{ row }">{{ $fmtDate(row.publishStart) }} ~ {{ $fmtDate(row.publishEnd) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><StatusTag :text="row.status" /></template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import http from '../../api/http'
import StatusTag from '../../components/StatusTag.vue'
import { money } from '../../utils/format'
import { store } from '../../store/user'

const roles = ref([])
const acts = ref([])
const notices = ref([])
const mySignupCount = ref(0)
const openCount = ref(0)
const noticeCount = ref(0)

onMounted(async () => {
  try { roles.value = (await http.get('/portal/profile/roles')).clubs || [] } catch (e) { /* 忽略 */ }
  try {
    const d = await http.get('/portal/activity/page', { params: { page: 1, size: 6, status: '已立项' } })
    acts.value = d.records || []
    openCount.value = d.total || 0
  } catch (e) { /* 忽略 */ }
  try {
    const s = await http.get('/portal/signup/mine')
    mySignupCount.value = (s || []).length
  } catch (e) { /* 忽略 */ }
  try {
    const n = await http.get('/fund-disclosure/page', { params: { page: 1, size: 5, status: '公示中' } })
    notices.value = n.records || []
    noticeCount.value = n.total || 0
  } catch (e) { /* 忽略 */ }
})
</script>

<style scoped>
.hero {
  display: grid; grid-template-columns: 1fr auto; gap: 24px; align-items: center;
  background: linear-gradient(120deg, #8C2F39 0%, #6E222B 62%, #4A1519 100%);
  border-radius: 12px; padding: 26px 30px; color: #fff;
}
.hero-txt h1 { font-size: 22px; margin: 0 0 8px; font-weight: 700; }
.hero-txt p { margin: 0 0 12px; font-size: 12.5px; color: rgba(255, 255, 255, .76); }
.hero-tags { display: flex; flex-wrap: wrap; gap: 8px; }
.hero-tags span {
  font-size: 11.5px; padding: 3px 10px; border-radius: 12px;
  background: rgba(232, 176, 75, .18); color: #F3CE86; border: 1px solid rgba(232, 176, 75, .35);
}
.hero-stat { display: flex; gap: 26px; }
.hero-stat div { text-align: center; }
.hero-stat b { display: block; font-size: 26px; font-variant-numeric: tabular-nums; }
.hero-stat span { font-size: 11.5px; color: rgba(255, 255, 255, .68); }

.acts { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 16px; }
.act {
  background: #fff; border-radius: 8px; overflow: hidden; box-shadow: var(--shadow);
  cursor: pointer; transition: transform .15s;
}
.act:hover { transform: translateY(-2px); }
.act img, .act .ph { width: 100%; height: 120px; object-fit: cover; display: block; background: var(--bg); }
.act-body { padding: 10px 12px 12px; }
.act-h { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin-bottom: 6px; }
.act-h .nm { font-size: 13px; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.act-body .tip { line-height: 1.75; }
</style>
