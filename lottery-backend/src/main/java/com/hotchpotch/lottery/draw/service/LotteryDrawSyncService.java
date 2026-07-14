package com.hotchpotch.lottery.draw.service;

import com.hotchpotch.lottery.common.constant.PageConstants;
import com.hotchpotch.lottery.common.exception.BusinessException;
import com.hotchpotch.lottery.common.exception.ErrorCode;
import com.hotchpotch.lottery.crawler.client.SportteryCrawlerClient;
import com.hotchpotch.lottery.crawler.record.CrawlerDraw;
import com.hotchpotch.lottery.crawler.record.CrawlerDrawResponse;
import com.hotchpotch.lottery.crawler.record.CrawlerHistoryPageResponse;
import com.hotchpotch.lottery.crawler.record.CrawlerPrizeTierResponse;
import com.hotchpotch.lottery.draw.entity.LotteryDraw;
import com.hotchpotch.lottery.draw.entity.LotteryPrizeTier;
import com.hotchpotch.lottery.draw.entity.LotterySyncTask;
import com.hotchpotch.lottery.draw.enums.LotterySyncType;
import com.hotchpotch.lottery.draw.enums.LotterySyncTaskStatus;
import com.hotchpotch.lottery.draw.enums.LotteryType;
import com.hotchpotch.lottery.draw.record.LotteryDrawSyncResult;
import com.hotchpotch.lottery.draw.record.LotterySyncTaskPageResponse;
import com.hotchpotch.lottery.draw.record.LotterySyncTaskResponse;
import com.hotchpotch.lottery.draw.repository.LotteryDrawRepository;
import com.hotchpotch.lottery.draw.repository.LotteryPrizeTierRepository;
import com.hotchpotch.lottery.draw.repository.LotterySyncTaskRepository;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 大乐透开奖同步服务。
 */
@Service
public class LotteryDrawSyncService {

    private static final String LATEST_REQUEST_PARAMS = "{\"source\":\"crawler.latest\"}";
    private static final int FAILURE_REASON_MAX_LENGTH = 1000;

    private final SportteryCrawlerClient crawlerClient;
    private final LotteryDrawRepository drawRepository;
    private final LotteryPrizeTierRepository prizeTierRepository;
    private final LotterySyncTaskRepository syncTaskRepository;

    /**
     * 初始化同步服务依赖的 crawler 客户端和数据仓储。
     */
    public LotteryDrawSyncService(
            SportteryCrawlerClient crawlerClient,
            LotteryDrawRepository drawRepository,
            LotteryPrizeTierRepository prizeTierRepository,
            LotterySyncTaskRepository syncTaskRepository) {
        this.crawlerClient = crawlerClient;
        this.drawRepository = drawRepository;
        this.prizeTierRepository = prizeTierRepository;
        this.syncTaskRepository = syncTaskRepository;
    }

    /**
     * 同步 crawler 返回的最新一期大乐透开奖。
     */
    public LotteryDrawSyncResult syncLatestDraw(String triggerSource) {
        LotterySyncTask task = createRunningTask(
                LotterySyncType.LATEST.code(),
                triggerSource,
                LATEST_REQUEST_PARAMS);
        syncTaskRepository.insert(task);

        try {
            CrawlerDraw draw = fetchLatestDraw();
            int successCount = syncOneDraw(draw) ? 1 : 0;
            int skippedCount = successCount == 1 ? 0 : 1;
            markSuccess(task, successCount, skippedCount);
            return result(task, draw.lotteryType(), draw.issueNo());
        } catch (RuntimeException ex) {
            markFailed(task, ex);
            throw ex;
        }
    }

    /**
     * 同步 crawler 返回的一页大乐透历史开奖。
     */
    public LotteryDrawSyncResult syncHistoryPage(int pageNo, int pageSize, String triggerSource) {
        LotterySyncTask task = createRunningTask(
                LotterySyncType.HISTORY_PAGE.code(),
                triggerSource,
                historyPageRequestParams(pageNo, pageSize));
        syncTaskRepository.insert(task);

        try {
            SyncCounter counter = syncHistoryPageDraws(pageNo, pageSize);
            markSuccess(task, counter.successCount, counter.skippedCount);
            return result(task, LotteryType.DLT.code(), null);
        } catch (RuntimeException ex) {
            markFailed(task, ex);
            throw ex;
        }
    }

    /**
     * 创建统一历史异步同步任务，任务由后台线程池继续执行。
     */
    public LotteryDrawSyncResult startHistorySync(
            int startPage,
            int pageSize,
            int maxPages,
            int pageDelayMillis,
            boolean stopWhenLastPage,
            String triggerSource) {
        validateHistorySyncParams(startPage, pageSize, maxPages, pageDelayMillis);
        LotterySyncTask task = createPendingHistoryTask(
                startPage,
                pageSize,
                maxPages,
                pageDelayMillis,
                stopWhenLastPage,
                triggerSource);
        syncTaskRepository.insert(task);
        return result(task, LotteryType.DLT.code(), null);
    }

    /**
     * 从失败页创建新的历史异步同步重试任务。
     */
    @Transactional(rollbackFor = RuntimeException.class)
    public LotteryDrawSyncResult retryHistorySync(String taskNo, String triggerSource) {
        LotterySyncTask failedTask = syncTaskRepository.findByTaskNo(taskNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "同步任务不存在"));
        validateRetryableHistoryTask(failedTask);

        LotterySyncTask retryTask = createPendingHistoryTask(
                failedTask.getFailedPage(),
                defaultIfNull(failedTask.getPageSize(), PageConstants.DEFAULT_PAGE_SIZE),
                defaultIfNull(failedTask.getMaxPages(), 1),
                defaultIfNull(failedTask.getPageDelayMillis(), 0),
                !Boolean.FALSE.equals(failedTask.getStopWhenLastPage()),
                triggerSource);
        syncTaskRepository.insert(retryTask);
        markRetried(failedTask);
        return result(retryTask, LotteryType.DLT.code(), null);
    }

    /**
     * 执行已经创建好的统一历史异步同步任务。
     */
    public void runHistoryTask(String taskNo) {
        LotterySyncTask task = syncTaskRepository.findByTaskNo(taskNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "同步任务不存在"));
        task.setStatus(LotterySyncTaskStatus.RUNNING.code());
        task.setStartTime(LocalDateTime.now());
        task.setFailureReason(null);
        syncTaskRepository.updateById(task);

        int successCount = zeroIfNull(task.getSuccessCount());
        int skippedCount = zeroIfNull(task.getSkippedCount());
        int startPage = defaultIfNull(task.getStartPage(), PageConstants.DEFAULT_PAGE_NO);
        int pageSize = defaultIfNull(task.getPageSize(), PageConstants.DEFAULT_PAGE_SIZE);
        int maxPages = defaultIfNull(task.getMaxPages(), 1);
        int pageDelayMillis = defaultIfNull(task.getPageDelayMillis(), 0);
        boolean stopWhenLastPage = !Boolean.FALSE.equals(task.getStopWhenLastPage());

        try {
            for (int pageNo = startPage; pageNo < startPage + maxPages; pageNo++) {
                task.setCurrentPage(pageNo);
                syncTaskRepository.updateById(task);

                CrawlerHistoryPageResponse pageResponse = fetchHistoryPageResponse(pageNo, pageSize);
                List<CrawlerDraw> draws = normalizeHistoryDraws(pageResponse);
                SyncCounter pageCounter = syncDraws(draws);
                successCount += pageCounter.successCount;
                skippedCount += pageCounter.skippedCount;

                task.setSuccessCount(successCount);
                task.setSkippedCount(skippedCount);
                task.setFailedCount(0);
                task.setLastSuccessPage(pageNo);
                task.setFailedPage(null);
                task.setFailureReason(null);
                syncTaskRepository.updateById(task);

                if (stopWhenLastPage && isLastHistoryPage(pageResponse, draws, pageNo, pageSize)) {
                    break;
                }
                if (pageNo < startPage + maxPages - 1) {
                    sleepPageDelay(pageDelayMillis);
                }
            }

            markSuccess(task, successCount, skippedCount);
        } catch (RuntimeException ex) {
            task.setFailedPage(defaultIfNull(task.getCurrentPage(), startPage));
            markFailed(task, successCount, skippedCount, 1, ex);
        }
    }

    /**
     * 按任务编号查询同步任务进度。
     */
    public LotterySyncTaskResponse findSyncTask(String taskNo) {
        return syncTaskRepository.findByTaskNo(taskNo)
                .map(this::toSyncTaskResponse)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "同步任务不存在"));
    }

    /**
     * 分页查询同步任务列表，状态为空时返回全部任务。
     */
    public LotterySyncTaskPageResponse listSyncTasks(int pageNo, int pageSize, String status) {
        int safePageNo = normalizePageNo(pageNo);
        int safePageSize = normalizePageSize(pageSize);
        String normalizedStatus = normalizeStatus(status);
        Long total = syncTaskRepository.countByStatus(normalizedStatus);
        List<LotterySyncTaskResponse> tasks = syncTaskRepository
                .findPageByStatus(normalizedStatus, safePageNo, safePageSize)
                .stream()
                .map(this::toSyncTaskResponse)
                .toList();

        return new LotterySyncTaskPageResponse(
                safePageNo,
                safePageSize,
                total,
                calculatePages(total, safePageSize),
                normalizedStatus,
                tasks);
    }

    /**
     * 创建一条运行中的开奖同步任务记录。
     */
    private LotterySyncTask createRunningTask(String syncType, String triggerSource, String requestParams) {
        LocalDateTime now = LocalDateTime.now();
        LotterySyncTask task = new LotterySyncTask();
        task.setTaskNo(LotteryType.DLT.code() + "-" + syncType + "-" + UUID.randomUUID());
        task.setLotteryType(LotteryType.DLT.code());
        task.setSyncType(syncType);
        task.setTriggerSource(triggerSource);
        task.setStatus(LotterySyncTaskStatus.RUNNING.code());
        task.setRequestParams(requestParams);
        task.setSuccessCount(0);
        task.setSkippedCount(0);
        task.setFailedCount(0);
        task.setStartTime(now);
        return task;
    }

    /**
     * 创建一条等待后台执行的历史同步任务记录。
     */
    private LotterySyncTask createPendingHistoryTask(
            int startPage,
            int pageSize,
            int maxPages,
            int pageDelayMillis,
            boolean stopWhenLastPage,
            String triggerSource) {
        LotterySyncTask task = new LotterySyncTask();
        task.setTaskNo(LotteryType.DLT.code() + "-" + LotterySyncType.HISTORY.code() + "-" + UUID.randomUUID());
        task.setLotteryType(LotteryType.DLT.code());
        task.setSyncType(LotterySyncType.HISTORY.code());
        task.setTriggerSource(triggerSource);
        task.setStatus(LotterySyncTaskStatus.PENDING.code());
        task.setRequestParams(historyRequestParams(startPage, pageSize, maxPages, pageDelayMillis, stopWhenLastPage));
        task.setStartPage(startPage);
        task.setPageSize(pageSize);
        task.setMaxPages(maxPages);
        task.setPageDelayMillis(pageDelayMillis);
        task.setStopWhenLastPage(stopWhenLastPage);
        task.setSuccessCount(0);
        task.setSkippedCount(0);
        task.setFailedCount(0);
        return task;
    }

    /**
     * 校验历史同步任务参数，避免创建无法执行或过于危险的任务。
     */
    private void validateHistorySyncParams(int startPage, int pageSize, int maxPages, int pageDelayMillis) {
        if (startPage < 1 || pageSize < 1 || maxPages < 1 || pageDelayMillis < 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "历史同步任务参数不合法");
        }
    }

    /**
     * 校验历史同步任务是否可以重试。
     */
    private void validateRetryableHistoryTask(LotterySyncTask task) {
        if (!LotterySyncType.HISTORY.code().equals(task.getSyncType())
                || !LotterySyncTaskStatus.FAILED.code().equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "只有失败的历史同步任务可以重试");
        }
        if (task.getFailedPage() == null || task.getFailedPage() < 1) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "失败任务缺少有效失败页码");
        }
    }

    /**
     * 从 crawler 拉取最新开奖数据，并校验响应体不能为空。
     */
    private CrawlerDraw fetchLatestDraw() {
        CrawlerDrawResponse response = crawlerClient.fetchLatestDraw();
        if (response == null || response.draw() == null) {
            throw new BusinessException(ErrorCode.UPSTREAM_SERVICE_ERROR, "crawler 返回开奖数据为空");
        }

        return response.draw();
    }

    /**
     * 从 crawler 拉取历史分页开奖数据，并将空列表归一化为空集合。
     */
    private List<CrawlerDraw> fetchHistoryPageDraws(int pageNo, int pageSize) {
        return normalizeHistoryDraws(fetchHistoryPageResponse(pageNo, pageSize));
    }

    /**
     * 从 crawler 拉取历史分页原始响应。
     */
    private CrawlerHistoryPageResponse fetchHistoryPageResponse(int pageNo, int pageSize) {
        return crawlerClient.fetchHistoryPage(pageNo, pageSize);
    }

    /**
     * 将历史分页响应中的开奖列表归一化为空安全集合。
     */
    private List<CrawlerDraw> normalizeHistoryDraws(CrawlerHistoryPageResponse response) {
        if (response == null || response.draws() == null) {
            return List.of();
        }

        return response.draws();
    }

    /**
     * 判断历史分页同步是否已经到达最后一页。
     */
    private boolean isLastHistoryPage(
            CrawlerHistoryPageResponse pageResponse,
            List<CrawlerDraw> draws,
            int pageNo,
            int pageSize) {
        if (pageResponse != null && pageResponse.pages() != null) {
            return pageNo >= pageResponse.pages();
        }

        return draws.isEmpty() || draws.size() < pageSize;
    }

    /**
     * 按配置等待历史分页之间的保护间隔。
     */
    private void sleepPageDelay(int pageDelayMillis) {
        if (pageDelayMillis <= 0) {
            return;
        }

        try {
            Thread.sleep(pageDelayMillis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "历史同步任务等待被中断");
        }
    }

    /**
     * 同步一页历史开奖数据，并返回当前页的新增和跳过数量。
     */
    private SyncCounter syncHistoryPageDraws(int pageNo, int pageSize) {
        return syncDraws(fetchHistoryPageDraws(pageNo, pageSize));
    }

    /**
     * 同步一组开奖数据，并累计新增和跳过数量。
     */
    private SyncCounter syncDraws(List<CrawlerDraw> draws) {
        int successCount = 0;
        int skippedCount = 0;

        for (CrawlerDraw draw : draws) {
            if (syncOneDraw(draw)) {
                successCount++;
            } else {
                skippedCount++;
            }
        }

        return new SyncCounter(successCount, skippedCount);
    }

    /**
     * 同步单期开奖数据；已存在返回 false，新入库返回 true。
     */
    private boolean syncOneDraw(CrawlerDraw draw) {
        return drawRepository.findByLotteryTypeAndIssueNo(draw.lotteryType(), draw.issueNo())
                .map(existingDraw -> {
                    fillMissingPrizeTiers(draw, existingDraw.getId());
                    return false;
                })
                .orElseGet(() -> insertNewDraw(draw));
    }

    /**
     * 插入一条新的开奖主表和对应奖级明细。
     */
    private boolean insertNewDraw(CrawlerDraw draw) {
        LotteryDraw savedDraw = toDrawEntity(draw);
        drawRepository.insert(savedDraw);
        prizeTierRepository.insertBatch(toPrizeTierEntities(draw, savedDraw.getId()));
        return true;
    }

    /**
     * 为已存在的开奖补齐缺失的奖级明细，修复历史同步中断后的半成品数据。
     */
    private void fillMissingPrizeTiers(CrawlerDraw crawlerDraw, Long drawId) {
        if (drawId == null || crawlerDraw.prizeTiers() == null || crawlerDraw.prizeTiers().isEmpty()) {
            return;
        }

        Set<String> existingPrizeNames = prizeTierRepository.findByDrawId(drawId)
                .stream()
                .map(LotteryPrizeTier::getPrizeName)
                .collect(Collectors.toCollection(HashSet::new));
        List<CrawlerPrizeTierResponse> missingPrizeTiers = crawlerDraw.prizeTiers().stream()
                .filter(prizeTier -> !existingPrizeNames.contains(prizeTier.name()))
                .toList();

        if (missingPrizeTiers.isEmpty()) {
            return;
        }

        prizeTierRepository.insertBatch(toPrizeTierEntities(crawlerDraw, drawId, missingPrizeTiers));
    }

    /**
     * 将 crawler 开奖 DTO 转换为开奖主表实体。
     */
    private LotteryDraw toDrawEntity(CrawlerDraw crawlerDraw) {
        LotteryDraw draw = new LotteryDraw();
        draw.setLotteryType(crawlerDraw.lotteryType());
        draw.setIssueNo(crawlerDraw.issueNo());
        draw.setDrawDate(crawlerDraw.drawDate());
        draw.setFrontNumbers(formatNumbers(crawlerDraw.frontNumbers()));
        draw.setBackNumbers(formatNumbers(crawlerDraw.backNumbers()));
        draw.setPoolBalance(crawlerDraw.poolBalance());
        draw.setSalesAmount(crawlerDraw.salesAmount());
        draw.setSourceUrl(crawlerDraw.source());
        draw.setPdfUrl(crawlerDraw.pdfUrl());
        draw.setFetchedTime(LocalDateTime.now());
        return draw;
    }

    /**
     * 将 crawler 奖级列表转换为开奖奖级明细实体集合。
     */
    private Collection<LotteryPrizeTier> toPrizeTierEntities(CrawlerDraw crawlerDraw, Long drawId) {
        if (crawlerDraw.prizeTiers() == null) {
            return List.of();
        }

        return toPrizeTierEntities(crawlerDraw, drawId, crawlerDraw.prizeTiers());
    }

    /**
     * 将指定 crawler 奖级列表转换为开奖奖级明细实体集合。
     */
    private Collection<LotteryPrizeTier> toPrizeTierEntities(
            CrawlerDraw crawlerDraw,
            Long drawId,
            List<CrawlerPrizeTierResponse> prizeTiers) {
        return prizeTiers.stream()
                .map(prizeTier -> toPrizeTierEntity(crawlerDraw, drawId, prizeTier))
                .toList();
    }

    /**
     * 将单条 crawler 奖级 DTO 转换为开奖奖级明细实体。
     */
    private LotteryPrizeTier toPrizeTierEntity(
            CrawlerDraw crawlerDraw,
            Long drawId,
            CrawlerPrizeTierResponse crawlerPrizeTier) {
        LotteryPrizeTier prizeTier = new LotteryPrizeTier();
        prizeTier.setDrawId(drawId);
        prizeTier.setLotteryType(crawlerDraw.lotteryType());
        prizeTier.setIssueNo(crawlerDraw.issueNo());
        prizeTier.setPrizeName(crawlerPrizeTier.name());
        prizeTier.setStakeCount(crawlerPrizeTier.stakeCount());
        prizeTier.setStakeAmount(crawlerPrizeTier.stakeAmount());
        prizeTier.setTotalPrizeAmount(crawlerPrizeTier.totalPrizeAmount());
        prizeTier.setSortOrder(crawlerPrizeTier.sort());
        prizeTier.setPrizeGroup(crawlerPrizeTier.group());
        return prizeTier;
    }

    /**
     * 将号码列表格式化为数据库存储使用的两位数字逗号分隔字符串。
     */
    private String formatNumbers(List<Integer> numbers) {
        if (numbers == null) {
            return "";
        }

        return numbers.stream()
                .map(number -> String.format("%02d", number))
                .collect(Collectors.joining(","));
    }

    /**
     * 将同步任务标记为成功，并写入成功和跳过数量。
     */
    private void markSuccess(LotterySyncTask task, int successCount, int skippedCount) {
        task.setStatus(LotterySyncTaskStatus.SUCCESS.code());
        task.setSuccessCount(successCount);
        task.setSkippedCount(skippedCount);
        task.setFailedCount(0);
        task.setFailedPage(null);
        task.setFailureReason(null);
        task.setFinishTime(LocalDateTime.now());
        syncTaskRepository.updateById(task);
    }

    /**
     * 将同步任务标记为失败，并记录失败原因摘要。
     */
    private void markFailed(LotterySyncTask task, RuntimeException ex) {
        markFailed(task, 0, 0, 1, ex);
    }

    /**
     * 将同步任务标记为失败，并保留已经完成的同步数量。
     */
    private void markFailed(
            LotterySyncTask task,
            int successCount,
            int skippedCount,
            int failedCount,
            RuntimeException ex) {
        task.setStatus(LotterySyncTaskStatus.FAILED.code());
        task.setSuccessCount(successCount);
        task.setSkippedCount(skippedCount);
        task.setFailedCount(failedCount);
        task.setFailureReason(abbreviate(ex.getMessage()));
        task.setFinishTime(LocalDateTime.now());
        syncTaskRepository.updateById(task);
    }

    /**
     * 将原失败任务标记为已发起重试，避免重复创建重试任务。
     */
    private void markRetried(LotterySyncTask task) {
        task.setStatus(LotterySyncTaskStatus.RETRIED.code());
        syncTaskRepository.updateById(task);
    }

    /**
     * 将失败原因裁剪到数据库字段允许的最大长度。
     */
    private String abbreviate(String message) {
        if (message == null || message.length() <= FAILURE_REASON_MAX_LENGTH) {
            return message;
        }

        return message.substring(0, FAILURE_REASON_MAX_LENGTH);
    }

    /**
     * 组装历史分页同步任务的请求参数 JSON。
     */
    private String historyPageRequestParams(int pageNo, int pageSize) {
        return "{\"pageNo\":" + pageNo + ",\"pageSize\":" + pageSize + "}";
    }

    /**
     * 组装统一历史同步任务的请求参数 JSON。
     */
    private String historyRequestParams(
            int startPage,
            int pageSize,
            int maxPages,
            int pageDelayMillis,
            boolean stopWhenLastPage) {
        return "{\"startPage\":" + startPage
                + ",\"pageSize\":" + pageSize
                + ",\"maxPages\":" + maxPages
                + ",\"pageDelayMillis\":" + pageDelayMillis
                + ",\"stopWhenLastPage\":" + stopWhenLastPage
                + "}";
    }

    /**
     * 根据同步任务和开奖标识组装对外返回的同步结果。
     */
    private LotteryDrawSyncResult result(LotterySyncTask task, String lotteryType, String issueNo) {
        return new LotteryDrawSyncResult(
                task.getTaskNo(),
                lotteryType,
                issueNo,
                task.getStatus(),
                task.getSuccessCount(),
                task.getSkippedCount(),
                task.getFailedCount());
    }

    /**
     * 将同步任务实体转换为任务进度响应。
     */
    private LotterySyncTaskResponse toSyncTaskResponse(LotterySyncTask task) {
        return new LotterySyncTaskResponse(
                task.getTaskNo(),
                task.getLotteryType(),
                task.getSyncType(),
                task.getTriggerSource(),
                task.getStatus(),
                task.getStartPage(),
                task.getCurrentPage(),
                task.getLastSuccessPage(),
                task.getFailedPage(),
                task.getPageSize(),
                task.getMaxPages(),
                task.getPageDelayMillis(),
                task.getStopWhenLastPage(),
                task.getSuccessCount(),
                task.getSkippedCount(),
                task.getFailedCount(),
                task.getFailureReason(),
                task.getStartTime(),
                task.getFinishTime());
    }

    /**
     * 将空整数按默认值处理。
     */
    private int defaultIfNull(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    /**
     * 将空计数按 0 处理。
     */
    private int zeroIfNull(Integer value) {
        return defaultIfNull(value, 0);
    }

    /**
     * 将页码归一化为从 1 开始的有效页码。
     */
    private int normalizePageNo(int pageNo) {
        return pageNo <= 0 ? PageConstants.DEFAULT_PAGE_NO : pageNo;
    }

    /**
     * 将分页大小归一化到允许范围内。
     */
    private int normalizePageSize(int pageSize) {
        if (pageSize <= 0) {
            return PageConstants.DEFAULT_PAGE_SIZE;
        }

        return Math.min(pageSize, PageConstants.MAX_PAGE_SIZE);
    }

    /**
     * 将状态筛选归一化为数据库中的大写状态值。
     */
    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        return status.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 根据总数和分页大小计算总页数。
     */
    private int calculatePages(Long total, int pageSize) {
        if (total == null || total <= 0) {
            return 0;
        }

        return (int) ((total + pageSize - 1) / pageSize);
    }

    /**
     * 同步数量计数器。
     */
    private static final class SyncCounter {

        private final int successCount;
        private final int skippedCount;

        /**
         * 初始化同步数量计数器。
         */
        private SyncCounter(int successCount, int skippedCount) {
            this.successCount = successCount;
            this.skippedCount = skippedCount;
        }
    }
}
