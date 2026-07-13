package com.hotchpotch.lottery.draw.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class LotteryDrawSyncServiceTest {

    /**
     * 验证最新一期不存在时会写入开奖主表、奖级明细，并将同步任务标记为成功。
     */
    @Test
    void syncLatestDrawInsertsNewDrawPrizeTiersAndMarksTaskSuccess() {
        SportteryCrawlerClient crawlerClient = org.mockito.Mockito.mock(SportteryCrawlerClient.class);
        LotteryDrawRepository drawRepository = org.mockito.Mockito.mock(LotteryDrawRepository.class);
        LotteryPrizeTierRepository prizeTierRepository = org.mockito.Mockito.mock(LotteryPrizeTierRepository.class);
        LotterySyncTaskRepository syncTaskRepository = org.mockito.Mockito.mock(LotterySyncTaskRepository.class);
        LotteryDrawSyncService service = new LotteryDrawSyncService(
                crawlerClient, drawRepository, prizeTierRepository, syncTaskRepository);

        when(crawlerClient.fetchLatestDraw()).thenReturn(new CrawlerDrawResponse(sampleCrawlerDraw()));
        when(drawRepository.findByLotteryTypeAndIssueNo("DLT", "26076")).thenReturn(Optional.empty());
        when(syncTaskRepository.insert(any())).thenAnswer(invocation -> {
            LotterySyncTask task = invocation.getArgument(0);
            task.setId(9L);
            return 1;
        });
        when(drawRepository.insert(any())).thenAnswer(invocation -> {
            LotteryDraw draw = invocation.getArgument(0);
            draw.setId(88L);
            return 1;
        });
        AtomicReference<Collection<LotteryPrizeTier>> savedPrizeTiers = new AtomicReference<>();
        when(prizeTierRepository.insertBatch(any())).thenAnswer(invocation -> {
            savedPrizeTiers.set(invocation.getArgument(0));
            return 2;
        });

        LotteryDrawSyncResult result = service.syncLatestDraw("ADMIN");

        ArgumentCaptor<LotteryDraw> drawCaptor = ArgumentCaptor.forClass(LotteryDraw.class);
        ArgumentCaptor<LotterySyncTask> taskCaptor = ArgumentCaptor.forClass(LotterySyncTask.class);
        verify(drawRepository).insert(drawCaptor.capture());
        verify(prizeTierRepository).insertBatch(any());
        verify(syncTaskRepository).updateById(taskCaptor.capture());

        LotteryDraw savedDraw = drawCaptor.getValue();
        assertThat(savedDraw.getLotteryType()).isEqualTo("DLT");
        assertThat(savedDraw.getIssueNo()).isEqualTo("26076");
        assertThat(savedDraw.getDrawDate()).isEqualTo(LocalDate.of(2026, 7, 11));
        assertThat(savedDraw.getFrontNumbers()).isEqualTo("01,02,03,04,05");
        assertThat(savedDraw.getBackNumbers()).isEqualTo("06,07");
        assertThat(savedDraw.getPoolBalance()).isEqualByComparingTo("1000000.00");
        assertThat(savedDraw.getSalesAmount()).isEqualByComparingTo("500000.00");
        assertThat(savedDraw.getSourceUrl()).isEqualTo("https://www.sporttery.cn/");
        assertThat(savedDraw.getPdfUrl()).isEqualTo("https://www.sporttery.cn/dlt.pdf");
        assertThat(savedDraw.getFetchedTime()).isNotNull();

        assertThat(savedPrizeTiers.get())
                .extracting(LotteryPrizeTier::getDrawId, LotteryPrizeTier::getLotteryType,
                        LotteryPrizeTier::getIssueNo, LotteryPrizeTier::getPrizeName,
                        LotteryPrizeTier::getStakeCount, LotteryPrizeTier::getStakeAmount,
                        LotteryPrizeTier::getTotalPrizeAmount, LotteryPrizeTier::getSortOrder,
                        LotteryPrizeTier::getPrizeGroup)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(88L, "DLT", "26076", "一等奖", 2,
                                new BigDecimal("10000.00"), new BigDecimal("20000.00"), 1, "1"),
                        org.assertj.core.groups.Tuple.tuple(88L, "DLT", "26076", "二等奖", 3,
                                new BigDecimal("5000.00"), new BigDecimal("15000.00"), 2, "2"));

        LotterySyncTask finishedTask = taskCaptor.getValue();
        assertThat(finishedTask.getStatus()).isEqualTo("SUCCESS");
        assertThat(finishedTask.getSuccessCount()).isEqualTo(1);
        assertThat(finishedTask.getSkippedCount()).isZero();
        assertThat(finishedTask.getFailedCount()).isZero();
        assertThat(finishedTask.getFinishTime()).isNotNull();

        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.skippedCount()).isZero();
        assertThat(result.failedCount()).isZero();
        assertThat(result.issueNo()).isEqualTo("26076");
    }

    /**
     * 验证最新一期已存在时不会重复入库，并将同步任务记录为跳过。
     */
    @Test
    void syncLatestDrawSkipsExistingIssueAndMarksTaskSuccess() {
        SportteryCrawlerClient crawlerClient = org.mockito.Mockito.mock(SportteryCrawlerClient.class);
        LotteryDrawRepository drawRepository = org.mockito.Mockito.mock(LotteryDrawRepository.class);
        LotteryPrizeTierRepository prizeTierRepository = org.mockito.Mockito.mock(LotteryPrizeTierRepository.class);
        LotterySyncTaskRepository syncTaskRepository = org.mockito.Mockito.mock(LotterySyncTaskRepository.class);
        LotteryDrawSyncService service = new LotteryDrawSyncService(
                crawlerClient, drawRepository, prizeTierRepository, syncTaskRepository);
        LotteryDraw existingDraw = new LotteryDraw();
        existingDraw.setId(88L);
        LotteryPrizeTier existingTier = new LotteryPrizeTier();
        existingTier.setPrizeName("一等奖");
        LotteryPrizeTier existingSecondTier = new LotteryPrizeTier();
        existingSecondTier.setPrizeName("二等奖");

        when(crawlerClient.fetchLatestDraw()).thenReturn(new CrawlerDrawResponse(sampleCrawlerDraw()));
        when(drawRepository.findByLotteryTypeAndIssueNo("DLT", "26076")).thenReturn(Optional.of(existingDraw));
        when(prizeTierRepository.findByDrawId(88L)).thenReturn(List.of(existingTier, existingSecondTier));
        when(syncTaskRepository.insert(any())).thenAnswer(invocation -> {
            LotterySyncTask task = invocation.getArgument(0);
            task.setId(10L);
            return 1;
        });

        LotteryDrawSyncResult result = service.syncLatestDraw("ADMIN");

        ArgumentCaptor<LotterySyncTask> taskCaptor = ArgumentCaptor.forClass(LotterySyncTask.class);
        verify(drawRepository, never()).insert(any());
        verify(prizeTierRepository, never()).insertBatch(any());
        verify(syncTaskRepository).updateById(taskCaptor.capture());

        assertThat(taskCaptor.getValue().getStatus()).isEqualTo("SUCCESS");
        assertThat(taskCaptor.getValue().getSuccessCount()).isZero();
        assertThat(taskCaptor.getValue().getSkippedCount()).isEqualTo(1);
        assertThat(taskCaptor.getValue().getFailedCount()).isZero();
        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(result.successCount()).isZero();
        assertThat(result.skippedCount()).isEqualTo(1);
        assertThat(result.issueNo()).isEqualTo("26076");
    }

    /**
     * 验证主表已存在但奖级明细不完整时，会补齐缺失的奖级明细。
     */
    @Test
    void syncLatestDrawFillsMissingPrizeTiersWhenDrawAlreadyExists() {
        SportteryCrawlerClient crawlerClient = org.mockito.Mockito.mock(SportteryCrawlerClient.class);
        LotteryDrawRepository drawRepository = org.mockito.Mockito.mock(LotteryDrawRepository.class);
        LotteryPrizeTierRepository prizeTierRepository = org.mockito.Mockito.mock(LotteryPrizeTierRepository.class);
        LotterySyncTaskRepository syncTaskRepository = org.mockito.Mockito.mock(LotterySyncTaskRepository.class);
        LotteryDrawSyncService service = new LotteryDrawSyncService(
                crawlerClient, drawRepository, prizeTierRepository, syncTaskRepository);
        LotteryDraw existingDraw = new LotteryDraw();
        existingDraw.setId(88L);
        LotteryPrizeTier existingTier = new LotteryPrizeTier();
        existingTier.setPrizeName("一等奖");
        AtomicReference<Collection<LotteryPrizeTier>> savedPrizeTiers = new AtomicReference<>();

        when(crawlerClient.fetchLatestDraw()).thenReturn(new CrawlerDrawResponse(sampleCrawlerDraw()));
        when(drawRepository.findByLotteryTypeAndIssueNo("DLT", "26076")).thenReturn(Optional.of(existingDraw));
        when(prizeTierRepository.findByDrawId(88L)).thenReturn(List.of(existingTier));
        when(prizeTierRepository.insertBatch(any())).thenAnswer(invocation -> {
            savedPrizeTiers.set(invocation.getArgument(0));
            return 1;
        });
        when(syncTaskRepository.insert(any())).thenAnswer(invocation -> {
            LotterySyncTask task = invocation.getArgument(0);
            task.setId(14L);
            return 1;
        });

        LotteryDrawSyncResult result = service.syncLatestDraw("ADMIN");

        verify(drawRepository, never()).insert(any());
        verify(prizeTierRepository).insertBatch(any());
        assertThat(savedPrizeTiers.get())
                .extracting(LotteryPrizeTier::getPrizeName, LotteryPrizeTier::getPrizeGroup)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("二等奖", "2"));
        assertThat(result.successCount()).isZero();
        assertThat(result.skippedCount()).isEqualTo(1);
    }

    /**
     * 验证 crawler 调用失败时会更新同步任务为失败状态，并继续抛出原异常。
     */
    @Test
    void syncLatestDrawMarksTaskFailedWhenCrawlerThrowsException() {
        SportteryCrawlerClient crawlerClient = org.mockito.Mockito.mock(SportteryCrawlerClient.class);
        LotteryDrawRepository drawRepository = org.mockito.Mockito.mock(LotteryDrawRepository.class);
        LotteryPrizeTierRepository prizeTierRepository = org.mockito.Mockito.mock(LotteryPrizeTierRepository.class);
        LotterySyncTaskRepository syncTaskRepository = org.mockito.Mockito.mock(LotterySyncTaskRepository.class);
        LotteryDrawSyncService service = new LotteryDrawSyncService(
                crawlerClient, drawRepository, prizeTierRepository, syncTaskRepository);
        BusinessException upstreamError = new BusinessException(ErrorCode.UPSTREAM_SERVICE_ERROR, "crawler timeout");

        when(syncTaskRepository.insert(any())).thenAnswer(invocation -> {
            LotterySyncTask task = invocation.getArgument(0);
            task.setId(11L);
            return 1;
        });
        when(crawlerClient.fetchLatestDraw()).thenThrow(upstreamError);

        assertThatThrownBy(() -> service.syncLatestDraw("ADMIN")).isSameAs(upstreamError);

        ArgumentCaptor<LotterySyncTask> taskCaptor = ArgumentCaptor.forClass(LotterySyncTask.class);
        verify(drawRepository, never()).insert(any());
        verify(prizeTierRepository, never()).insertBatch(any());
        verify(syncTaskRepository).updateById(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getStatus()).isEqualTo("FAILED");
        assertThat(taskCaptor.getValue().getSuccessCount()).isZero();
        assertThat(taskCaptor.getValue().getSkippedCount()).isZero();
        assertThat(taskCaptor.getValue().getFailedCount()).isEqualTo(1);
        assertThat(taskCaptor.getValue().getFailureReason()).contains("crawler timeout");
        assertThat(taskCaptor.getValue().getFinishTime()).isNotNull();
    }

    /**
     * 验证历史分页同步会写入新开奖、跳过已存在开奖，并记录同步数量。
     */
    @Test
    void syncHistoryPageInsertsNewDrawsSkipsExistingDrawsAndMarksTaskSuccess() {
        SportteryCrawlerClient crawlerClient = org.mockito.Mockito.mock(SportteryCrawlerClient.class);
        LotteryDrawRepository drawRepository = org.mockito.Mockito.mock(LotteryDrawRepository.class);
        LotteryPrizeTierRepository prizeTierRepository = org.mockito.Mockito.mock(LotteryPrizeTierRepository.class);
        LotterySyncTaskRepository syncTaskRepository = org.mockito.Mockito.mock(LotterySyncTaskRepository.class);
        LotteryDrawSyncService service = new LotteryDrawSyncService(
                crawlerClient, drawRepository, prizeTierRepository, syncTaskRepository);
        CrawlerDraw newDraw = sampleCrawlerDraw("26076");
        CrawlerDraw existingCrawlerDraw = sampleCrawlerDraw("26075");
        LotteryDraw existingDraw = new LotteryDraw();
        existingDraw.setId(77L);
        LotteryPrizeTier existingFirstTier = new LotteryPrizeTier();
        existingFirstTier.setPrizeName("一等奖");
        LotteryPrizeTier existingSecondTier = new LotteryPrizeTier();
        existingSecondTier.setPrizeName("二等奖");

        when(crawlerClient.fetchHistoryPage(1, 2)).thenReturn(new CrawlerHistoryPageResponse(
                1,
                2,
                1,
                2,
                List.of(newDraw, existingCrawlerDraw)));
        when(drawRepository.findByLotteryTypeAndIssueNo("DLT", "26076")).thenReturn(Optional.empty());
        when(drawRepository.findByLotteryTypeAndIssueNo("DLT", "26075")).thenReturn(Optional.of(existingDraw));
        when(prizeTierRepository.findByDrawId(77L)).thenReturn(List.of(existingFirstTier, existingSecondTier));
        when(syncTaskRepository.insert(any())).thenAnswer(invocation -> {
            LotterySyncTask task = invocation.getArgument(0);
            task.setId(12L);
            return 1;
        });
        when(drawRepository.insert(any())).thenAnswer(invocation -> {
            LotteryDraw draw = invocation.getArgument(0);
            draw.setId(88L);
            return 1;
        });
        when(prizeTierRepository.insertBatch(any())).thenReturn(2);

        LotteryDrawSyncResult result = service.syncHistoryPage(1, 2, "ADMIN");

        ArgumentCaptor<LotterySyncTask> taskCaptor = ArgumentCaptor.forClass(LotterySyncTask.class);
        verify(drawRepository).insert(any());
        verify(prizeTierRepository).insertBatch(any());
        verify(syncTaskRepository).updateById(taskCaptor.capture());

        LotterySyncTask finishedTask = taskCaptor.getValue();
        assertThat(finishedTask.getSyncType()).isEqualTo("HISTORY_PAGE");
        assertThat(finishedTask.getRequestParams()).isEqualTo("{\"pageNo\":1,\"pageSize\":2}");
        assertThat(finishedTask.getStatus()).isEqualTo("SUCCESS");
        assertThat(finishedTask.getSuccessCount()).isEqualTo(1);
        assertThat(finishedTask.getSkippedCount()).isEqualTo(1);
        assertThat(finishedTask.getFailedCount()).isZero();
        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.skippedCount()).isEqualTo(1);
        assertThat(result.failedCount()).isZero();
    }

    /**
     * 验证历史分页 crawler 调用失败时会更新同步任务为失败状态，并继续抛出原异常。
     */
    @Test
    void syncHistoryPageMarksTaskFailedWhenCrawlerThrowsException() {
        SportteryCrawlerClient crawlerClient = org.mockito.Mockito.mock(SportteryCrawlerClient.class);
        LotteryDrawRepository drawRepository = org.mockito.Mockito.mock(LotteryDrawRepository.class);
        LotteryPrizeTierRepository prizeTierRepository = org.mockito.Mockito.mock(LotteryPrizeTierRepository.class);
        LotterySyncTaskRepository syncTaskRepository = org.mockito.Mockito.mock(LotterySyncTaskRepository.class);
        LotteryDrawSyncService service = new LotteryDrawSyncService(
                crawlerClient, drawRepository, prizeTierRepository, syncTaskRepository);
        BusinessException upstreamError = new BusinessException(ErrorCode.UPSTREAM_SERVICE_ERROR, "crawler timeout");

        when(syncTaskRepository.insert(any())).thenAnswer(invocation -> {
            LotterySyncTask task = invocation.getArgument(0);
            task.setId(13L);
            return 1;
        });
        when(crawlerClient.fetchHistoryPage(1, 20)).thenThrow(upstreamError);

        assertThatThrownBy(() -> service.syncHistoryPage(1, 20, "ADMIN")).isSameAs(upstreamError);

        ArgumentCaptor<LotterySyncTask> taskCaptor = ArgumentCaptor.forClass(LotterySyncTask.class);
        verify(drawRepository, never()).insert(any());
        verify(prizeTierRepository, never()).insertBatch(any());
        verify(syncTaskRepository).updateById(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getSyncType()).isEqualTo("HISTORY_PAGE");
        assertThat(taskCaptor.getValue().getStatus()).isEqualTo("FAILED");
        assertThat(taskCaptor.getValue().getSuccessCount()).isZero();
        assertThat(taskCaptor.getValue().getSkippedCount()).isZero();
        assertThat(taskCaptor.getValue().getFailedCount()).isEqualTo(1);
        assertThat(taskCaptor.getValue().getFailureReason()).contains("crawler timeout");
    }

    /**
     * 验证统一异步历史同步启动时只创建待执行任务，不直接抓取 crawler。
     */
    @Test
    void startHistorySyncCreatesPendingTaskWithoutFetchingCrawler() {
        SportteryCrawlerClient crawlerClient = org.mockito.Mockito.mock(SportteryCrawlerClient.class);
        LotteryDrawRepository drawRepository = org.mockito.Mockito.mock(LotteryDrawRepository.class);
        LotteryPrizeTierRepository prizeTierRepository = org.mockito.Mockito.mock(LotteryPrizeTierRepository.class);
        LotterySyncTaskRepository syncTaskRepository = org.mockito.Mockito.mock(LotterySyncTaskRepository.class);
        LotteryDrawSyncService service = new LotteryDrawSyncService(
                crawlerClient, drawRepository, prizeTierRepository, syncTaskRepository);

        when(syncTaskRepository.insert(any())).thenAnswer(invocation -> {
            LotterySyncTask task = invocation.getArgument(0);
            task.setId(19L);
            return 1;
        });

        LotteryDrawSyncResult result = service.startHistorySync(2, 20, 5, 1000, false, "ADMIN");

        ArgumentCaptor<LotterySyncTask> taskCaptor = ArgumentCaptor.forClass(LotterySyncTask.class);
        verify(syncTaskRepository).insert(taskCaptor.capture());
        verify(crawlerClient, never()).fetchHistoryPage(anyInt(), anyInt());
        LotterySyncTask pendingTask = taskCaptor.getValue();
        assertThat(pendingTask.getSyncType()).isEqualTo("HISTORY");
        assertThat(pendingTask.getStatus()).isEqualTo("PENDING");
        assertThat(pendingTask.getRequestParams()).isEqualTo(
                "{\"startPage\":2,\"pageSize\":20,\"maxPages\":5,\"pageDelayMillis\":1000,"
                        + "\"stopWhenLastPage\":false}");
        assertThat(pendingTask.getStartPage()).isEqualTo(2);
        assertThat(pendingTask.getCurrentPage()).isNull();
        assertThat(pendingTask.getLastSuccessPage()).isNull();
        assertThat(pendingTask.getFailedPage()).isNull();
        assertThat(pendingTask.getPageSize()).isEqualTo(20);
        assertThat(pendingTask.getMaxPages()).isEqualTo(5);
        assertThat(pendingTask.getPageDelayMillis()).isEqualTo(1000);
        assertThat(pendingTask.getStopWhenLastPage()).isFalse();
        assertThat(result.taskNo()).startsWith("DLT-HISTORY-");
        assertThat(result.status()).isEqualTo("PENDING");
    }

    /**
     * 验证后台历史任务会按任务参数逐页同步，并在完成后标记成功。
     */
    @Test
    void runHistoryTaskFetchesPagesUpdatesProgressAndMarksSuccess() {
        SportteryCrawlerClient crawlerClient = org.mockito.Mockito.mock(SportteryCrawlerClient.class);
        LotteryDrawRepository drawRepository = org.mockito.Mockito.mock(LotteryDrawRepository.class);
        LotteryPrizeTierRepository prizeTierRepository = org.mockito.Mockito.mock(LotteryPrizeTierRepository.class);
        LotterySyncTaskRepository syncTaskRepository = org.mockito.Mockito.mock(LotterySyncTaskRepository.class);
        LotteryDrawSyncService service = new LotteryDrawSyncService(
                crawlerClient, drawRepository, prizeTierRepository, syncTaskRepository);
        LotterySyncTask task = pendingHistoryTask("DLT-HISTORY-ASYNC-001");
        CrawlerDraw newDraw = sampleCrawlerDraw("26076");
        CrawlerDraw existingCrawlerDraw = sampleCrawlerDraw("26075");
        LotteryDraw existingDraw = new LotteryDraw();
        existingDraw.setId(77L);
        LotteryPrizeTier existingFirstTier = new LotteryPrizeTier();
        existingFirstTier.setPrizeName("一等奖");
        LotteryPrizeTier existingSecondTier = new LotteryPrizeTier();
        existingSecondTier.setPrizeName("二等奖");

        when(syncTaskRepository.findByTaskNo("DLT-HISTORY-ASYNC-001")).thenReturn(Optional.of(task));
        when(crawlerClient.fetchHistoryPage(1, 2)).thenReturn(new CrawlerHistoryPageResponse(
                1,
                2,
                2,
                3,
                List.of(newDraw)));
        when(crawlerClient.fetchHistoryPage(2, 2)).thenReturn(new CrawlerHistoryPageResponse(
                2,
                2,
                2,
                3,
                List.of(existingCrawlerDraw)));
        when(drawRepository.findByLotteryTypeAndIssueNo("DLT", "26076")).thenReturn(Optional.empty());
        when(drawRepository.findByLotteryTypeAndIssueNo("DLT", "26075")).thenReturn(Optional.of(existingDraw));
        when(prizeTierRepository.findByDrawId(77L)).thenReturn(List.of(existingFirstTier, existingSecondTier));
        when(drawRepository.insert(any())).thenAnswer(invocation -> {
            LotteryDraw draw = invocation.getArgument(0);
            draw.setId(88L);
            return 1;
        });
        when(prizeTierRepository.insertBatch(any())).thenReturn(2);

        service.runHistoryTask("DLT-HISTORY-ASYNC-001");

        verify(crawlerClient).fetchHistoryPage(1, 2);
        verify(crawlerClient).fetchHistoryPage(2, 2);
        verify(crawlerClient, never()).fetchHistoryPage(3, 2);
        assertThat(task.getStatus()).isEqualTo("SUCCESS");
        assertThat(task.getCurrentPage()).isEqualTo(2);
        assertThat(task.getLastSuccessPage()).isEqualTo(2);
        assertThat(task.getFailedPage()).isNull();
        assertThat(task.getSuccessCount()).isEqualTo(1);
        assertThat(task.getSkippedCount()).isEqualTo(1);
        assertThat(task.getFailedCount()).isZero();
        assertThat(task.getStartTime()).isNotNull();
        assertThat(task.getFinishTime()).isNotNull();
    }

    /**
     * 验证后台历史任务中途失败时会记录失败页和已完成数量。
     */
    @Test
    void runHistoryTaskMarksFailedPageAndKeepsPartialCountsWhenLaterPageThrowsException() {
        SportteryCrawlerClient crawlerClient = org.mockito.Mockito.mock(SportteryCrawlerClient.class);
        LotteryDrawRepository drawRepository = org.mockito.Mockito.mock(LotteryDrawRepository.class);
        LotteryPrizeTierRepository prizeTierRepository = org.mockito.Mockito.mock(LotteryPrizeTierRepository.class);
        LotterySyncTaskRepository syncTaskRepository = org.mockito.Mockito.mock(LotterySyncTaskRepository.class);
        LotteryDrawSyncService service = new LotteryDrawSyncService(
                crawlerClient, drawRepository, prizeTierRepository, syncTaskRepository);
        LotterySyncTask task = pendingHistoryTask("DLT-HISTORY-ASYNC-002");
        BusinessException upstreamError = new BusinessException(ErrorCode.UPSTREAM_SERVICE_ERROR, "crawler timeout");

        when(syncTaskRepository.findByTaskNo("DLT-HISTORY-ASYNC-002")).thenReturn(Optional.of(task));
        when(crawlerClient.fetchHistoryPage(1, 2)).thenReturn(new CrawlerHistoryPageResponse(
                1,
                2,
                2,
                3,
                List.of(sampleCrawlerDraw("26076"))));
        when(crawlerClient.fetchHistoryPage(2, 2)).thenThrow(upstreamError);
        when(drawRepository.findByLotteryTypeAndIssueNo("DLT", "26076")).thenReturn(Optional.empty());
        when(drawRepository.insert(any())).thenAnswer(invocation -> {
            LotteryDraw draw = invocation.getArgument(0);
            draw.setId(88L);
            return 1;
        });
        when(prizeTierRepository.insertBatch(any())).thenReturn(2);

        assertThatCode(() -> service.runHistoryTask("DLT-HISTORY-ASYNC-002")).doesNotThrowAnyException();

        assertThat(task.getStatus()).isEqualTo("FAILED");
        assertThat(task.getCurrentPage()).isEqualTo(2);
        assertThat(task.getLastSuccessPage()).isEqualTo(1);
        assertThat(task.getFailedPage()).isEqualTo(2);
        assertThat(task.getSuccessCount()).isEqualTo(1);
        assertThat(task.getSkippedCount()).isZero();
        assertThat(task.getFailedCount()).isEqualTo(1);
        assertThat(task.getFailureReason()).contains("crawler timeout");
        assertThat(task.getFinishTime()).isNotNull();
    }

    /**
     * 构造一条用于同步服务测试的 crawler 开奖样例数据。
     */
    private CrawlerDraw sampleCrawlerDraw() {
        return sampleCrawlerDraw("26076");
    }

    /**
     * 构造指定期号的 crawler 开奖样例数据。
     */
    private CrawlerDraw sampleCrawlerDraw(String issueNo) {
        return new CrawlerDraw(
                "DLT",
                issueNo,
                LocalDate.of(2026, 7, 11),
                List.of(1, 2, 3, 4, 5),
                List.of(6, 7),
                new BigDecimal("1000000.00"),
                new BigDecimal("500000.00"),
                List.of(
                        new CrawlerPrizeTierResponse("一等奖", 2, new BigDecimal("10000.00"),
                                new BigDecimal("20000.00"), 1, "1"),
                        new CrawlerPrizeTierResponse("二等奖", 3, new BigDecimal("5000.00"),
                                new BigDecimal("15000.00"), 2, "2")),
                "https://www.sporttery.cn/",
                "https://www.sporttery.cn/dlt.pdf");
    }

    /**
     * 构造一个待后台执行的历史同步任务。
     */
    private LotterySyncTask pendingHistoryTask(String taskNo) {
        LotterySyncTask task = new LotterySyncTask();
        task.setId(19L);
        task.setTaskNo(taskNo);
        task.setLotteryType("DLT");
        task.setSyncType("HISTORY");
        task.setTriggerSource("ADMIN");
        task.setStatus("PENDING");
        task.setStartPage(1);
        task.setPageSize(2);
        task.setMaxPages(2);
        task.setPageDelayMillis(0);
        task.setStopWhenLastPage(true);
        task.setSuccessCount(0);
        task.setSkippedCount(0);
        task.setFailedCount(0);
        return task;
    }
}
