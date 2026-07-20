<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'

import { logout } from '@/api/auth'
import {
  activateFavorite,
  deactivateFavorite,
  deleteFavorite,
  fetchFavoritePage,
  updateFavorite,
} from '@/api/favorites'
import { fetchUserProfile } from '@/api/user'
import AppToast from '@/components/AppToast.vue'
import ProfileShell from '@/components/profile/ProfileShell.vue'
import type { CurrentUser } from '@/types/auth'
import type { FavoriteStatus, LotteryNumberFavorite, LotteryNumberFavoritePage } from '@/types/favorite'
import type { UserProfile } from '@/types/user'
import { getErrorMessage } from '@/utils/lotteryFormat'

const router = useRouter()
const profile = ref<UserProfile | null>(null)
const favoritePage = ref<LotteryNumberFavoritePage | null>(null)
const loadingProfile = ref(false)
const loadingFavorites = ref(false)
const authLoading = ref(false)
const operatingFavoriteId = ref<number | null>(null)
const errorMessage = ref('')
const toastMessage = ref('')
const toastType = ref<'success' | 'error' | 'info'>('info')
const statusFilter = ref<FavoriteStatus>('ACTIVE')
const keyword = ref('')
const pageNo = ref(1)
const pageSize = 10
const editingFavoriteId = ref<number | null>(null)
const editName = ref('')
const editRemark = ref('')

const statusFilters: Array<{ label: string, value: FavoriteStatus }> = [
  { label: '有效收藏', value: 'ACTIVE' },
  { label: '已取消', value: 'CANCELLED' },
]

const currentUser = computed<CurrentUser | null>(() => {
  if (!profile.value) {
    return null
  }

  return {
    userId: profile.value.userId,
    nickname: profile.value.nickname,
    avatarUrl: profile.value.avatarUrl,
    roles: profile.value.roles,
  }
})

const favorites = computed(() => favoritePage.value?.favorites ?? [])
const totalPages = computed(() => Math.max(favoritePage.value?.pages ?? 0, 1))
const hasPreviousPage = computed(() => pageNo.value > 1)
const hasNextPage = computed(() => pageNo.value < totalPages.value)

function parseNumbers(value: string): string[] {
  return value
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
}

function lotteryTypeText(value: string): string {
  if (value === 'DLT') {
    return '大乐透'
  }

  return value || '-'
}

function statusText(value: FavoriteStatus): string {
  return value === 'ACTIVE' ? '有效' : '已取消'
}

function formatTime(value: string | null | undefined): string {
  if (!value) {
    return '-'
  }

  return value.replace('T', ' ').slice(0, 16)
}

function isUnauthorized(error: unknown): boolean {
  return getErrorMessage(error).includes('请先登录')
}

function showToast(message: string, type: 'success' | 'error' | 'info' = 'info') {
  toastType.value = type
  toastMessage.value = message
}

function closeToast() {
  toastMessage.value = ''
}

async function loadProfile() {
  loadingProfile.value = true
  errorMessage.value = ''

  try {
    profile.value = await fetchUserProfile()
  } catch (error) {
    if (isUnauthorized(error)) {
      await router.push({
        path: '/login',
        query: { redirect: '/profile/favorites' },
      })
      return
    }

    errorMessage.value = getErrorMessage(error)
    profile.value = null
  } finally {
    loadingProfile.value = false
  }
}

async function loadFavorites() {
  if (!profile.value) {
    return
  }

  loadingFavorites.value = true
  errorMessage.value = ''

  try {
    favoritePage.value = await fetchFavoritePage({
      pageNo: pageNo.value,
      pageSize,
      status: statusFilter.value,
      keyword: keyword.value.trim(),
    })
  } catch (error) {
    if (isUnauthorized(error)) {
      await router.push({
        path: '/login',
        query: { redirect: '/profile/favorites' },
      })
      return
    }

    errorMessage.value = getErrorMessage(error)
    favoritePage.value = null
  } finally {
    loadingFavorites.value = false
  }
}

async function initializePage() {
  await loadProfile()
  await loadFavorites()
}

async function changeStatus(nextStatus: FavoriteStatus) {
  if (statusFilter.value === nextStatus) {
    return
  }

  statusFilter.value = nextStatus
  pageNo.value = 1
  editingFavoriteId.value = null
  closeToast()
  await loadFavorites()
}

async function submitSearch() {
  pageNo.value = 1
  editingFavoriteId.value = null
  closeToast()
  await loadFavorites()
}

async function changePage(nextPageNo: number) {
  pageNo.value = nextPageNo
  editingFavoriteId.value = null
  closeToast()
  await loadFavorites()
}

function startEdit(favorite: LotteryNumberFavorite) {
  editingFavoriteId.value = favorite.id
  editName.value = favorite.favoriteName
  editRemark.value = favorite.remark ?? ''
  closeToast()
}

function cancelEdit() {
  editingFavoriteId.value = null
  editName.value = ''
  editRemark.value = ''
}

async function saveFavorite() {
  if (!editingFavoriteId.value) {
    return
  }

  operatingFavoriteId.value = editingFavoriteId.value
  errorMessage.value = ''
  closeToast()

  try {
    await updateFavorite({
      favoriteId: editingFavoriteId.value,
      favoriteName: editName.value,
      remark: editRemark.value,
    })
    cancelEdit()
    showToast('收藏信息已保存', 'success')
    await loadFavorites()
  } catch (error) {
    showToast(getErrorMessage(error), 'error')
  } finally {
    operatingFavoriteId.value = null
  }
}

async function deactivateCurrentFavorite(favoriteId: number) {
  operatingFavoriteId.value = favoriteId
  errorMessage.value = ''
  closeToast()

  try {
    await deactivateFavorite(favoriteId)
    showToast('收藏已取消', 'success')
    await loadFavorites()
  } catch (error) {
    showToast(getErrorMessage(error), 'error')
  } finally {
    operatingFavoriteId.value = null
  }
}

async function activateCurrentFavorite(favoriteId: number) {
  operatingFavoriteId.value = favoriteId
  errorMessage.value = ''
  closeToast()

  try {
    await activateFavorite(favoriteId)
    showToast('收藏已重新启用', 'success')
    await loadFavorites()
  } catch (error) {
    showToast(getErrorMessage(error), 'error')
  } finally {
    operatingFavoriteId.value = null
  }
}

async function deleteCurrentFavorite(favoriteId: number) {
  const confirmed = window.confirm('确定删除这条已取消的收藏吗？删除后不可恢复。')
  if (!confirmed) {
    return
  }

  operatingFavoriteId.value = favoriteId
  errorMessage.value = ''
  closeToast()

  try {
    await deleteFavorite(favoriteId)
    showToast('收藏已删除', 'success')
    await loadFavorites()
    if (favoritePage.value && favoritePage.value.total === 0 && pageNo.value > 1) {
      pageNo.value -= 1
      await loadFavorites()
    }
  } catch (error) {
    showToast(getErrorMessage(error), 'error')
  } finally {
    operatingFavoriteId.value = null
  }
}

async function handleLogout() {
  authLoading.value = true
  try {
    await logout()
  } finally {
    profile.value = null
    authLoading.value = false
    await router.push('/login')
  }
}

onMounted(initializePage)
</script>

<template>
  <ProfileShell
    :current-user="currentUser"
    :loading="authLoading || loadingProfile"
    active-nav="favorites"
    @logout="handleLogout"
  >
    <AppToast :message="toastMessage" :type="toastType" @close="closeToast" />
    <section class="favorites-panel">
          <div class="favorites-panel__header">
            <div>
              <h1>我的收藏号码</h1>
              <p>管理你收藏的大乐透号码</p>
            </div>
            <button class="favorites-refresh-button" type="button" :disabled="loadingFavorites" @click="loadFavorites">
              {{ loadingFavorites ? '加载中' : '刷新' }}
            </button>
          </div>

          <div class="favorites-toolbar">
            <div class="favorites-status-tabs" aria-label="收藏状态筛选">
              <button
                v-for="item in statusFilters"
                :key="item.value"
                class="favorites-status-tab"
                :class="{ active: statusFilter === item.value }"
                :data-test="item.value === 'CANCELLED' ? 'cancelled-filter' : 'active-filter'"
                type="button"
                @click="changeStatus(item.value)"
              >
                {{ item.label }}
              </button>
            </div>

            <form class="favorites-search" @submit.prevent="submitSearch">
              <input v-model="keyword" type="search" placeholder="搜索名称或备注" aria-label="搜索收藏" />
              <button type="submit" :disabled="loadingFavorites">搜索</button>
            </form>
          </div>

          <div v-if="errorMessage" class="favorites-state favorites-state--error">{{ errorMessage }}</div>

          <div v-if="loadingFavorites && !favoritePage" class="favorites-state">正在加载收藏号码</div>
          <div v-else-if="!loadingFavorites && !favorites.length" class="favorites-empty">
            <p>暂无{{ statusFilter === 'ACTIVE' ? '有效' : '已取消' }}收藏号码</p>
            <RouterLink
              v-if="statusFilter === 'ACTIVE'"
              class="favorites-empty__action"
              to="/lottery-assistant?tab=analyze"
            >
              去号码分析
            </RouterLink>
          </div>

          <div v-else class="favorites-table-wrap">
            <table class="favorites-table">
              <thead>
                <tr>
                  <th>收藏名称</th>
                  <th>彩票类型</th>
                  <th>收藏号码</th>
                  <th>备注</th>
                  <th>状态</th>
                  <th>收藏时间</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <template v-for="favorite in favorites" :key="favorite.id">
                  <tr>
                    <td>
                      <strong>{{ favorite.favoriteName || '未命名收藏' }}</strong>
                    </td>
                    <td>{{ lotteryTypeText(favorite.lotteryType) }}</td>
                    <td>
                      <div class="favorite-number-group" :aria-label="favorite.displayText">
                        <span
                          v-for="number in parseNumbers(favorite.frontNumbers)"
                          :key="`${favorite.id}-front-${number}`"
                          class="favorite-number-ball favorite-number-ball--front"
                        >
                          {{ number }}
                        </span>
                        <span class="favorite-number-plus">+</span>
                        <span
                          v-for="number in parseNumbers(favorite.backNumbers)"
                          :key="`${favorite.id}-back-${number}`"
                          class="favorite-number-ball favorite-number-ball--back"
                        >
                          {{ number }}
                        </span>
                      </div>
                    </td>
                    <td class="favorites-remark">{{ favorite.remark || '-' }}</td>
                    <td>
                      <span class="favorites-status" :class="`favorites-status--${favorite.status.toLowerCase()}`">
                        {{ statusText(favorite.status) }}
                      </span>
                    </td>
                    <td>{{ formatTime(favorite.favoriteTime) }}</td>
                    <td>
                      <div class="favorites-actions">
                        <button
                          data-test="edit-favorite"
                          type="button"
                          :disabled="operatingFavoriteId === favorite.id"
                          @click="startEdit(favorite)"
                        >
                          编辑
                        </button>
                        <button
                          v-if="favorite.status === 'ACTIVE'"
                          data-test="deactivate-favorite"
                          class="favorites-action--warn"
                          type="button"
                          :disabled="operatingFavoriteId === favorite.id"
                          @click="deactivateCurrentFavorite(favorite.id)"
                        >
                          取消收藏
                        </button>
                        <button
                          v-else
                          data-test="activate-favorite"
                          class="favorites-action--success"
                          type="button"
                          :disabled="operatingFavoriteId === favorite.id"
                          @click="activateCurrentFavorite(favorite.id)"
                        >
                          重新启用
                        </button>
                        <button
                          v-if="favorite.status === 'CANCELLED'"
                          data-test="delete-favorite"
                          class="favorites-action--danger"
                          type="button"
                          :disabled="operatingFavoriteId === favorite.id"
                          @click="deleteCurrentFavorite(favorite.id)"
                        >
                          删除
                        </button>
                      </div>
                    </td>
                  </tr>
                  <tr v-if="editingFavoriteId === favorite.id" class="favorites-edit-row">
                    <td colspan="7">
                      <form class="favorites-edit-form" @submit.prevent="saveFavorite">
                        <label for="favoriteName">
                          收藏名称
                          <input id="favoriteName" v-model="editName" type="text" maxlength="64" />
                        </label>
                        <label for="favoriteRemark">
                          备注
                          <input id="favoriteRemark" v-model="editRemark" type="text" maxlength="255" />
                        </label>
                        <div class="favorites-edit-actions">
                          <button
                            data-test="save-favorite"
                            class="favorites-save-button"
                            type="submit"
                            :disabled="operatingFavoriteId === favorite.id"
                          >
                            保存
                          </button>
                          <button type="button" :disabled="operatingFavoriteId === favorite.id" @click="cancelEdit">
                            取消
                          </button>
                        </div>
                      </form>
                    </td>
                  </tr>
                </template>
              </tbody>
            </table>
          </div>

          <div v-if="favoritePage && favoritePage.total > 0" class="favorites-pagination">
            <span>共 {{ favoritePage.total }} 条，第 {{ pageNo }} / {{ totalPages }} 页</span>
            <div>
              <button type="button" :disabled="!hasPreviousPage || loadingFavorites" @click="changePage(pageNo - 1)">
                上一页
              </button>
              <button type="button" :disabled="!hasNextPage || loadingFavorites" @click="changePage(pageNo + 1)">
                下一页
              </button>
            </div>
          </div>
        </section>
  </ProfileShell>
</template>

<style scoped>
.favorites-panel {
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  background: #ffffff;
  padding: 28px;
  box-shadow: 0 12px 36px rgb(15 23 42 / 0.06);
}

.favorites-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid #f1f5f9;
  padding-bottom: 22px;
}

.favorites-panel__header h1 {
  margin: 0;
  color: #111827;
  font-size: 22px;
  font-weight: 900;
}

.favorites-panel__header p {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 14px;
}

.favorites-refresh-button,
.favorites-search button,
.favorites-pagination button,
.favorites-actions button,
.favorites-edit-actions button {
  min-width: fit-content;
  height: 36px;
  border: 1px solid #dbeafe;
  border-radius: 10px;
  background: #eff6ff;
  color: #1d4ed8;
  padding: 0 14px;
  font-size: 13px;
  font-weight: 900;
  white-space: nowrap;
  cursor: pointer;
}

.favorites-refresh-button:disabled,
.favorites-search button:disabled,
.favorites-pagination button:disabled,
.favorites-actions button:disabled,
.favorites-edit-actions button:disabled {
  cursor: not-allowed;
  opacity: 0.58;
}

.favorites-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 22px;
}

.favorites-status-tabs {
  display: inline-flex;
  border: 1px solid #dbeafe;
  border-radius: 12px;
  background: #f8fafc;
  padding: 3px;
}

.favorites-status-tab {
  height: 34px;
  border: 0;
  border-radius: 9px;
  background: transparent;
  color: #64748b;
  padding: 0 14px;
  font-size: 13px;
  font-weight: 900;
  white-space: nowrap;
  cursor: pointer;
}

.favorites-status-tab.active {
  background: #2563eb;
  color: #ffffff;
  box-shadow: 0 6px 14px rgb(37 99 235 / 0.16);
}

.favorites-search {
  display: flex;
  min-width: 280px;
  gap: 8px;
}

.favorites-search input {
  width: 220px;
  height: 36px;
  border: 1px solid #dbeafe;
  border-radius: 10px;
  background: #ffffff;
  color: #0f172a;
  padding: 0 12px;
  font-size: 13px;
  font-weight: 800;
  outline: none;
}

.favorites-search input:focus,
.favorites-edit-form input:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgb(191 219 254 / 0.7);
}

.favorites-state {
  margin-top: 18px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #f8fafc;
  color: #64748b;
  padding: 14px 16px;
  font-size: 14px;
  font-weight: 800;
}

.favorites-state--error {
  border-color: #fecaca;
  background: #fef2f2;
  color: #b91c1c;
}

.favorites-state--success {
  border-color: #bbf7d0;
  background: #f0fdf4;
  color: #15803d;
}

.favorites-empty {
  display: grid;
  justify-items: center;
  gap: 14px;
  margin-top: 18px;
  border: 1px dashed #cbd5e1;
  border-radius: 12px;
  background: #f8fafc;
  color: #64748b;
  padding: 42px 16px;
  text-align: center;
  font-size: 14px;
  font-weight: 900;
}

.favorites-empty p {
  margin: 0;
}

.favorites-empty__action {
  display: inline-flex;
  height: 38px;
  align-items: center;
  justify-content: center;
  border: 1px solid #2563eb;
  border-radius: 10px;
  background: #2563eb;
  color: #ffffff;
  padding: 0 16px;
  font-size: 14px;
  font-weight: 900;
  text-decoration: none;
  box-shadow: 0 8px 18px rgb(37 99 235 / 0.16);
}

.favorites-empty__action:hover {
  background: #1d4ed8;
}

.favorites-table-wrap {
  margin-top: 18px;
  overflow-x: auto;
}

.favorites-table {
  width: 100%;
  min-width: 920px;
  border-collapse: separate;
  border-spacing: 0;
  table-layout: fixed;
}

.favorites-table th {
  border-top: 1px solid #e2e8f0;
  border-bottom: 1px solid #e2e8f0;
  background: #f8fafc;
  color: #64748b;
  padding: 12px 14px;
  font-size: 13px;
  font-weight: 900;
  text-align: center;
}

.favorites-table th:first-child {
  width: 130px;
  border-left: 1px solid #e2e8f0;
  border-radius: 10px 0 0 10px;
}

.favorites-table th:nth-child(2) {
  width: 90px;
}

.favorites-table th:nth-child(3) {
  width: 260px;
}

.favorites-table th:nth-child(4) {
  width: 150px;
}

.favorites-table th:nth-child(5) {
  width: 90px;
}

.favorites-table th:nth-child(6) {
  width: 140px;
}

.favorites-table th:last-child {
  width: 190px;
  border-right: 1px solid #e2e8f0;
  border-radius: 0 10px 10px 0;
}

.favorites-table td {
  border-bottom: 1px solid #f1f5f9;
  color: #475569;
  padding: 16px 14px;
  font-size: 14px;
  font-weight: 800;
  text-align: center;
  vertical-align: middle;
}

.favorites-table td strong {
  display: block;
  overflow-wrap: anywhere;
  color: #0f172a;
}

.favorites-table tbody tr:not(.favorites-edit-row):hover td {
  background: #f8fbff;
}

.favorite-number-group {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-width: 0;
}

.favorite-number-ball {
  display: inline-flex;
  width: 26px;
  height: 26px;
  flex: 0 0 26px;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  color: #ffffff;
  font-size: 12px;
  font-weight: 900;
}

.favorite-number-ball--front {
  background: #ef4444;
}

.favorite-number-ball--back {
  background: #2563eb;
}

.favorite-number-plus {
  color: #94a3b8;
  font-weight: 900;
}

.favorites-remark {
  overflow-wrap: anywhere;
}

.favorites-status {
  display: inline-flex;
  height: 26px;
  align-items: center;
  border-radius: 999px;
  padding: 0 10px;
  font-size: 12px;
  font-weight: 900;
}

.favorites-status--active {
  border: 1px solid #bbf7d0;
  background: #f0fdf4;
  color: #15803d;
}

.favorites-status--cancelled {
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  color: #64748b;
}

.favorites-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
}

.favorites-actions .favorites-action--warn {
  border-color: #fed7aa;
  background: #fff7ed;
  color: #c2410c;
}

.favorites-actions .favorites-action--success,
.favorites-edit-actions .favorites-save-button {
  border-color: #bbf7d0;
  background: #f0fdf4;
  color: #15803d;
}

.favorites-actions .favorites-action--danger {
  border-color: #fecaca;
  background: #fef2f2;
  color: #b91c1c;
}

.favorites-edit-row td {
  background: #f8fafc;
}

.favorites-edit-form {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) minmax(220px, 2fr) auto;
  align-items: end;
  gap: 12px;
  text-align: left;
}

.favorites-edit-form label {
  display: grid;
  gap: 8px;
  color: #334155;
  font-size: 13px;
  font-weight: 900;
}

.favorites-edit-form input {
  height: 38px;
  min-width: 0;
  border: 1px solid #dbeafe;
  border-radius: 10px;
  background: #ffffff;
  color: #0f172a;
  padding: 0 12px;
  font-size: 13px;
  font-weight: 800;
  outline: none;
}

.favorites-edit-actions {
  display: flex;
  gap: 8px;
}

.favorites-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 18px;
  color: #64748b;
  font-size: 13px;
  font-weight: 800;
}

.favorites-pagination div {
  display: flex;
  gap: 8px;
}

@media (max-width: 860px) {
  .favorites-panel {
    padding: 20px;
  }

  .favorites-panel__header,
  .favorites-toolbar,
  .favorites-pagination {
    align-items: stretch;
    flex-direction: column;
  }

  .favorites-search {
    min-width: 0;
  }

  .favorites-search input {
    width: 100%;
  }

  .favorites-edit-form {
    grid-template-columns: 1fr;
  }
}
</style>
