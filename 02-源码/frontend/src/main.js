import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import './styles.css'
import { fmt, fmtDate } from './utils/format'

const app = createApp(App)
for (const [key, comp] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, comp)
}
app.use(ElementPlus, { locale: zhCn })
app.use(router)
// 全局时间格式化函数：全系统统一调用（规范第 9 条）
app.config.globalProperties.$fmt = fmt
app.config.globalProperties.$fmtDate = fmtDate
app.mount('#app')
