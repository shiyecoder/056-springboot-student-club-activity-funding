<template>
  <div>
    <div class="sec-h">
      <h2>菜单管理<span class="sub">目录 / 菜单 / 按钮三级结构，按角色动态下发</span></h2>
    </div>

    <div class="card">
      <el-table :data="tree" v-loading="loading" row-key="id" border default-expand-all
                :tree-props="{ children: 'children' }">
        <el-table-column prop="menuName" label="菜单名称" min-width="180" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="row.menuType === 'DIR' ? 'info' : 'success'" effect="plain" size="small">
              {{ row.menuType === 'DIR' ? '目录' : '菜单' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路由路径" min-width="150" />
        <el-table-column prop="component" label="组件" min-width="150" />
        <el-table-column prop="perms" label="权限标识" min-width="130" />
        <el-table-column prop="sortNo" label="排序" width="70" align="center" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <span class="tip">{{ Number(row.status) === 1 ? '启用' : '停用' }}</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="card">
      <div class="card-h"><h3>说明</h3></div>
      <div class="tip" style="line-height:2">
        菜单由后端按当前登录角色的 sys_role_menu 关联动态返回，前端只渲染接口结果，不在代码里写死权限。
        「分配菜单」时会将本节点从可选项中隐藏，避免非管理员角色获得菜单管理入口。
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import http from '../../api/http'

const tree = ref([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try { tree.value = (await http.get('/system/menu/tree')) || [] } catch (e) { /* 提示 */ } finally { loading.value = false }
})
</script>
