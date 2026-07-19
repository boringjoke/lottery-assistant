<script setup lang="ts">
import { computed, ref, type ComponentPublicInstance } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { createFavorite } from '@/api/favorites'
import { analyzeDltNumbers } from '@/api/lottery'
import AppToast from '@/components/AppToast.vue'
import LotteryNumberGroup from '@/components/LotteryNumberGroup.vue'
import StateMessage from '@/components/StateMessage.vue'
import type { CurrentUser } from '@/types/auth'
import type {
  LotteryDltAnalyzeHitDetail,
  LotteryDltAnalyzeNumberResult,
  LotteryDltAnalyzeResponse,
} from '@/types/lottery'
import {
  buildSingleAnalyzeLine,
  getErrorMessage,
  parseNumberText,
  splitBatchAnalyzeInput,
} from '@/utils/lotteryFormat'

type InputMode = 'single' | 'batch'
type NumberArea = 'front' | 'back'

interface FlattenHitDetail extends LotteryDltAnalyzeHitDetail {
  displayText: string
}

const props = defineProps<{
  currentUser?: CurrentUser | null
}>()

const route = useRoute()
const router = useRouter()
const inputMode = ref<InputMode>('single')
const frontNumbers = ref(['', '', '', '', ''])
const backNumbers = ref(['', ''])
const batchText = ref('')
const analyzing = ref(false)
const error = ref('')
const toastMessage = ref('')
const toastType = ref<'success' | 'error' | 'info'>('info')
const favoriteSavingLineNo = ref<number | null>(null)
const result = ref<LotteryDltAnalyzeResponse | null>(null)
const singleInputRefs = ref<HTMLInputElement[]>([])
const hitDetailPageNo = ref(1)
const hitDetailPageSize = 10

/**
 * 将每注结果中的中奖明细展开成表格数据，并补上对应输入号码。
 */
const hitDetails = computed<FlattenHitDetail[]>(() => {
  return (
    result.value?.results.flatMap((numberResult) =>
      numberResult.hitDetails.map((detail) => ({
        ...detail,
        displayText: numberResult.displayText,
      })),
    ) ?? []
  )
})

const hitDetailTotalPages = computed(() => Math.max(Math.ceil(hitDetails.value.length / hitDetailPageSize), 1))
const pagedHitDetails = computed(() => {
  const start = (hitDetailPageNo.value - 1) * hitDetailPageSize

  return hitDetails.value.slice(start, start + hitDetailPageSize)
})
const hasPreviousHitDetailPage = computed(() => hitDetailPageNo.value > 1)
const hasNextHitDetailPage = computed(() => hitDetailPageNo.value < hitDetailTotalPages.value)

function showToast(message: string, type: 'success' | 'error' | 'info' = 'info') {
  toastType.value = type
  toastMessage.value = message
}

function closeToast() {
  toastMessage.value = ''
}

/**
 * 更新前区输入框，只保留两位以内的数字。
 */
function updateFrontNumber(index: number, event: Event) {
  const normalizedValue = normalizeNumberInput(event)
  frontNumbers.value[index] = normalizedValue
  validateSingleInputRange()
  if (isSingleInputValueInRange(normalizedValue, 'front')) {
    focusNextSingleInput(index, normalizedValue)
  }
}

/**
 * 更新后区输入框，只保留两位以内的数字。
 */
function updateBackNumber(index: number, event: Event) {
  const normalizedValue = normalizeNumberInput(event)
  backNumbers.value[index] = normalizedValue
  validateSingleInputRange()
  if (isSingleInputValueInRange(normalizedValue, 'back')) {
    focusNextSingleInput(frontNumbers.value.length + index, normalizedValue)
  }
}

/**
 * 根据当前输入模式清空草稿和历史分析结果。
 */
function clearInput() {
  error.value = ''
  closeToast()
  result.value = null
  hitDetailPageNo.value = 1
  if (inputMode.value === 'single') {
    frontNumbers.value = ['', '', '', '', '']
    backNumbers.value = ['', '']
  } else {
    batchText.value = ''
  }
}

/**
 * 切换分析模式时清空错误和结果，避免单注和批量之间串联提示状态。
 */
function changeInputMode(mode: InputMode) {
  inputMode.value = mode
  error.value = ''
  closeToast()
  result.value = null
  hitDetailPageNo.value = 1
}

/**
 * 将单注输入或批量输入统一转换为后端 analyze 接口需要的 numbers 数组。
 */
function buildAnalyzeNumbers(): string[] {
  if (inputMode.value === 'single') {
    return [buildSingleAnalyzeLine(frontNumbers.value, backNumbers.value)]
  }

  return splitBatchAnalyzeInput(batchText.value)
}

/**
 * 提交前校验当前输入，返回可直接展示给用户的错误文案。
 */
function validateAnalyzeInput(numbers: string[]): string {
  if (inputMode.value === 'single') {
    return validateSingleInput()
  }

  if (numbers.length === 0) {
    return '号码不能为空'
  }

  return validateBatchInput(numbers)
}

/**
 * 保存单注输入框引用，便于输入满两位后自动跳到下一个输入框。
 */
function setSingleInputRef(element: Element | ComponentPublicInstance | null, index: number) {
  if (element instanceof HTMLInputElement) {
    singleInputRefs.value[index] = element
  }
}

/**
 * 当前号码输入满两位后，将焦点移动到下一个号码输入框。
 */
function focusNextSingleInput(index: number, value: string) {
  if (value.length < 2) {
    return
  }

  singleInputRefs.value[index + 1]?.focus()
}

/**
 * 清洗号码输入框的当前 DOM 值，确保字母、符号和超长内容不会残留在输入框里。
 */
function normalizeNumberInput(event: Event): string {
  const inputElement = event.target as HTMLInputElement
  const normalizedValue = inputElement.value.replace(/\D/g, '').slice(0, 2)
  inputElement.value = normalizedValue

  return normalizedValue
}

/**
 * 校验单注输入框的号码范围，前区为 01-35，后区为 01-12。
 */
function validateSingleInputRange(): boolean {
  if (inputMode.value !== 'single') {
    return true
  }

  const invalidFrontNumber = frontNumbers.value.find((number) => !isSingleInputValueInRange(number, 'front'))
  if (invalidFrontNumber) {
    error.value = '前区号码范围必须为 01-35'
    return false
  }

  const invalidBackNumber = backNumbers.value.find((number) => !isSingleInputValueInRange(number, 'back'))
  if (invalidBackNumber) {
    error.value = '后区号码范围必须为 01-12'
    return false
  }

  if (error.value === '前区号码范围必须为 01-35' || error.value === '后区号码范围必须为 01-12') {
    error.value = ''
  }

  return true
}

/**
 * 校验单注输入是否符合大乐透规则：前区 5 个、后区 2 个、范围合法且区内不重复。
 */
function validateSingleInput(): string {
  if (frontNumbers.value.some((number) => !number) || backNumbers.value.some((number) => !number)) {
    return '请输入完整号码：前区 5 个号码，后区 2 个号码'
  }

  const frontError = validateNumberArea(frontNumbers.value, 'front')
  if (frontError) {
    return frontError
  }

  const backError = validateNumberArea(backNumbers.value, 'back')
  if (backError) {
    return backError
  }

  return ''
}

/**
 * 校验批量输入中的每一行，并在错误文案中标明行号。
 */
function validateBatchInput(numbers: string[]): string {
  for (const [index, line] of numbers.entries()) {
    const lineError = validateBatchLine(line)
    if (lineError) {
      return `第${index + 1}行：${lineError}`
    }
  }

  return ''
}

/**
 * 校验单行批量输入是否符合“前区 + 后区”的大乐透格式。
 */
function validateBatchLine(line: string): string {
  const areas = line.trim().split('+')
  if (areas.length !== 2) {
    return '号码格式不合法，请使用 + 分隔前区和后区'
  }

  const frontValues = splitAreaNumbers(areas[0])
  const backValues = splitAreaNumbers(areas[1])
  if (frontValues.some((number) => !/^\d+$/.test(number))) {
    return '前区号码必须为数字'
  }
  if (backValues.some((number) => !/^\d+$/.test(number))) {
    return '后区号码必须为数字'
  }
  if (frontValues.length !== 5) {
    return '前区号码必须为 5 个'
  }
  if (backValues.length !== 2) {
    return '后区号码必须为 2 个'
  }

  return validateNumberArea(frontValues, 'front') || validateNumberArea(backValues, 'back')
}

/**
 * 按空格、中英文逗号拆分一个号码区。
 */
function splitAreaNumbers(areaText: string | undefined): string[] {
  return (areaText ?? '')
    .trim()
    .split(/[,，\s]+/)
    .map((token) => token.trim())
    .filter(Boolean)
}

/**
 * 校验某个号码区的范围和重复性。
 */
function validateNumberArea(values: string[], area: NumberArea): string {
  const areaName = area === 'front' ? '前区' : '后区'
  const rangeText = area === 'front' ? '01-35' : '01-12'
  const normalizedValues = values.map((value) => Number(value))

  if (normalizedValues.some((number) => !Number.isInteger(number))) {
    return `${areaName}号码必须为数字`
  }
  if (normalizedValues.some((number) => !isSingleInputValueInRange(String(number), area))) {
    return `${areaName}号码范围必须为 ${rangeText}`
  }
  if (new Set(normalizedValues).size !== normalizedValues.length) {
    return `${areaName}号码不能重复`
  }

  return ''
}

/**
 * 判断单个号码是否落在对应区域允许范围内；空值留给提交时的完整性校验处理。
 */
function isSingleInputValueInRange(value: string, area: NumberArea): boolean {
  if (!value) {
    return true
  }

  const numericValue = Number(value)
  if (!Number.isInteger(numericValue)) {
    return false
  }

  return area === 'front'
    ? numericValue >= 1 && numericValue <= 35
    : numericValue >= 1 && numericValue <= 12
}

/**
 * 提交号码分析请求，并把后端校验错误展示在输入区域下方。
 */
async function submitAnalyze() {
  const numbers = buildAnalyzeNumbers()
  const validateError = validateAnalyzeInput(numbers)
  if (validateError) {
    error.value = validateError
    result.value = null
    return
  }

  analyzing.value = true
  error.value = ''
  closeToast()

  try {
    result.value = await analyzeDltNumbers({ numbers })
    hitDetailPageNo.value = 1
  } catch (err) {
    result.value = null
    error.value = getErrorMessage(err)
  } finally {
    analyzing.value = false
  }
}

/**
 * 将后端返回的数字数组转换成号码球组件可展示的前区/后区数据。
 */
function formatResultNumbers(numberResult: LotteryDltAnalyzeNumberResult) {
  return {
    front: parseNumberText(numberResult.frontNumbers),
    back: parseNumberText(numberResult.backNumbers),
  }
}

function changeHitDetailPage(nextPageNo: number) {
  hitDetailPageNo.value = Math.min(Math.max(nextPageNo, 1), hitDetailTotalPages.value)
}

async function favoriteAnalyzeNumber(numberResult: LotteryDltAnalyzeNumberResult) {
  favoriteSavingLineNo.value = numberResult.lineNo
  closeToast()

  try {
    await createFavorite({
      lotteryType: 'DLT',
      frontNumbers: numberResult.frontNumbers,
      backNumbers: numberResult.backNumbers,
      favoriteName: numberResult.displayText,
      remark: '来自号码分析结果',
    })
    showToast('号码已收藏', 'success')
  } catch (err) {
    showToast(getErrorMessage(err), 'error')
  } finally {
    favoriteSavingLineNo.value = null
  }
}

async function goLoginBeforeFavorite() {
  await router.push({
    path: '/login',
    query: { redirect: route.fullPath || '/lottery-assistant?tab=analyze' },
  })
}
</script>

<template>
  <div class="content-container analysis-layout">
    <AppToast :message="toastMessage" :type="toastType" @close="closeToast" />

    <section class="card input-card">
      <div class="input-heading">
        <div>
          <div class="title-row">
            <h2 class="section-title">号码历史中奖分析</h2>
            <span class="ticket-chip">当前票种：大乐透</span>
          </div>
          <p>根据当前票种规则输入号码</p>
        </div>
      </div>

      <div class="mode-tabs">
        <button
          type="button"
          :class="{ active: inputMode === 'single' }"
          @click="changeInputMode('single')"
        >
          单注分析
        </button>
        <button
          type="button"
          :class="{ active: inputMode === 'batch' }"
          @click="changeInputMode('batch')"
        >
          批量分析
        </button>
      </div>

      <div class="input-panel">
        <template v-if="inputMode === 'single'">
          <div class="single-input-row">
            <div class="number-inputs">
              <input
                v-for="(_, index) in frontNumbers"
                :key="`front-input-${index}`"
                class="number-input number-input--red"
                type="text"
                inputmode="numeric"
                maxlength="2"
                :ref="(element) => setSingleInputRef(element, index)"
                :value="frontNumbers[index]"
                @input="updateFrontNumber(index, $event)"
              />
              <span class="number-plus">+</span>
              <input
                v-for="(_, index) in backNumbers"
                :key="`back-input-${index}`"
                class="number-input number-input--blue"
                type="text"
                inputmode="numeric"
                maxlength="2"
                :ref="(element) => setSingleInputRef(element, frontNumbers.length + index)"
                :value="backNumbers[index]"
                @input="updateBackNumber(index, $event)"
              />
            </div>
            <div class="input-actions">
              <button class="secondary-button" type="button" @click="clearInput">清空</button>
              <button class="primary-button" type="button" :disabled="analyzing" @click="submitAnalyze">
                {{ analyzing ? '分析中...' : '开始分析' }}
              </button>
            </div>
          </div>
          <p class="rule-tip">规则：前区 5 个号码，后区 2 个号码；示例：01 05 12 23 35 + 03 11</p>
        </template>

        <template v-else>
          <label class="field-label" for="batchText">批量号码输入</label>
          <textarea
            id="batchText"
            v-model="batchText"
            class="text-area"
            placeholder="01 05 12 23 35 + 03 11&#10;02 09 16 22 33 + 04 09"
          ></textarea>
          <div class="batch-footer">
            <p class="rule-tip">
              每行一注号码，前区和后区使用 + 分隔；单次最多分析 50 注。
            </p>
            <div class="input-actions">
              <button class="secondary-button" type="button" @click="clearInput">清空</button>
              <button class="primary-button" type="button" :disabled="analyzing" @click="submitAnalyze">
                {{ analyzing ? '分析中...' : '开始分析' }}
              </button>
            </div>
          </div>
        </template>
      </div>

      <p v-if="error" class="form-error">{{ error }}</p>
    </section>

    <template v-if="result">
      <section class="stats-grid">
        <div class="stat-card stat-card--blue">
          <span>共分析</span>
          <strong>{{ result.totalNumberCount }}</strong>
          <small>注号码</small>
        </div>
        <div class="stat-card stat-card--cyan">
          <span>历史开奖</span>
          <strong>{{ result.analyzedDrawCount }}</strong>
          <small>期参与分析</small>
        </div>
        <div class="stat-card stat-card--orange">
          <span>历史中过奖</span>
          <strong>{{ result.winningNumberCount }} / {{ result.winningHitCount }}</strong>
          <small>注 / 次</small>
        </div>
        <div class="stat-card stat-card--amber">
          <span>最高命中奖级</span>
          <strong>{{ result.bestPrizeName }}</strong>
          <small>基于历史开奖数据</small>
        </div>
      </section>

      <section class="card result-card">
        <h3 class="section-title">分析号码结果</h3>
        <div class="result-list">
          <div v-for="numberResult in result.results" :key="numberResult.lineNo" class="result-item">
            <LotteryNumberGroup
              :front-numbers="formatResultNumbers(numberResult).front"
              :back-numbers="formatResultNumbers(numberResult).back"
            />
            <span>展示号码：{{ numberResult.displayText }}</span>
            <span class="result-metric result-metric--hit">
              历史命中：<strong>{{ numberResult.winningHitCount }} 次</strong>
            </span>
            <span class="result-metric result-metric--prize">
              最高奖级：<strong>{{ numberResult.bestPrizeName }}</strong>
            </span>
            <button
              v-if="props.currentUser"
              data-test="favorite-analyze-number"
              class="favorite-number-button"
              type="button"
              :disabled="favoriteSavingLineNo === numberResult.lineNo"
              @click="favoriteAnalyzeNumber(numberResult)"
            >
              {{ favoriteSavingLineNo === numberResult.lineNo ? '收藏中' : '收藏号码' }}
            </button>
            <button
              v-else
              data-test="login-before-favorite"
              class="favorite-number-button favorite-number-button--ghost"
              type="button"
              @click="goLoginBeforeFavorite"
            >
              登录后收藏
            </button>
          </div>
        </div>
      </section>

      <section class="card hit-card">
        <h3 class="section-title">中奖明细</h3>
        <div v-if="hitDetails.length" class="table-scroll">
          <table class="data-table hit-detail-table">
            <thead>
              <tr>
                <th>输入号码</th>
                <th>命中期号</th>
                <th>命中日期</th>
                <th>开奖号码</th>
                <th>前区命中</th>
                <th>后区命中</th>
                <th>奖级</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(detail, index) in pagedHitDetails" :key="`${detail.issueNo}-${index}`">
                <td class="mono-cell">{{ detail.displayText }}</td>
                <td>{{ detail.issueNo }}</td>
                <td>{{ detail.drawDate }}</td>
                <td>
                  <LotteryNumberGroup
                    :front-numbers="detail.drawFrontNumbers"
                    :back-numbers="detail.drawBackNumbers"
                  />
                </td>
                <td>
                  <span class="hit-pill hit-pill--red">{{ detail.frontHitCount }} 个</span>
                </td>
                <td>
                  <span class="hit-pill hit-pill--blue">{{ detail.backHitCount }} 个</span>
                </td>
                <td>
                  <strong>{{ detail.prizeName }}</strong>
                </td>
              </tr>
            </tbody>
          </table>
          <div v-if="hitDetails.length > hitDetailPageSize" class="hit-detail-pagination">
            <span>共 {{ hitDetails.length }} 条，第 {{ hitDetailPageNo }} / {{ hitDetailTotalPages }} 页</span>
            <div>
              <button
                data-test="hit-detail-prev-page"
                type="button"
                :disabled="!hasPreviousHitDetailPage"
                @click="changeHitDetailPage(hitDetailPageNo - 1)"
              >
                上一页
              </button>
              <button
                data-test="hit-detail-next-page"
                type="button"
                :disabled="!hasNextHitDetailPage"
                @click="changeHitDetailPage(hitDetailPageNo + 1)"
              >
                下一页
              </button>
            </div>
          </div>
        </div>
        <StateMessage v-else title="暂无中奖明细" message="当前号码在已入库历史开奖中没有中奖记录。" />
      </section>
    </template>

    <StateMessage
      v-else-if="!analyzing"
      class="card"
      title="等待分析"
      message="输入一注或多注大乐透号码后，可以查看其历史命中情况。"
    />

    <p class="analysis-disclaimer data-note">
      分析结果仅基于历史开奖数据，不代表未来开奖结果或中奖概率。
    </p>
  </div>
</template>

<style scoped>
.analysis-layout {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.input-card,
.result-card,
.hit-card {
  padding: 24px;
}

.hit-card .section-title {
  margin-bottom: 18px;
}

.hit-detail-table {
  border-collapse: separate;
  border-spacing: 0;
}

.hit-detail-table th {
  border-bottom-color: #e2e8f0;
  background: #f8fafc;
  padding: 12px;
}

.hit-detail-table th:first-child {
  border-top-left-radius: 10px;
}

.hit-detail-table th:last-child {
  border-top-right-radius: 10px;
}

.hit-detail-table td {
  border-bottom-color: #eef2f7;
}

.hit-detail-table th,
.hit-detail-table td {
  text-align: center;
  vertical-align: middle;
}

.hit-detail-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 16px;
  color: #64748b;
  font-size: 13px;
  font-weight: 800;
}

.hit-detail-pagination div {
  display: flex;
  gap: 8px;
}

.hit-detail-pagination button {
  min-width: fit-content;
  height: 34px;
  border: 1px solid #dbeafe;
  border-radius: 10px;
  background: #eff6ff;
  color: #1d4ed8;
  padding: 0 14px;
  font-size: 13px;
  font-weight: 900;
  white-space: nowrap;
}

.hit-detail-pagination button:disabled {
  opacity: 0.58;
}

.title-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.ticket-chip {
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #eff6ff;
  color: #2563eb;
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 800;
}

.input-heading p {
  margin: 6px 0 0 16px;
  color: #64748b;
  font-size: 14px;
}

.mode-tabs {
  display: flex;
  gap: 24px;
  margin-top: 24px;
  border-bottom: 1px solid #f1f5f9;
}

.mode-tabs button {
  border: 0;
  border-bottom: 2px solid transparent;
  background: transparent;
  color: #64748b;
  padding: 0 4px 12px;
  font-size: 14px;
  font-weight: 800;
}

.mode-tabs button.active {
  border-bottom-color: #2563eb;
  color: #2563eb;
}

.input-panel {
  margin-top: 24px;
  padding: 24px;
  border: 1px solid #f1f5f9;
  border-radius: 16px;
  background: #f8fafc;
}

.single-input-row {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
}

.number-inputs {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.number-input {
  width: 48px;
  height: 48px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #ffffff;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace;
  font-size: 18px;
  font-weight: 900;
  text-align: center;
  outline: none;
  transition:
    border-color 0.2s,
    box-shadow 0.2s;
}

.number-input--red {
  color: #dc2626;
}

.number-input--red:focus {
  border-color: #f87171;
  box-shadow: 0 0 0 3px rgb(248 113 113 / 0.18);
}

.number-input--blue {
  color: #2563eb;
}

.number-input--blue:focus {
  border-color: #60a5fa;
  box-shadow: 0 0 0 3px rgb(96 165 250 / 0.18);
}

.number-plus {
  color: #cbd5e1;
  font-size: 24px;
  font-weight: 300;
}

.input-actions {
  display: flex;
  flex: 0 0 auto;
  gap: 12px;
}

.input-actions > button {
  min-width: 96px;
  white-space: nowrap;
}

.rule-tip {
  margin: 14px 0 0;
  color: #64748b;
  font-size: 13px;
}

.batch-footer {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-top: 16px;
}

.form-error {
  margin: 14px 0 0;
  color: #dc2626;
  font-size: 14px;
  font-weight: 700;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.stat-card {
  position: relative;
  overflow: hidden;
  min-height: 132px;
  padding: 22px;
  border-radius: 16px;
  background: #ffffff;
  box-shadow: 0 1px 3px rgb(15 23 42 / 0.05);
}

.stat-card::after {
  position: absolute;
  top: -32px;
  right: -32px;
  width: 96px;
  height: 96px;
  border-radius: 999px;
  content: "";
}

.stat-card span,
.stat-card small {
  display: block;
  font-size: 14px;
  font-weight: 700;
}

.stat-card strong {
  display: block;
  margin-top: 8px;
  font-size: 30px;
  font-weight: 900;
  letter-spacing: 0;
}

.stat-card small {
  margin-top: 4px;
  opacity: 0.72;
}

.stat-card--blue {
  border: 1px solid #dbeafe;
  color: #2563eb;
}

.stat-card--blue::after {
  background: #eff6ff;
}

.stat-card--cyan {
  border: 1px solid #cffafe;
  color: #0891b2;
}

.stat-card--cyan::after {
  background: #ecfeff;
}

.stat-card--orange {
  border: 1px solid #fed7aa;
  color: #ea580c;
}

.stat-card--orange::after {
  background: #fff7ed;
}

.stat-card--amber {
  border: 1px solid #fde68a;
  color: #d97706;
}

.stat-card--amber::after {
  background: #fffbeb;
}

.result-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 18px;
}

.result-item {
  display: grid;
  grid-template-columns: minmax(280px, 1.2fr) repeat(3, minmax(120px, 1fr)) auto;
  gap: 16px;
  align-items: center;
  padding: 16px;
  border: 1px solid #f1f5f9;
  border-radius: 14px;
  background: #f8fafc;
  color: #475569;
  font-size: 14px;
}

.result-metric {
  display: inline-flex;
  width: fit-content;
  align-items: center;
  border-radius: 999px;
  padding: 6px 12px;
  font-weight: 800;
}

.result-metric strong {
  margin-left: 4px;
  font-weight: 900;
}

.result-metric--hit {
  background: #fff7ed;
  color: #c2410c;
}

.result-metric--prize {
  background: #fffbeb;
  color: #b45309;
}

.favorite-number-button {
  display: inline-flex;
  width: max-content;
  min-width: 88px;
  height: 36px;
  align-items: center;
  justify-content: center;
  border: 1px solid #dbeafe;
  border-radius: 10px;
  background: #eff6ff;
  color: #1d4ed8;
  padding: 0 14px;
  font-size: 13px;
  font-weight: 900;
  white-space: nowrap;
}

.favorite-number-button--ghost {
  border-color: #e2e8f0;
  background: #ffffff;
  color: #64748b;
}

.favorite-number-button:disabled {
  opacity: 0.58;
}

.mono-cell {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace;
}

.hit-pill {
  display: inline-flex;
  border-radius: 6px;
  padding: 2px 8px;
  font-size: 12px;
  font-weight: 800;
}

.hit-pill--red {
  background: #fef2f2;
  color: #b91c1c;
}

.hit-pill--blue {
  background: #eff6ff;
  color: #1d4ed8;
}

.analysis-disclaimer {
  justify-content: center;
  padding: 8px 0;
}

@media (max-width: 1120px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .result-item {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .single-input-row,
  .batch-footer {
    align-items: stretch;
    flex-direction: column;
  }

  .input-actions > button {
    flex: 1;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }

  .hit-detail-pagination {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
