<template>
  <div>
    <div class="sec-h">
      <h2>我的社团<span class="sub">你当前加入的社团与任职</span></h2>
    </div>

    <div v-if="clubs.length" class="club-list">
      <div v-for="c in clubs" :key="c.id" class="club-card">
        <img v-if="c.coverUrl" :src="c.coverUrl" alt="" />
        <div v-else class="ph"></div>
        <div class="c-body">
          <div class="c-h">
            <span class="c-nm">{{ c.clubName }}</span>
            <StatusTag :text="c.status" />
          </div>
          <div class="c-meta">
            <span>{{ c.clubType }}</span>
            <span>负责人 {{ c.presidentName || '-' }}</span>
            <span>成员 {{ c.memberCount }} 人</span>
          </div>
          <div class="c-role">我的职务：<b>{{ c.memberRole }}</b> · 入社 {{ $fmtDate(c.joinDate) }}</div>
          <div class="c-stat">
            <div><b>{{ c.activityCount || 0 }}</b><span>参与活动</span></div>
            <div><b>{{ c.serviceHours || 0 }}</b><span>志愿时长</span></div>
          </div>
        </div>
      </div>
    </div>
    <el-empty v-else description="尚未加入任何社团" />

    <div class="card" style="margin-top:12px">
      <div class="card-h"><h3>成员权益说明</h3></div>
      <div class="tip" style="line-height:2">
        成员可报名本社团及全校公开活动；活动签到成功计入志愿服务时长；
        对经费公示有疑问时，可在「经费公开」中提交异议，由社团负责人答复。
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import http from '../../api/http'
import StatusTag from '../../components/StatusTag.vue'

const clubs = ref([])

onMounted(async () => {
  try {
    const d = await http.get('/portal/club/mine')
    clubs.value = Array.isArray(d) ? d : (d.clubs || [])
  } catch (e) { /* 提示 */ }
})
</script>

<style scoped>
.club-list { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.club-card {
  background: #fff; border-radius: 8px; box-shadow: var(--shadow);
  display: grid; grid-template-columns: 150px 1fr; overflow: hidden;
}
.club-card img, .club-card .ph { width: 100%; height: 100%; min-height: 150px; object-fit: cover; background: var(--bg); }
.c-body { padding: 14px 16px; }
.c-h { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.c-nm { font-size: 15px; font-weight: 700; }
.c-meta { display: flex; flex-wrap: wrap; gap: 12px; font-size: 11.5px; color: var(--text-3); margin-bottom: 8px; }
.c-role { font-size: 12px; color: var(--text-2); margin-bottom: 10px; }
.c-role b { color: var(--primary); }
.c-stat { display: flex; gap: 28px; padding-top: 10px; border-top: 1px dashed var(--line); }
.c-stat b { font-size: 17px; font-variant-numeric: tabular-nums; }
.c-stat span { display: block; font-size: 11px; color: var(--text-3); }
</style>
