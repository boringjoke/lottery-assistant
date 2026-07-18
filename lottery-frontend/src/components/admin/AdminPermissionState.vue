<script setup lang="ts">
defineProps<{
  denied?: boolean
}>()
</script>

<template>
  <section v-if="denied" class="permission-state permission-state--denied">
    <div class="permission-state__mark">!</div>
    <h1>您无权操作页面</h1>
    <p>当前账号没有同步管理权限，请返回公共彩票助手页面继续使用开奖查询和号码分析功能。</p>
    <RouterLink class="permission-state__button" to="/lottery-assistant?tab=overview">
      返回彩票助手
    </RouterLink>
  </section>

  <section v-else class="permission-state" aria-live="polite">
    <div class="permission-state__mark">≋</div>
    <h1>正在验证管理权限</h1>
    <p>请稍候，系统正在确认当前登录状态。</p>
  </section>
</template>
