<script setup lang="ts">
import LotteryNumberGroup from '@/components/LotteryNumberGroup.vue'
import StateMessage from '@/components/StateMessage.vue'
import type { LotteryDrawDetail } from '@/types/lottery'
import { formatCurrency, formatLotteryType, getDltPrizeRule } from '@/utils/lotteryFormat'

defineProps<{
  open: boolean
  draw: LotteryDrawDetail | null
  loading: boolean
  error: string
}>()

defineEmits<{
  close: []
  retry: []
}>()
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="dialog-mask" @click="$emit('close')">
      <section class="dialog-panel" aria-modal="true" role="dialog" @click.stop>
        <header class="dialog-header">
          <h2>开奖详情</h2>
          <button class="dialog-close" type="button" aria-label="关闭弹窗" @click="$emit('close')">
            ×
          </button>
        </header>

        <div class="dialog-body">
          <StateMessage v-if="loading" title="正在加载开奖详情" />
          <StateMessage
            v-else-if="error"
            title="详情加载失败"
            :message="error"
            action-label="重试"
            @action="$emit('retry')"
          />

          <template v-else-if="draw">
            <div class="detail-summary">
              <div>
                <span>彩票类型</span>
                <strong>{{ formatLotteryType(draw.lotteryType) }}</strong>
              </div>
              <div>
                <span>期号</span>
                <strong>第 {{ draw.issueNo }} 期</strong>
              </div>
              <div>
                <span>开奖日期</span>
                <strong>{{ draw.drawDate }}</strong>
              </div>
            </div>

            <section class="detail-section">
              <h3>开奖号码</h3>
              <div class="number-card">
                <LotteryNumberGroup
                  :front-numbers="draw.frontNumbers"
                  :back-numbers="draw.backNumbers"
                  size="md"
                />
              </div>
            </section>

            <section class="detail-section">
              <h3>奖池与销售</h3>
              <div class="amount-grid">
                <div class="amount-card">
                  <span>奖池金额 (元)</span>
                  <strong>{{ formatCurrency(draw.poolBalance) }}</strong>
                </div>
                <div class="amount-card">
                  <span>销售金额 (元)</span>
                  <strong>{{ formatCurrency(draw.salesAmount) }}</strong>
                </div>
              </div>
            </section>

            <section class="detail-section">
              <h3 class="prize-section-title">奖级明细</h3>
              <div class="table-scroll prize-table-wrap">
                <table class="data-table">
                  <thead>
                    <tr>
                      <th>奖级</th>
                      <th>中奖规则</th>
                      <th>中奖注数</th>
                      <th class="align-right">单注奖金</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="tier in draw.prizeTiers" :key="`${tier.sortOrder}-${tier.prizeName}`">
                      <td>
                        <strong>{{ tier.prizeName }}</strong>
                      </td>
                      <td>{{ getDltPrizeRule(tier.prizeName) }}</td>
                      <td>{{ tier.stakeCount ?? '-' }}</td>
                      <td class="align-right amount-text">{{ formatCurrency(tier.stakeAmount) }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </section>

            <section class="detail-section">
              <h3>数据来源</h3>
              <div class="source-info">
                <span>中国体彩网官方公开数据，由后端同步入库。</span>
              </div>
              <div v-if="draw.pdfUrl" class="pdf-action">
                <div>
                  <strong>开奖公告</strong>
                  <span>可跳转查看官方公告 PDF 原文</span>
                </div>
                <a :href="draw.pdfUrl" target="_blank" rel="noopener">查看 PDF</a>
              </div>
              <p class="data-note">开奖数据仅供参考，最终结果以中国体彩网官方公布为准。</p>
            </section>
          </template>
        </div>

        <footer class="dialog-footer">
          <button class="primary-button" type="button" @click="$emit('close')">关闭</button>
        </footer>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.dialog-mask {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  background: rgb(15 23 42 / 0.42);
  backdrop-filter: blur(4px);
}

.dialog-panel {
  display: flex;
  width: min(720px, 100%);
  max-height: 90vh;
  flex-direction: column;
  overflow: hidden;
  border-radius: 18px;
  background: #ffffff;
  box-shadow: 0 24px 60px rgb(15 23 42 / 0.24);
}

.dialog-header,
.dialog-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
}

.dialog-header {
  border-bottom: 1px solid #f1f5f9;
}

.dialog-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
}

.dialog-close {
  width: 32px;
  height: 32px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: #94a3b8;
  font-size: 24px;
  line-height: 1;
}

.dialog-close:hover {
  background: #f1f5f9;
  color: #475569;
}

.dialog-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.dialog-footer {
  justify-content: flex-end;
  border-top: 1px solid #f1f5f9;
  background: #f8fafc;
}

.detail-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  padding: 16px;
  border: 1px solid #f1f5f9;
  border-radius: 14px;
  background: #f8fafc;
}

.detail-summary span,
.amount-card span {
  display: block;
  margin-bottom: 4px;
  color: #64748b;
  font-size: 12px;
}

.detail-summary strong,
.amount-card strong {
  color: #111827;
  font-size: 14px;
}

.amount-card span {
  margin-bottom: 6px;
  font-size: 14px;
  font-weight: 700;
}

.amount-card strong {
  font-size: 22px;
  font-weight: 900;
}

.detail-section {
  margin-top: 28px;
}

.detail-section h3 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 12px;
  color: #111827;
  font-size: 14px;
  font-weight: 800;
}

.detail-section h3::before {
  display: block;
  width: 4px;
  height: 14px;
  border-radius: 999px;
  background: #3b82f6;
  content: "";
}

.number-card,
.amount-card {
  border: 1px solid #f1f5f9;
  border-radius: 14px;
  background: #ffffff;
  box-shadow: 0 1px 2px rgb(15 23 42 / 0.04);
}

.number-card {
  padding: 16px;
}

.amount-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.amount-card {
  padding: 16px;
}

.amount-card strong,
.amount-text {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace;
}

.prize-table-wrap {
  border: 1px solid #f1f5f9;
  border-radius: 14px;
}

.prize-table-wrap :deep(thead th) {
  height: 44px;
  padding-top: 0;
  padding-bottom: 0;
  background: #f8fafc;
  vertical-align: middle;
}

.prize-table-wrap :deep(thead th:first-child) {
  border-top-left-radius: 14px;
}

.prize-table-wrap :deep(thead th:last-child) {
  border-top-right-radius: 14px;
}

.align-right {
  text-align: right;
}

.source-info {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
  color: #64748b;
  font-size: 14px;
}

.pdf-action {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
  padding: 12px 14px;
  border: 1px solid #dbeafe;
  border-radius: 12px;
  background: #eff6ff;
}

.pdf-action div {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.pdf-action strong {
  color: #1e3a8a;
  font-size: 14px;
}

.pdf-action span {
  color: #64748b;
  font-size: 12px;
}

.pdf-action a {
  flex: 0 0 auto;
  color: #2563eb;
  font-size: 14px;
  font-weight: 700;
  text-decoration: none;
}

.pdf-action a:hover {
  color: #1d4ed8;
}

@media (max-width: 640px) {
  .detail-summary,
  .amount-grid {
    grid-template-columns: 1fr;
  }

  .dialog-body {
    padding: 18px;
  }
}
</style>
