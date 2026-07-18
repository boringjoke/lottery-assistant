<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'

import { ApiError } from '@/api/http'
import { fetchDltDrawPage, fetchLatestDltDraw } from '@/api/lottery'
import LotteryNumberGroup from '@/components/LotteryNumberGroup.vue'
import StateMessage from '@/components/StateMessage.vue'
import type { LotteryDrawDetail, LotteryDrawSummary } from '@/types/lottery'
import { formatCountdown, formatCurrency, getErrorMessage, getNextDltDrawTime } from '@/utils/lotteryFormat'

const emit = defineEmits<{
  viewHistory: []
  openDetail: [issueNo: string, draw?: LotteryDrawDetail]
}>()

const latestDraw = ref<LotteryDrawDetail | null>(null)
const recentDraws = ref<LotteryDrawSummary[]>([])
const loading = ref(false)
const error = ref('')
const currentTime = ref(new Date())
const nextDrawTime = computed(() => getNextDltDrawTime(currentTime.value))
const nextDrawCountdown = computed(() =>
  formatCountdown(nextDrawTime.value.targetTimeMillis, currentTime.value),
)
let countdownTimer: ReturnType<typeof setInterval> | undefined

/**
 * 数据库尚未同步开奖时，最新开奖接口可能返回 404；概览页应展示空状态而不是失败页。
 */
async function fetchLatestDltDrawSafely(): Promise<LotteryDrawDetail | null> {
  try {
    return await fetchLatestDltDraw()
  } catch (err) {
    if (err instanceof ApiError && err.status === 404) {
      return null
    }

    throw err
  }
}

/**
 * 同时加载最新开奖和最近 5 期开奖，保证概览页两块数据同步刷新。
 */
async function loadOverview() {
  loading.value = true
  error.value = ''

  try {
    const [latest, recentPage] = await Promise.all([
      fetchLatestDltDrawSafely(),
      fetchDltDrawPage({ pageNo: 1, pageSize: 5 }),
    ])
    latestDraw.value = latest
    recentDraws.value = recentPage.draws
  } catch (err) {
    error.value = getErrorMessage(err)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadOverview()
  countdownTimer = setInterval(() => {
    currentTime.value = new Date()
  }, 1000)
})

onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
})
</script>

<template>
  <div class="content-container overview-layout">
    <StateMessage
      v-if="loading"
      class="card"
      title="正在加载开奖概览"
      message="请稍候，正在读取最新开奖和近期开奖记录。"
    />

    <StateMessage
      v-else-if="error"
      class="card"
      title="开奖概览加载失败"
      :message="error"
      action-label="重试"
      @action="loadOverview"
    />

    <template v-else>
      <div class="top-grid">
        <section class="card latest-card">
          <div class="card-accent"></div>
          <div v-if="latestDraw" class="latest-card__content">
            <div class="latest-card__header">
              <h2 class="section-title">最近开奖</h2>
              <span class="issue-badge">大乐透 第 {{ latestDraw.issueNo }} 期</span>
            </div>

            <LotteryNumberGroup
              class="latest-numbers"
              :front-numbers="latestDraw.frontNumbers"
              :back-numbers="latestDraw.backNumbers"
              size="lg"
            />

            <div class="amount-grid">
              <div class="metric-card">
                <span>奖池金额 (元)</span>
                <strong>{{ formatCurrency(latestDraw.poolBalance) }}</strong>
              </div>
              <div class="metric-card">
                <span>销售金额 (元)</span>
                <strong>{{ formatCurrency(latestDraw.salesAmount) }}</strong>
              </div>
            </div>

            <div class="latest-meta">
              <span>开奖日期：{{ latestDraw.drawDate }}</span>
              <button class="ghost-button" type="button" @click="emit('openDetail', latestDraw.issueNo, latestDraw)">
                查看详情
              </button>
            </div>
          </div>
          <StateMessage v-else title="暂无最新开奖数据" message="请先完成开奖数据同步。" />
        </section>

        <section class="next-card">
          <div class="next-card__shade next-card__shade--top"></div>
          <div class="next-card__shade next-card__shade--bottom"></div>
          <h2 class="section-title section-title--light">下次开奖</h2>
          <p>{{ nextDrawTime.source }}</p>
          <div class="next-draw-state">
            <div class="countdown-grid" aria-label="下次开奖倒计时">
              <div class="countdown-item">
                <strong>{{ nextDrawCountdown.days }}</strong>
                <span>天</span>
              </div>
              <div class="countdown-item">
                <strong>{{ nextDrawCountdown.hours }}</strong>
                <span>时</span>
              </div>
              <div class="countdown-item">
                <strong>{{ nextDrawCountdown.minutes }}</strong>
                <span>分</span>
              </div>
              <div class="countdown-item">
                <strong>{{ nextDrawCountdown.seconds }}</strong>
                <span>秒</span>
              </div>
            </div>
            <span class="next-draw-state__time">
              推算开奖时间：{{ nextDrawTime.drawDate }} {{ nextDrawTime.weekday }} {{ nextDrawTime.estimatedTime }}
            </span>
          </div>
          <p class="next-card__notice">实际开奖安排以中国体彩网官方公告为准。</p>
        </section>
      </div>

      <section class="card history-card">
        <div class="history-card__header">
          <h2 class="section-title">最近 5 期开奖</h2>
          <button class="ghost-button" type="button" @click="emit('viewHistory')">
            查看全部历史开奖 ›
          </button>
        </div>

        <div class="table-scroll">
          <table v-if="recentDraws.length" class="data-table recent-draw-table">
            <thead>
              <tr>
                <th>期号</th>
                <th>开奖日期</th>
                <th>开奖号码</th>
                <th class="operation-cell">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="draw in recentDraws" :key="draw.issueNo">
                <td>
                  <strong>{{ draw.issueNo }}</strong>
                </td>
                <td>{{ draw.drawDate }}</td>
                <td>
                  <LotteryNumberGroup :front-numbers="draw.frontNumbers" :back-numbers="draw.backNumbers" />
                </td>
                <td class="operation-cell">
                  <button class="ghost-button" type="button" @click="emit('openDetail', draw.issueNo)">
                    详情
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
          <StateMessage v-else title="暂无近期开奖记录" />
        </div>
      </section>

      <p class="data-note overview-note">开奖数据仅供参考，最终结果以中国体彩网官方公布为准。</p>
    </template>
  </div>
</template>

<style scoped>
.overview-layout {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.top-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(360px, 1fr);
  gap: 24px;
}

.latest-card,
.next-card {
  position: relative;
  min-height: 300px;
  overflow: hidden;
  padding: 24px;
}

.card-accent {
  position: absolute;
  top: 0;
  right: 0;
  width: 240px;
  height: 240px;
  border-bottom-left-radius: 999px;
  background: linear-gradient(225deg, #eff6ff 0%, rgb(239 246 255 / 0) 70%);
  pointer-events: none;
}

.latest-card__content {
  position: relative;
  z-index: 1;
}

.latest-card__header,
.history-card__header,
.latest-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.history-card__header {
  margin-bottom: 20px;
}

.issue-badge {
  border: 1px solid #dbeafe;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  padding: 4px 12px;
  font-size: 14px;
  font-weight: 800;
}

.latest-numbers {
  margin-top: 28px;
  margin-bottom: 28px;
}

.amount-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.metric-card {
  padding: 16px;
  border: 1px solid #dbeafe;
  border-radius: 14px;
  background: linear-gradient(135deg, #eff6ff 0%, #ffffff 100%);
}

.metric-card span {
  display: block;
  margin-bottom: 4px;
  color: #2563eb;
  font-size: 14px;
  font-weight: 700;
}

.metric-card strong {
  color: #1e3a8a;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace;
  font-size: 24px;
  font-weight: 900;
}

.latest-meta {
  margin-top: 24px;
  color: #94a3b8;
  font-size: 13px;
}

.next-card {
  display: flex;
  flex-direction: column;
  border: 1px solid #3b82f6;
  border-radius: 16px;
  background: linear-gradient(135deg, #2563eb 0%, #1e40af 100%);
  box-shadow: 0 12px 30px rgb(37 99 235 / 0.18);
}

.next-card__shade {
  position: absolute;
  border-radius: 999px;
  pointer-events: none;
}

.next-card__shade--top {
  top: -80px;
  right: -80px;
  width: 240px;
  height: 240px;
  background: rgb(96 165 250 / 0.24);
}

.next-card__shade--bottom {
  bottom: -90px;
  left: -90px;
  width: 220px;
  height: 220px;
  background: rgb(30 64 175 / 0.45);
}

.section-title--light {
  color: #ffffff;
}

.section-title--light::before {
  background: #93c5fd;
}

.next-card p {
  position: relative;
  z-index: 1;
  margin: 8px 0 0 16px;
  color: #dbeafe;
  font-size: 14px;
}

.next-draw-state {
  position: relative;
  z-index: 1;
  display: flex;
  flex: 1;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  text-align: center;
}

.countdown-grid {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.countdown-item {
  display: flex;
  width: 76px;
  min-height: 82px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 1px solid rgb(255 255 255 / 0.24);
  border-radius: 14px;
  background: rgb(255 255 255 / 0.12);
  box-shadow: inset 0 1px 0 rgb(255 255 255 / 0.18);
}

.countdown-item strong {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace;
  font-size: 34px;
  font-weight: 900;
  line-height: 1;
  letter-spacing: 0;
}

.countdown-item span {
  margin-top: 8px;
  color: #bfdbfe;
  font-size: 13px;
  font-weight: 800;
}

.next-draw-state__time {
  margin-top: 14px;
  color: #dbeafe;
  font-size: 13px;
  font-weight: 700;
}

.next-card__notice {
  margin-top: 0 !important;
  color: #bfdbfe !important;
  font-size: 13px !important;
}

.history-card {
  padding: 24px;
}

.recent-draw-table {
  border-collapse: separate;
  border-spacing: 0;
}

.recent-draw-table th {
  border-bottom-color: #e2e8f0;
  background: #f8fafc;
  padding: 12px;
  text-align: center;
}

.recent-draw-table th:first-child {
  border-top-left-radius: 10px;
}

.recent-draw-table th:last-child {
  border-top-right-radius: 10px;
}

.recent-draw-table td {
  border-bottom-color: #eef2f7;
  text-align: center;
}

.operation-cell {
  text-align: right;
}

.recent-draw-table .operation-cell {
  text-align: center;
}

.overview-note {
  justify-content: center;
}

@media (max-width: 960px) {
  .top-grid,
  .amount-grid {
    grid-template-columns: 1fr;
  }

  .next-card {
    min-height: 220px;
  }
}

@media (max-width: 640px) {
  .latest-card__header,
  .history-card__header,
  .latest-meta {
    align-items: flex-start;
    flex-direction: column;
  }

  .countdown-grid {
    width: 100%;
    gap: 8px;
  }

  .countdown-item {
    width: 25%;
    min-width: 0;
  }

  .countdown-item strong {
    font-size: 28px;
  }
}
</style>
