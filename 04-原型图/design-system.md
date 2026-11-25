# 设计系统 · 学生社团活动与经费管理系统（NEW-056）

> 依据需求文档第 10 章「UI 设计规范」落地。所有页面 1:1 复刻本规范，实现阶段直接引用 design/assets/mock.css 的变量。

## 1. 设计方向

酒红 + 暖金，社团门户偏活力但不使用插画；财务板块强调表格与金额右对齐、等宽数字。
后台走「白色侧栏 + 酒红顶栏 + 浅灰内容区」，门户走「白顶栏 + 酒红渐变主视觉 + 卡片流」。
状态绝不只靠颜色表达，所有状态同时带中文标签。

## 2. 色彩

| Token | 色值 | 用途 |
|---|---|---|
| `--primary` | `#8C2F39` | 顶栏、主按钮、当前菜单、关键数字 |
| `--primary-dark` | `#6E222B` | 主按钮 hover |
| `--primary-soft` | `#F6E9EA` | 主色浅底（图标块、选中态） |
| `--second` | `#C25B63` | 次级按钮、筛选选中、普通图表系列 |
| `--accent` | `#E8B04B` | 重点提示、待办角标、对比系列 |
| `--bg` | `#F5F6F7` | 内容区背景 |
| `--card` | `#FFFFFF` | 表格、表单、卡片容器 |
| `--text` | `#1F2933` | 标题与正文 |
| `--text-2` | `#5B6770` | 次要信息、表头 |
| `--text-3` | `#98A2AB` | 辅助说明、占位 |
| `--line` | `#E3E6E8` | 分割线、输入框描边 |
| `--ok` | `#2B7955` | 已通过、正常、已支付 |
| `--danger` | `#B02A37` | 驳回、超时、删除（小面积） |
| `--info` | `#3A6EA5` | 待审批、进行中 |

图表配色顺序：`#8C2F39` → `#C25B63` → `#E8B04B` → `#6B7A85` → `#BFC6CB`。
同一含义跨页面同色：待审批=蓝、进行中=金、已通过/正常=绿、驳回/逾期=红、结束/归档=灰。

## 3. 字体与字号

系统字体栈：`-apple-system, "PingFang SC", "Microsoft YaHei", Helvetica, Arial`。

| 层级 | 字号 / 字重 | 场景 |
|---|---|---|
| 页面标题 H2 | 15px / 600 | `.sec-h h2` |
| 卡片标题 | 13.5px / 600 | `.card-h h3` |
| 正文 | 13px / 400 | 表单、详情值 |
| 表格 | 12.5px / 400，表头 11.5px / 600 | `.tbl` |
| KPI 数值 | 21px / 700，`font-variant-numeric: tabular-nums` | `.kpi .val` |
| 说明文字 | 11–11.5px / 400 | `.tip` `.hint` `.dup` |

金额、比例、编号一律 `tabular-nums`；金额格式 `¥12,600.00`，比例保留 1 位小数。

## 4. 间距与圆角

- 基准 4px 栅格；卡片内边距 16px，内容区外边距 24px，卡片间距 12px。
- 圆角：卡片/容器 8px、按钮与输入框 6px、标签 11px、头像 50%。
- 阴影：`0 1px 2px rgba(31,41,51,.06), 0 2px 8px rgba(31,41,51,.04)`，仅卡片使用。
- 表单标签与控件间距 5px；两列栅格列间距 20px、行间距 14px。

## 5. 布局框架

**管理端**：`.app` = 220px 侧栏 + 56px 顶栏 + 自适应内容区（`height:100vh`，内容区独立滚动）。
侧栏分 5 组：社团治理 / 活动运营 / 经费管理 / 统计与规则 / 系统管理，底部为站内消息与个人中心；待办角标用强调色。
顶栏只放面包屑、消息、个人中心、头像与角色，不放宣传内容。

**门户端**：`.pt-top` 60px 白顶栏（品牌 + 5 个导航 + 消息 + 用户），内容区最大宽 1240px 居中，顶部渐变 Hero 横幅。

**一屏约束**：驾驶舱在 1440×900 下必须完整显示 8 个 KPI + 6 个图表，判定依据是 `.content` 的 `scrollHeight === clientHeight`（可用高度 844px）。
当前实测：KPI 2 行 ≈ 160px，图表 2 行 ≈ 412px，标题行 26px，合计 610px，余量 194px。

## 6. 页面清单（32 页，均可点击互跳）

管理端 26 页：
`dashboard` 社团驾驶舱 · `club-list` 社团列表 · `club-review` 年审填报 · `member-list` 成员名册 · `member-appoint` 干部任免 ·
`activity-list` 活动列表 · `activity-apply` 活动申报 · `activity-approve` 立项审批台 · `activity-detail` 活动详情 · `activity-checkin` 签到核验 ·
`budget-edit` 预算编制 · `budget-audit` 预算审核台 · `expense-apply` 报销单 · `expense-audit` 财务复核台 ·
`fund-flow` 经费流水 · `fund-disclosure` 经费公示 · `objection` 异议处理 ·
`report` 社团统计 · `feasibility-rule` 可行性规则 ·
`user` 用户管理 · `role` 角色管理 · `menu` 菜单管理 · `dict` 字典管理 · `log` 操作日志 · `message` 站内消息 · `profile` 个人中心

门户端 5 页：`portal-home` 首页 · `portal-activity` 活动广场 · `portal-my` 我的报名 · `portal-club` 我的社团 · `portal-fund` 经费公示

登录页：`design/login.html`（左侧酒红品牌区 + 右侧表单，演示账号 admin/123456）。

## 7. 角色视觉区分

| 角色 | 落地页 | 视觉特征 |
|---|---|---|
| 校团委干事 | 管理端全量 | 顶栏显示「校团委干事」，审批类页面为主 |
| 财务人员 | expense-audit / fund-flow / fund-disclosure | 表格列以金额为主，金额右对齐加粗 |
| 社团负责人 | activity-apply / budget-edit / expense-apply | 表单密集，右侧实时校验面板 |
| 社团成员 | 门户 5 页 | 卡片流 + 渐变 Hero，无侧栏 |
| 系统管理员 | user / role / menu / dict / log | 表格 + 抽屉，颜色克制 |

## 8. 实现约定

- 所有图片字段用本地上传，路径 `/uploads/seed/<实体>/NN.jpg`，禁止外链。
- 图表用 ECharts，颜色取第 2 节图表序列；环形图中心显示合计。
- 列表分页后端实现，前端保留页码与每页条数；表头不固定宽度。
- 状态标签统一 `<StatusTag>` 组件，从字典接口取标签与颜色语义。
- 时间统一 `yyyy-MM-dd HH:mm:ss`，日期 `yyyy-MM-dd`，全系统一个格式化函数。
