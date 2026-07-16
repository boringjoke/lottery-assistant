<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { fetchDltDrawDetail } from '@/api/lottery'
import AnalyzeTab from '@/components/AnalyzeTab.vue'
import DrawDetailDialog from '@/components/DrawDetailDialog.vue'
import HistoryTab from '@/components/HistoryTab.vue'
import OverviewTab from '@/components/OverviewTab.vue'
import type { LotteryDrawDetail } from '@/types/lottery'
import { getErrorMessage } from '@/utils/lotteryFormat'

type ActiveTab = 'overview' | 'history' | 'analyze'

const route = useRoute()
const router = useRouter()

const tabs: Array<{ id: ActiveTab; label: string }> = [
  { id: 'overview', label: '概览' },
  { id: 'history', label: '历史开奖' },
  { id: 'analyze', label: '号码分析' },
]

const activeTab = ref<ActiveTab>(normalizeTab(route.query.tab))
const ticketDropdownOpen = ref(false)
const detailOpen = ref(false)
const detailDraw = ref<LotteryDrawDetail | null>(null)
const detailLoading = ref(false)
const detailError = ref('')
const detailIssueNo = ref('')

const activeTabLabel = computed(() => tabs.find((tab) => tab.id === activeTab.value)?.label ?? '概览')

/**
 * 规范化 URL 中的 tab 参数，非法或缺失时回到概览页。
 */
function normalizeTab(tab: unknown): ActiveTab {
  return tab === 'history' || tab === 'analyze' || tab === 'overview' ? tab : 'overview'
}

/**
 * 切换页内标签，并同步到查询参数，支持刷新和复制链接。
 */
function changeTab(tab: ActiveTab) {
  activeTab.value = tab
  void router.replace({
    path: '/lottery-assistant',
    query: { ...route.query, tab },
  })
}

/**
 * 打开奖项详情弹窗；如果调用方已有详情数据，则直接复用，避免重复请求。
 */
async function openDetail(issueNo: string, presetDraw?: LotteryDrawDetail) {
  detailOpen.value = true
  detailIssueNo.value = issueNo
  detailError.value = ''

  if (presetDraw) {
    detailDraw.value = presetDraw
    return
  }

  await loadDetail(issueNo)
}

/**
 * 按期号加载开奖详情，用于历史列表、期号直查和弹窗重试。
 */
async function loadDetail(issueNo = detailIssueNo.value) {
  if (!issueNo) {
    return
  }

  detailLoading.value = true
  detailError.value = ''
  detailDraw.value = null

  try {
    detailDraw.value = await fetchDltDrawDetail(issueNo)
  } catch (err) {
    detailError.value = getErrorMessage(err)
  } finally {
    detailLoading.value = false
  }
}

watch(
  () => route.query.tab,
  (tab) => {
    activeTab.value = normalizeTab(tab)
  },
)
</script>

<template>
  <div class="page-shell">
    <header class="app-header">
      <div class="app-header__inner">
        <div class="top-row">
          <div>
            <div class="brand">
              <div class="brand-icon">≋</div>
              <h1>彩票助手</h1>
            </div>
            <p>查看开奖数据，分析号码历史命中情况</p>
          </div>

          <div class="header-actions">
            <div class="ticket-selector">
              <span>票种</span>
              <button
                class="ticket-button"
                type="button"
                @click="ticketDropdownOpen = !ticketDropdownOpen"
              >
                <span class="ticket-button__label">大乐透</span>
                <span
                  class="ticket-button__arrow"
                  :class="{ rotated: ticketDropdownOpen }"
                  aria-hidden="true"
                ></span>
              </button>

              <div v-if="ticketDropdownOpen" class="dropdown-mask" @click="ticketDropdownOpen = false"></div>
              <div v-if="ticketDropdownOpen" class="ticket-menu">
                <button type="button" class="ticket-menu__active" @click="ticketDropdownOpen = false">
                  大乐透 <span>✓</span>
                </button>
                <button type="button" disabled>双色球 <span>敬请期待</span></button>
                <button type="button" disabled>福彩3D <span>敬请期待</span></button>
              </div>
            </div>
          </div>
        </div>

        <nav class="tab-nav" :aria-label="`当前标签：${activeTabLabel}`">
          <button
            v-for="tab in tabs"
            :key="tab.id"
            type="button"
            :class="{ active: activeTab === tab.id }"
            @click="changeTab(tab.id)"
          >
            {{ tab.label }}
          </button>
        </nav>
      </div>
    </header>

    <main>
      <OverviewTab
        v-if="activeTab === 'overview'"
        @view-history="changeTab('history')"
        @open-detail="openDetail"
      />
      <HistoryTab v-else-if="activeTab === 'history'" @open-detail="openDetail" />
      <AnalyzeTab v-else />
    </main>

    <DrawDetailDialog
      :open="detailOpen"
      :draw="detailDraw"
      :loading="detailLoading"
      :error="detailError"
      @close="detailOpen = false"
      @retry="loadDetail()"
    />
  </div>
</template>

<style scoped>
.app-header {
  border-bottom: 1px solid #e2e8f0;
  background: #ffffff;
}

.app-header__inner {
  max-width: 1440px;
  margin: 0 auto;
  padding: 24px 24px 0;
}

.top-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 24px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.brand-icon {
  display: flex;
  width: 32px;
  height: 32px;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  color: #ffffff;
  box-shadow: 0 6px 16px rgb(37 99 235 / 0.22);
  font-size: 20px;
  font-weight: 900;
}

.brand h1 {
  margin: 0;
  background: linear-gradient(90deg, #1e3a8a 0%, #3b82f6 100%);
  background-clip: text;
  color: transparent;
  font-size: 24px;
  font-weight: 900;
  letter-spacing: 0;
}

.top-row p {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 14px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 24px;
}

.ticket-selector {
  position: relative;
  z-index: 10;
  display: flex;
  align-items: center;
  gap: 8px;
}

.ticket-selector > span {
  color: #64748b;
  font-size: 14px;
}

.ticket-button {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 34px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #f8fafc;
  color: #334155;
  padding: 0 12px;
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
}

.ticket-button__label {
  display: inline-flex;
  align-items: center;
  height: 20px;
  line-height: 20px;
}

.ticket-button__arrow {
  display: inline-flex;
  position: relative;
  flex: 0 0 20px;
  width: 20px;
  height: 20px;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  background: #f1f5f9;
  transition: transform 0.2s;
}

.ticket-button__arrow::before {
  content: "";
  width: 6px;
  height: 6px;
  border-right: 2px solid #94a3b8;
  border-bottom: 2px solid #94a3b8;
  transform: translateY(-1px) rotate(45deg);
}

.ticket-button__arrow.rotated {
  transform: rotate(180deg);
}

.dropdown-mask {
  position: fixed;
  inset: 0;
  z-index: 30;
}

.ticket-menu {
  position: absolute;
  top: calc(100% + 6px);
  right: 0;
  z-index: 40;
  width: 148px;
  overflow: hidden;
  border: 1px solid #f1f5f9;
  border-radius: 14px;
  background: #ffffff;
  box-shadow: 0 16px 40px rgb(15 23 42 / 0.12);
}

.ticket-menu button {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  border: 0;
  background: #ffffff;
  color: #64748b;
  padding: 10px 12px;
  font-size: 14px;
  text-align: left;
}

.ticket-menu button:not(:disabled) {
  cursor: pointer;
}

.ticket-menu button:disabled {
  color: #cbd5e1;
  cursor: not-allowed;
}

.ticket-menu button span {
  font-size: 11px;
}

.ticket-menu__active {
  color: #2563eb !important;
  background: #eff6ff !important;
  font-weight: 800;
}

.tab-nav {
  display: flex;
  gap: 32px;
}

.tab-nav button {
  border: 0;
  border-bottom: 2px solid transparent;
  background: transparent;
  color: #64748b;
  padding: 0 2px 16px;
  font-size: 14px;
  font-weight: 800;
}

.tab-nav button:hover {
  color: #111827;
}

.tab-nav button.active {
  border-bottom-color: #2563eb;
  color: #2563eb;
}

@media (max-width: 700px) {
  .app-header__inner {
    padding: 18px 16px 0;
  }

  .top-row {
    flex-direction: column;
  }

  .tab-nav {
    gap: 20px;
    overflow-x: auto;
  }
}
</style>
