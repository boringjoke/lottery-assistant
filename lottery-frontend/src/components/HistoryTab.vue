<script setup lang="ts">
import { onMounted, ref } from 'vue'

import { fetchDltDrawPage } from '@/api/lottery'
import LotteryNumberGroup from '@/components/LotteryNumberGroup.vue'
import StateMessage from '@/components/StateMessage.vue'
import type { LotteryDrawPage } from '@/types/lottery'
import { formatCurrency, getErrorMessage } from '@/utils/lotteryFormat'

const emit = defineEmits<{
  openDetail: [issueNo: string]
}>()

const pageNo = ref(1)
const pageSize = ref(20)
const issueNo = ref('')
const startDate = ref('')
const endDate = ref('')
const pageData = ref<LotteryDrawPage | null>(null)
const loading = ref(false)
const error = ref('')

/**
 * 按当前分页参数加载历史开奖记录列表。
 */
async function loadHistory() {
  loading.value = true
  error.value = ''

  try {
    pageData.value = await fetchDltDrawPage({
      pageNo: pageNo.value,
      pageSize: pageSize.value,
      issueNo: issueNo.value.trim() || undefined,
      startDate: startDate.value || undefined,
      endDate: endDate.value || undefined,
    })
  } catch (err) {
    error.value = getErrorMessage(err)
  } finally {
    loading.value = false
  }
}

/**
 * 根据期号和日期范围查询列表，结果直接展示在下方表格中。
 */
function searchDraws() {
  pageNo.value = 1
  void loadHistory()
}

/**
 * 清空查询条件并回到默认分页列表。
 */
function resetSearch() {
  issueNo.value = ''
  startDate.value = ''
  endDate.value = ''
  pageNo.value = 1
  pageSize.value = 20
  void loadHistory()
}

/**
 * 切换分页页码，并防止跳转到无效页。
 */
function changePage(nextPageNo: number) {
  if (!pageData.value || nextPageNo < 1 || nextPageNo > pageData.value.pages) {
    return
  }
  pageNo.value = nextPageNo
  void loadHistory()
}

/**
 * 修改每页条数后回到第一页，避免旧页码超出新分页范围。
 */
function changePageSize() {
  pageNo.value = 1
  void loadHistory()
}

onMounted(loadHistory)
</script>

<template>
  <div class="content-container history-layout">
    <section class="card filter-card">
      <div class="filter-heading">
        <h2 class="section-title">历史开奖查询</h2>
        <p>支持按期号和开奖日期范围查询</p>
      </div>

      <form class="filter-row" @submit.prevent="searchDraws">
        <div class="issue-field">
          <label class="field-label" for="issueNo">期号</label>
          <input
            id="issueNo"
            v-model="issueNo"
            class="text-input"
            type="text"
            placeholder="请输入期号"
          />
        </div>

        <div class="date-field">
          <label class="field-label" for="startDate">开始日期</label>
          <div class="date-input-wrap">
            <input id="startDate" v-model="startDate" class="text-input date-input" type="date" />
            <span class="calendar-indicator" aria-hidden="true">📅</span>
          </div>
        </div>

        <div class="date-field">
          <label class="field-label" for="endDate">结束日期</label>
          <div class="date-input-wrap">
            <input id="endDate" v-model="endDate" class="text-input date-input" type="date" />
            <span class="calendar-indicator" aria-hidden="true">📅</span>
          </div>
        </div>

        <div class="filter-actions">
          <button class="secondary-button filter-action-button" type="button" @click="resetSearch">
            <span class="button-icon" aria-hidden="true">↻</span>
            重置
          </button>
          <button class="primary-button filter-action-button" type="submit">
            <span class="button-icon" aria-hidden="true">⌕</span>
            查询
          </button>
        </div>
      </form>
    </section>

    <section class="card list-card">
      <div class="list-card__header">
        <h2 class="section-title">开奖记录列表</h2>
        <p class="data-note">开奖数据仅供参考，最终结果以中国体彩网官方公布为准。</p>
      </div>

      <StateMessage v-if="loading" title="正在加载开奖记录" />
      <StateMessage
        v-else-if="error"
        title="开奖记录加载失败"
        :message="error"
        action-label="重试"
        @action="loadHistory"
      />
      <StateMessage
        v-else-if="!pageData || pageData.draws.length === 0"
        title="暂无开奖记录"
        message="请先完成开奖数据同步。"
      />

      <template v-else>
        <div class="table-scroll">
          <table class="data-table">
            <thead>
              <tr>
                <th>期号</th>
                <th>开奖日期</th>
                <th>开奖号码</th>
                <th>奖池金额</th>
                <th>销售金额</th>
                <th class="operation-cell">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="draw in pageData.draws" :key="draw.issueNo">
                <td>
                  <strong>{{ draw.issueNo }}</strong>
                </td>
                <td>{{ draw.drawDate }}</td>
                <td>
                  <LotteryNumberGroup :front-numbers="draw.frontNumbers" :back-numbers="draw.backNumbers" />
                </td>
                <td class="money-cell">{{ formatCurrency(draw.poolBalance) }}</td>
                <td class="money-cell">{{ formatCurrency(draw.salesAmount) }}</td>
                <td class="operation-cell">
                  <button class="ghost-button" type="button" @click="emit('openDetail', draw.issueNo)">
                    详情
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="pagination-bar">
          <div class="total-text">
            共 <strong>{{ pageData.total }}</strong> 条数据
          </div>
          <div class="pagination-controls">
            <button
              class="page-button"
              type="button"
              :disabled="pageNo <= 1"
              @click="changePage(pageNo - 1)"
            >
              ‹
            </button>
            <span class="page-current">{{ pageNo }} / {{ pageData.pages || 1 }}</span>
            <button
              class="page-button"
              type="button"
              :disabled="pageNo >= pageData.pages"
              @click="changePage(pageNo + 1)"
            >
              ›
            </button>
            <select v-model.number="pageSize" class="select-input page-size-select" @change="changePageSize">
              <option :value="10">10 条/页</option>
              <option :value="20">20 条/页</option>
              <option :value="50">50 条/页</option>
            </select>
          </div>
        </div>
      </template>
    </section>
  </div>
</template>

<style scoped>
.history-layout {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.filter-card,
.list-card {
  padding: 24px;
}

.filter-heading p {
  margin: 6px 0 0 16px;
  color: #64748b;
  font-size: 14px;
}

.filter-row {
  display: flex;
  align-items: flex-end;
  gap: 16px;
  margin-top: 24px;
}

.issue-field {
  width: min(260px, 100%);
}

.date-field {
  width: min(220px, 100%);
}

.date-input-wrap {
  position: relative;
}

.date-input {
  padding-right: 48px;
}

.date-input::-webkit-calendar-picker-indicator {
  position: absolute;
  right: 0;
  width: 46px;
  height: 100%;
  opacity: 0;
  cursor: pointer;
}

.calendar-indicator {
  position: absolute;
  top: 50%;
  right: 4px;
  display: flex;
  width: 36px;
  height: 32px;
  align-items: center;
  justify-content: center;
  transform: translateY(-50%);
  border-left: 1px solid #e2e8f0;
  color: #2563eb;
  font-size: 17px;
  pointer-events: none;
}

.filter-actions {
  display: flex;
  gap: 12px;
  margin-left: auto;
}

.filter-action-button {
  min-height: 44px;
  padding: 0 24px;
  font-size: 15px;
}

.button-icon {
  display: inline-flex;
  width: 19px;
  height: 19px;
  align-items: center;
  justify-content: center;
  margin-right: 8px;
  font-size: 19px;
  line-height: 1;
  transform: translateY(-1px);
}

.list-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 20px;
}

.operation-cell {
  text-align: right;
}

.money-cell {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace;
}

.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #f1f5f9;
}

.total-text {
  color: #64748b;
  font-size: 14px;
}

.total-text strong {
  color: #111827;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 10px;
}

.page-button {
  width: 34px;
  height: 34px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #ffffff;
  color: #475569;
  font-size: 20px;
  font-weight: 800;
}

.page-button:hover:not(:disabled) {
  background: #f8fafc;
}

.page-button:disabled {
  color: #cbd5e1;
}

.page-current {
  min-width: 72px;
  color: #334155;
  font-size: 14px;
  font-weight: 800;
  text-align: center;
}

.page-size-select {
  width: 116px;
}

@media (max-width: 780px) {
  .filter-row,
  .list-card__header,
  .pagination-bar {
    align-items: stretch;
    flex-direction: column;
  }

  .filter-actions {
    margin-left: 0;
  }

  .filter-actions > button {
    flex: 1;
  }
}
</style>
