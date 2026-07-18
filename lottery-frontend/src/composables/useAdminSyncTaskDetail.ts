import { ref } from 'vue'

import { fetchSyncTask } from '@/api/lottery'
import type { LotterySyncTask } from '@/types/lottery'
import { getErrorMessage } from '@/utils/lotteryFormat'

export function useAdminSyncTaskDetail() {
  const detailDrawerOpen = ref(false)
  const detailLoading = ref(false)
  const detailError = ref('')
  const selectedTask = ref<LotterySyncTask | null>(null)

  async function openTaskDetail(taskNo: string) {
    detailDrawerOpen.value = true
    detailLoading.value = true
    detailError.value = ''
    selectedTask.value = null

    try {
      selectedTask.value = await fetchSyncTask(taskNo)
    } catch (err) {
      detailError.value = getErrorMessage(err, '同步任务详情加载失败，请稍后重试')
    } finally {
      detailLoading.value = false
    }
  }

  function closeTaskDetail() {
    detailDrawerOpen.value = false
    detailLoading.value = false
    detailError.value = ''
    selectedTask.value = null
  }

  return {
    detailDrawerOpen,
    detailLoading,
    detailError,
    selectedTask,
    openTaskDetail,
    closeTaskDetail,
  }
}
