import { createRouter, createWebHistory } from 'vue-router'

/**
 * 路由表与后端 sys_menu.component 一一对应。
 * 守卫只读 localStorage 的 token，避免刷新后被 store 快照弹回登录页。
 */
const routes = [
  { path: '/login', component: () => import('../views/Login.vue'), meta: { public: true } },
  {
    path: '/',
    component: () => import('../layouts/AdminLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '社团驾驶舱' } },
      { path: 'club/list', component: () => import('../views/club/List.vue'), meta: { title: '社团列表' } },
      { path: 'club/review', component: () => import('../views/club/Review.vue'), meta: { title: '年审填报' } },
      { path: 'member/list', component: () => import('../views/member/List.vue'), meta: { title: '成员名册' } },
      { path: 'member/appoint', component: () => import('../views/member/Appoint.vue'), meta: { title: '干部任免' } },
      { path: 'activity/list', component: () => import('../views/activity/List.vue'), meta: { title: '活动列表' } },
      { path: 'activity/apply', component: () => import('../views/activity/Apply.vue'), meta: { title: '活动申报' } },
      { path: 'activity/approve', component: () => import('../views/activity/Approve.vue'), meta: { title: '立项审批台' } },
      { path: 'activity/checkin', component: () => import('../views/activity/Checkin.vue'), meta: { title: '签到核验' } },
      { path: 'activity/detail/:id', component: () => import('../views/activity/Detail.vue'), meta: { title: '活动详情' } },
      { path: 'fund/budget', component: () => import('../views/fund/Budget.vue'), meta: { title: '预算编制' } },
      { path: 'fund/budget-audit', component: () => import('../views/fund/BudgetAudit.vue'), meta: { title: '预算审核' } },
      { path: 'fund/expense', component: () => import('../views/fund/Expense.vue'), meta: { title: '报销单' } },
      { path: 'fund/finance-check', component: () => import('../views/fund/FinanceCheck.vue'), meta: { title: '财务复核' } },
      { path: 'fund/flow', component: () => import('../views/fund/Flow.vue'), meta: { title: '经费流水' } },
      { path: 'fund/disclosure', component: () => import('../views/fund/Disclosure.vue'), meta: { title: '经费公示' } },
      { path: 'fund/objection', component: () => import('../views/fund/Objection.vue'), meta: { title: '公示异议' } },
      { path: 'stat/report', component: () => import('../views/stat/Report.vue'), meta: { title: '统计报表' } },
      { path: 'stat/rule', component: () => import('../views/stat/Rule.vue'), meta: { title: '可行性规则' } },
      { path: 'system/user', component: () => import('../views/system/User.vue'), meta: { title: '用户管理' } },
      { path: 'system/role', component: () => import('../views/system/Role.vue'), meta: { title: '角色管理' } },
      { path: 'system/menu', component: () => import('../views/system/Menu.vue'), meta: { title: '菜单管理' } },
      { path: 'system/dict', component: () => import('../views/system/Dict.vue'), meta: { title: '字典管理' } },
      { path: 'system/log', component: () => import('../views/system/Log.vue'), meta: { title: '操作日志' } },
      { path: 'message', component: () => import('../views/Message.vue'), meta: { title: '消息中心' } },
      { path: 'profile', component: () => import('../views/Profile.vue'), meta: { title: '个人中心' } }
    ]
  },
  {
    path: '/portal',
    component: () => import('../layouts/PortalLayout.vue'),
    redirect: '/portal/home',
    children: [
      { path: 'home', component: () => import('../views/portal/Home.vue'), meta: { title: '门户首页' } },
      { path: 'club', component: () => import('../views/portal/Club.vue'), meta: { title: '我的社团' } },
      { path: 'activity', component: () => import('../views/portal/Activity.vue'), meta: { title: '活动广场' } },
      { path: 'signup', component: () => import('../views/portal/Signup.vue'), meta: { title: '我的报名' } },
      { path: 'fund', component: () => import('../views/portal/Fund.vue'), meta: { title: '经费公开' } },
      { path: 'profile', component: () => import('../views/Profile.vue'), meta: { title: '个人中心' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
  if (to.meta.public) return true
  if (!localStorage.getItem('cfs_token')) return '/login'
  // 成员角色只进门户，管理角色不进门户首页
  const role = localStorage.getItem('cfs_roleCode') || ''
  if (role === 'MEMBER' && !to.path.startsWith('/portal')) return '/portal/home'
  if (role !== 'MEMBER' && to.path === '/portal/home') return '/dashboard'
  return true
})

export default router
