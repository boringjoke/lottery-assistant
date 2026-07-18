<script setup lang="ts">
import { ref } from 'vue'

import type {
  LotteryDateRangeSyncRequest,
  LotteryHistorySyncRequest,
  LotteryIssueRangeSyncRequest,
} from '@/types/lottery'

defineProps<{
  hasActiveTask: boolean
  submittingAction: string
  historyPageForm: { pageNo: number; pageSize: number }
  historyForm: LotteryHistorySyncRequest
  issueRangeForm: LotteryIssueRangeSyncRequest
  dateRangeForm: LotteryDateRangeSyncRequest
}>()

const emit = defineEmits<{
  submitLatest: []
  submitHistoryPage: []
  submitHistory: []
  submitIssueRange: []
  submitDateRange: []
}>()

const ticketDropdownOpen = ref(false)
const operationsCollapsed = ref(false)
</script>

<template>
  <section class="operation-panel">
    <div class="operation-panel__header">
      <div class="section-heading">
        <span></span>
        <div>
          <h2>同步操作</h2>
          <p>当前票种固定为大乐透；范围同步请保持低频、串行执行。</p>
        </div>
      </div>
      <button
        class="collapse-button"
        type="button"
        :aria-expanded="!operationsCollapsed"
        @click="operationsCollapsed = !operationsCollapsed"
      >
        <span>{{ operationsCollapsed ? '展开操作' : '收起操作' }}</span>
        <span
          class="collapse-button__arrow"
          :class="{ collapsed: operationsCollapsed }"
          aria-hidden="true"
        ></span>
      </button>
    </div>

    <div v-if="!operationsCollapsed" class="operation-panel__body">
      <div class="ticket-line">
        <label>票种</label>
        <div class="ticket-selector">
          <button
            type="button"
            class="ticket-button"
            @click="ticketDropdownOpen = !ticketDropdownOpen"
          >
            <span>大乐透</span>
            <span
              class="ticket-button__arrow"
              :class="{ rotated: ticketDropdownOpen }"
              aria-hidden="true"
            ></span>
          </button>
          <div
            v-if="ticketDropdownOpen"
            class="dropdown-mask"
            @click="ticketDropdownOpen = false"
          ></div>
          <div v-if="ticketDropdownOpen" class="ticket-menu">
            <button type="button" class="ticket-menu__active" @click="ticketDropdownOpen = false">
              大乐透 <span>✓</span>
            </button>
            <button type="button" disabled>双色球 <span>敬请期待</span></button>
            <button type="button" disabled>福彩3D <span>敬请期待</span></button>
          </div>
        </div>
        <span>首版仅开放大乐透，接口保留多票种扩展空间。</span>
      </div>

      <div class="operation-grid">
        <form class="operation-card operation-card--latest" @submit.prevent="emit('submitLatest')">
          <div>
            <h3>同步最新开奖</h3>
            <p>抓取并同步当前票种最新一期开奖数据。</p>
          </div>
          <button class="primary-button" type="submit" :disabled="hasActiveTask || submittingAction === 'latest'">
            {{ submittingAction === 'latest' ? '创建中' : '同步最新开奖' }}
          </button>
        </form>

        <form class="operation-card" @submit.prevent="emit('submitHistoryPage')">
          <div>
            <h3>同步历史分页</h3>
            <p>用于快速补一页历史开奖数据，任务会立即执行。</p>
          </div>
          <div class="field-row">
            <label for="historyPageNo">页码</label>
            <input id="historyPageNo" v-model.number="historyPageForm.pageNo" min="1" type="number" />
          </div>
          <div class="field-row">
            <label for="historyPageSize">每页数量</label>
            <input id="historyPageSize" v-model.number="historyPageForm.pageSize" min="1" max="50" type="number" />
          </div>
          <button
            class="primary-button"
            type="submit"
            :disabled="hasActiveTask || submittingAction === 'historyPage'"
          >
            {{ submittingAction === 'historyPage' ? '创建中' : '同步历史分页' }}
          </button>
        </form>

        <form
          class="operation-card"
          data-testid="history-sync-form"
          @submit.prevent="emit('submitHistory')"
        >
          <div>
            <h3>历史分页批量同步</h3>
            <p>从指定页开始连续扫描历史开奖。</p>
          </div>
          <div class="field-row">
            <label for="historyStartPage">起始页</label>
            <input id="historyStartPage" v-model.number="historyForm.startPage" min="1" type="number" />
          </div>
          <div class="field-row">
            <label for="historyPageSizeBatch">每页数量</label>
            <input id="historyPageSizeBatch" v-model.number="historyForm.pageSize" min="1" max="50" type="number" />
          </div>
          <div class="field-row">
            <label for="historyMaxPages">最大扫描页数</label>
            <input id="historyMaxPages" v-model.number="historyForm.maxPages" min="1" type="number" />
          </div>
          <div class="field-row">
            <label for="historyPageDelay">页间隔毫秒</label>
            <input id="historyPageDelay" v-model.number="historyForm.pageDelayMillis" min="0" type="number" />
          </div>
          <label class="check-row">
            <input v-model="historyForm.stopWhenLastPage" type="checkbox" />
            遇到最后一页停止
          </label>
          <button class="primary-button" type="submit" :disabled="hasActiveTask || submittingAction === 'history'">
            {{ submittingAction === 'history' ? '创建中' : '创建批量同步' }}
          </button>
        </form>

        <form
          class="operation-card"
          data-testid="issue-range-form"
          @submit.prevent="emit('submitIssueRange')"
        >
          <div>
            <h3>按期号范围同步</h3>
            <p>扫描历史分页并同步落在期号范围内的数据。</p>
          </div>
          <div class="field-row">
            <label for="issueStartIssueNo">起始期号</label>
            <input
              id="issueStartIssueNo"
              v-model="issueRangeForm.startIssueNo"
              placeholder="如 26070"
              type="text"
            />
          </div>
          <div class="field-row">
            <label for="issueEndIssueNo">结束期号</label>
            <input
              id="issueEndIssueNo"
              v-model="issueRangeForm.endIssueNo"
              placeholder="如 26076"
              type="text"
            />
          </div>
          <div class="field-row">
            <label for="issueStartPage">起始页</label>
            <input id="issueStartPage" v-model.number="issueRangeForm.startPage" min="1" type="number" />
          </div>
          <div class="field-row">
            <label for="issuePageSize">每页数量</label>
            <input id="issuePageSize" v-model.number="issueRangeForm.pageSize" min="1" max="50" type="number" />
          </div>
          <div class="field-row">
            <label for="issuePageDelay">页间隔毫秒</label>
            <input id="issuePageDelay" v-model.number="issueRangeForm.pageDelayMillis" min="0" type="number" />
          </div>
          <label class="check-row">
            <input v-model="issueRangeForm.stopWhenLastPage" type="checkbox" />
            遇到最后一页停止
          </label>
          <button class="primary-button" type="submit" :disabled="hasActiveTask || submittingAction === 'issueRange'">
            {{ submittingAction === 'issueRange' ? '创建中' : '创建期号范围同步' }}
          </button>
        </form>

        <form
          class="operation-card"
          data-testid="date-range-form"
          @submit.prevent="emit('submitDateRange')"
        >
          <div>
            <h3>按日期范围同步</h3>
            <p>扫描历史分页并同步落在开奖日期范围内的数据。</p>
          </div>
          <div class="field-row">
            <label for="dateStartDate">开始日期</label>
            <input id="dateStartDate" v-model="dateRangeForm.startDate" type="date" />
          </div>
          <div class="field-row">
            <label for="dateEndDate">结束日期</label>
            <input id="dateEndDate" v-model="dateRangeForm.endDate" type="date" />
          </div>
          <div class="field-row">
            <label for="dateStartPage">起始页</label>
            <input id="dateStartPage" v-model.number="dateRangeForm.startPage" min="1" type="number" />
          </div>
          <div class="field-row">
            <label for="datePageSize">每页数量</label>
            <input id="datePageSize" v-model.number="dateRangeForm.pageSize" min="1" max="50" type="number" />
          </div>
          <div class="field-row">
            <label for="datePageDelay">页间隔毫秒</label>
            <input id="datePageDelay" v-model.number="dateRangeForm.pageDelayMillis" min="0" type="number" />
          </div>
          <label class="check-row">
            <input v-model="dateRangeForm.stopWhenLastPage" type="checkbox" />
            遇到最后一页停止
          </label>
          <button class="primary-button" type="submit" :disabled="hasActiveTask || submittingAction === 'dateRange'">
            {{ submittingAction === 'dateRange' ? '创建中' : '创建日期范围同步' }}
          </button>
        </form>
      </div>
    </div>
  </section>
</template>
