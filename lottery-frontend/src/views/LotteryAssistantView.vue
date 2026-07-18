<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { fetchCurrentUser, logout } from '@/api/auth'
import { fetchDltDrawDetail } from '@/api/lottery'
import AnalyzeTab from '@/components/AnalyzeTab.vue'
import DrawDetailDialog from '@/components/DrawDetailDialog.vue'
import HistoryTab from '@/components/HistoryTab.vue'
import OverviewTab from '@/components/OverviewTab.vue'
import type { CurrentUser } from '@/types/auth'
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
const currentUser = ref<CurrentUser | null>(null)
const authLoading = ref(false)

const activeTabLabel = computed(() => tabs.find((tab) => tab.id === activeTab.value)?.label ?? '概览')
const userInitial = computed(() => currentUser.value?.nickname?.slice(0, 1) || '用')

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

/**
 * 页面初始化时尝试恢复登录态；游客访问公共页面时静默忽略未登录错误。
 */
async function loadCurrentUser() {
  authLoading.value = true
  try {
    currentUser.value = await fetchCurrentUser()
  } catch {
    currentUser.value = null
  } finally {
    authLoading.value = false
  }
}

/**
 * 清理当前登录会话，并将顶部入口恢复成游客状态。
 */
async function handleLogout() {
  authLoading.value = true
  try {
    await logout()
    currentUser.value = null
  } finally {
    authLoading.value = false
  }
}

function goLogin() {
  void router.push('/login')
}

watch(
  () => route.query.tab,
  (tab) => {
    activeTab.value = normalizeTab(tab)
  },
)

onMounted(loadCurrentUser)
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
            <span class="header-divider" aria-hidden="true"></span>
            <button
              v-if="!currentUser"
              class="login-entry-button"
              type="button"
              :disabled="authLoading"
              @click="goLogin"
            >
              <svg class="login-entry-button__icon" viewBox="0 0 1024 1024" aria-hidden="true">
                <path
                  d="M960.853333 903.816533a463.633067 463.633067 0 0 0-11.264-39.185066c-1.536-4.539733-3.413333-8.942933-5.051733-13.448534a484.078933 484.078933 0 0 0-9.557333-24.4736c-2.2528-5.188267-4.881067-10.274133-7.338667-15.394133-3.413333-7.099733-6.8608-14.165333-10.6496-21.0944-2.901333-5.3248-6.075733-10.513067-9.181867-15.701333-2.423467-4.061867-4.573867-8.226133-7.133866-12.219734-1.604267-2.4576-3.413333-4.778667-5.0176-7.202133-1.501867-2.218667-2.730667-4.608-4.266667-6.792533-0.4096-0.6144-1.058133-0.887467-1.501867-1.4336a461.482667 461.482667 0 0 0-90.385066-96.768c-13.5168-10.786133-27.7504-20.48-42.257067-29.5936-0.477867-0.341333-0.7168-0.8192-1.194667-1.1264-3.6864-2.286933-7.509333-4.3008-11.264-6.485334-4.266667-2.491733-8.4992-5.051733-12.868266-7.441066-6.826667-3.6864-13.789867-7.099733-20.753067-10.478934-3.618133-1.7408-7.202133-3.618133-10.8544-5.290666a449.194667 449.194667 0 0 0-31.607467-12.731734c-0.7168-0.273067-1.365333-0.6144-2.082133-0.8192-3.140267-1.1264-6.417067-1.911467-9.557333-2.935466-4.164267-1.399467-8.328533-2.833067-12.561067-4.096a259.9936 259.9936 0 0 0 129.194667-225.450667 260.061867 260.061867 0 0 0-76.629334-185.002667 259.9936 259.9936 0 0 0-185.002666-76.629333H512h-0.034133a259.857067 259.857067 0 0 0-185.002667 76.629333 259.925333 259.925333 0 0 0-76.629333 185.002667 259.584 259.584 0 0 0 76.629333 185.002667c15.906133 15.940267 33.655467 29.2864 52.565333 40.448-4.266667 1.262933-8.430933 2.730667-12.663466 4.096-3.140267 1.058133-6.3488 1.8432-9.489067 2.935466-0.7168 0.238933-1.365333 0.580267-2.048 0.8192-10.683733 3.822933-21.265067 8.0896-31.675733 12.765867-3.584 1.604267-7.0656 3.4816-10.615467 5.154133-7.099733 3.413333-14.165333 6.826667-21.0944 10.615467-4.266667 2.321067-8.3968 4.8128-12.561067 7.2704-3.822933 2.218667-7.748267 4.266667-11.502933 6.621867-0.512 0.3072-0.750933 0.8192-1.2288 1.160533-14.506667 9.147733-28.706133 18.807467-42.222933 29.559467a459.6736 459.6736 0 0 0-90.385067 96.768c-0.443733 0.546133-1.092267 0.8192-1.501867 1.4336-1.536 2.184533-2.7648 4.573867-4.266666 6.792533-1.604267 2.423467-3.447467 4.744533-5.0176 7.202133-2.56 3.9936-4.7104 8.157867-7.133867 12.219734-3.106133 5.188267-6.280533 10.376533-9.181867 15.701333-3.7888 6.929067-7.202133 13.994667-10.6496 21.0944-2.4576 5.12-5.051733 10.205867-7.338666 15.394133-3.515733 8.021333-6.519467 16.247467-9.557334 24.4736-1.672533 4.5056-3.549867 8.9088-5.051733 13.448534-4.3008 12.868267-8.0896 25.941333-11.264 39.185066-3.072 12.970667 2.594133 25.770667 13.073067 32.802134a31.3344 31.3344 0 0 0 9.966933 4.608 30.9248 30.9248 0 0 0 34.030933-15.2576 30.446933 30.446933 0 0 0 3.345067-7.7824c2.833067-11.844267 6.178133-23.483733 10.0352-34.9184 0.6144-1.8432 1.399467-3.549867 2.013867-5.358934 3.447467-9.762133 7.133867-19.456 11.332266-28.945066 0.512-1.160533 1.1264-2.2528 1.6384-3.447467 4.7104-10.308267 9.728-20.48 15.291734-30.344533l0.068266-0.1024a402.773333 402.773333 0 0 1 19.694934-31.4368l0.136533-0.375467a397.4144 397.4144 0 0 1 116.599467-111.2064c0.136533-0.1024 0.3072-0.068267 0.443733-0.170667a397.824 397.824 0 0 1 94.993067-42.973866c2.7648-0.8192 5.495467-1.7408 8.2944-2.491734 5.7344-1.604267 11.5712-3.003733 17.373866-4.334933a367.8208 367.8208 0 0 1 47.342934-7.953067c3.8912-0.443733 7.7824-0.9216 11.6736-1.2288 10.410667-0.785067 20.8896-1.3312 31.505066-1.3312s21.060267 0.546133 31.505067 1.3312c3.8912 0.3072 7.816533 0.785067 11.707733 1.2288a361.3696 361.3696 0 0 1 47.240534 7.953067c5.870933 1.3312 11.707733 2.730667 17.5104 4.334933 2.696533 0.750933 5.358933 1.6384 8.021333 2.4576 33.348267 10.103467 65.365333 24.405333 95.197867 43.008 0.136533 0.1024 0.3072 0.068267 0.443733 0.170667a396.151467 396.151467 0 0 1 116.599467 111.2064c0.1024 0.136533 0.1024 0.273067 0.170666 0.375467 13.687467 19.7632 25.3952 40.5504 35.191467 62.1568l1.467733 3.037866c4.3008 9.659733 8.055467 19.592533 11.605334 29.5936 0.546133 1.604267 1.2288 3.106133 1.774933 4.7104 3.822933 11.4688 7.236267 23.176533 10.0352 35.0208a31.061333 31.061333 0 0 0 60.450133-14.336z m-249.275733-560.2304A199.850667 199.850667 0 0 1 512 543.197867a199.850667 199.850667 0 0 1-199.5776-199.611734A199.816533 199.816533 0 0 1 512 144.008533a199.816533 199.816533 0 0 1 199.5776 199.5776z"
                  fill="currentColor"
                />
              </svg>
              <span>登录</span>
            </button>
            <div v-else class="user-session">
              <span class="user-session__avatar" aria-hidden="true">
                <img v-if="currentUser.avatarUrl" :src="currentUser.avatarUrl" alt="" />
                <span v-else>{{ userInitial }}</span>
              </span>
              <span class="user-session__name">{{ currentUser.nickname }}</span>
              <button class="logout-button" type="button" :disabled="authLoading" @click="handleLogout">
                退出
              </button>
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
  gap: 16px;
}

.header-divider {
  width: 1px;
  height: 24px;
  background: #e2e8f0;
}

.login-entry-button {
  display: inline-flex;
  height: 34px;
  align-items: center;
  gap: 8px;
  border: 1px solid #bfdbfe;
  border-radius: 10px;
  background: #eff6ff;
  color: #1d4ed8;
  padding: 0 13px 0 10px;
  font-size: 14px;
  font-weight: 900;
  cursor: pointer;
  box-shadow: 0 4px 12px rgb(37 99 235 / 0.08);
  transition:
    background 0.18s,
    border-color 0.18s,
    box-shadow 0.18s,
    color 0.18s,
    transform 0.18s;
}

.login-entry-button:hover {
  border-color: #2563eb;
  background: #2563eb;
  color: #ffffff;
  box-shadow: 0 10px 24px rgb(37 99 235 / 0.2);
  transform: translateY(-1px);
}

.login-entry-button:disabled,
.logout-button:disabled {
  cursor: not-allowed;
  opacity: 0.62;
}

.login-entry-button:focus-visible {
  outline: 3px solid #bfdbfe;
  outline-offset: 2px;
}

.login-entry-button__icon {
  display: inline-flex;
  width: 18px;
  height: 18px;
  flex: 0 0 18px;
}

.user-session {
  display: inline-flex;
  height: 36px;
  align-items: center;
  gap: 8px;
  border: 1px solid #dbeafe;
  border-radius: 999px;
  background: #ffffff;
  padding: 3px 5px 3px 4px;
  box-shadow: 0 6px 18px rgb(15 23 42 / 0.06);
}

.user-session__avatar {
  display: inline-flex;
  width: 28px;
  height: 28px;
  flex: 0 0 28px;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 50%;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 13px;
  font-weight: 900;
}

.user-session__avatar img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-session__name {
  max-width: 120px;
  overflow: hidden;
  color: #0f172a;
  font-size: 14px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.logout-button {
  height: 28px;
  border: 0;
  border-radius: 999px;
  background: #f1f5f9;
  color: #64748b;
  padding: 0 10px;
  font-size: 12px;
  font-weight: 900;
  cursor: pointer;
  transition:
    background 0.18s,
    color 0.18s;
}

.logout-button:hover:not(:disabled) {
  background: #fee2e2;
  color: #b91c1c;
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
