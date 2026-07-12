package com.hotchpotch.lottery.draw.service;

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
import com.hotchpotch.lottery.draw.record.LotteryDrawSyncResult;
import com.hotchpotch.lottery.draw.repository.LotteryDrawRepository;
import com.hotchpotch.lottery.draw.repository.LotteryPrizeTierRepository;
import com.hotchpotch.lottery.draw.repository.LotterySyncTaskRepository;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * 大乐透开奖同步服务。
 */
@Service
public class LotteryDrawSyncService {

    private static final String SYNC_TYPE_LATEST = "LATEST";
    private static final String SYNC_TYPE_HISTORY_PAGE = "HISTORY_PAGE";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String DEFAULT_LOTTERY_TYPE = "DLT";
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
        LotterySyncTask task = createRunningTask(SYNC_TYPE_LATEST, triggerSource, LATEST_REQUEST_PARAMS);
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
                SYNC_TYPE_HISTORY_PAGE,
                triggerSource,
                historyPageRequestParams(pageNo, pageSize));
        syncTaskRepository.insert(task);

        try {
            List<CrawlerDraw> draws = fetchHistoryPageDraws(pageNo, pageSize);
            int successCount = 0;
            int skippedCount = 0;

            for (CrawlerDraw draw : draws) {
                if (syncOneDraw(draw)) {
                    successCount++;
                } else {
                    skippedCount++;
                }
            }

            markSuccess(task, successCount, skippedCount);
            return result(task, DEFAULT_LOTTERY_TYPE, null);
        } catch (RuntimeException ex) {
            markFailed(task, ex);
            throw ex;
        }
    }

    /**
     * 创建一条运行中的开奖同步任务记录。
     */
    private LotterySyncTask createRunningTask(String syncType, String triggerSource, String requestParams) {
        LocalDateTime now = LocalDateTime.now();
        LotterySyncTask task = new LotterySyncTask();
        task.setTaskNo("DLT-" + syncType + "-" + UUID.randomUUID());
        task.setLotteryType(DEFAULT_LOTTERY_TYPE);
        task.setSyncType(syncType);
        task.setTriggerSource(triggerSource);
        task.setStatus(STATUS_RUNNING);
        task.setRequestParams(requestParams);
        task.setSuccessCount(0);
        task.setSkippedCount(0);
        task.setFailedCount(0);
        task.setStartTime(now);
        return task;
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
        CrawlerHistoryPageResponse response = crawlerClient.fetchHistoryPage(pageNo, pageSize);
        if (response == null || response.draws() == null) {
            return List.of();
        }

        return response.draws();
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
        task.setStatus(STATUS_SUCCESS);
        task.setSuccessCount(successCount);
        task.setSkippedCount(skippedCount);
        task.setFailedCount(0);
        task.setFailureReason(null);
        task.setFinishTime(LocalDateTime.now());
        syncTaskRepository.updateById(task);
    }

    /**
     * 将同步任务标记为失败，并记录失败原因摘要。
     */
    private void markFailed(LotterySyncTask task, RuntimeException ex) {
        task.setStatus(STATUS_FAILED);
        task.setSuccessCount(0);
        task.setSkippedCount(0);
        task.setFailedCount(1);
        task.setFailureReason(abbreviate(ex.getMessage()));
        task.setFinishTime(LocalDateTime.now());
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
}
