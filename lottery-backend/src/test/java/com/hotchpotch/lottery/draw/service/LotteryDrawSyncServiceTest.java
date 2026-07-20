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
import com.hotchpotch.lottery.draw.record.LotterySyncTaskPageResponse;
import com.hotchpotch.lottery.draw.record.LotterySyncTaskStatisticsResponse;
import com.hotchpotch.lottery.draw.repository.LotteryDrawRepository;
import com.hotchpotch.lottery.draw.repository.LotteryPrizeTierRepository;
import com.hotchpotch.lottery.draw.repository.LotterySyncTaskRepository;
import com.hotchpotch.lottery.favorite.service.LotteryFavoriteDrawResultGenerateService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
     * 验证新开奖首次入库成功后会触发收藏开奖结果生成。
     */
    @Test
    void syncLatestDrawGeneratesFavoriteDrawResultsAfterInsertingNewDraw() {
        SportteryCrawlerClient crawlerClient = org.mockito.Mockito.mock(SportteryCrawlerClient.class);
        LotteryDrawRepository drawRepository = org.mockito.Mockito.mock(LotteryDrawRepository.class);
        LotteryPrizeTierRepository prizeTierRepository = org.mockito.Mockito.mock(LotteryPrizeTierRepository.class);
        LotterySyncTaskRepository syncTaskRepository = org.mockito.Mockito.mock(LotterySyncTaskRepository.class);
        LotteryFavoriteDrawResultGenerateService favoriteDrawResultGenerateService =
                org.mockito.Mockito.mock(LotteryFavoriteDrawResultGenerateService.class);
        LotteryDrawSyncService service = new LotteryDrawSyncService(
                crawlerClient,
                drawRepository,
                prizeTierRepository,
                syncTaskRepository,
                favoriteDrawResultGenerateService);

        when(crawlerClient.fetchLatestDraw()).thenReturn(new CrawlerDrawResponse(sampleCrawlerDraw()));
        when(drawRepository.findByLotteryTypeAndIssueNo("DLT", "26076")).thenReturn(Optional.empty());
        when(syncTaskRepository.insert(any())).thenAnswer(invocation -> {
            LotterySyncTask task = invocation.getArgument(0);
            task.setId(15L);
            return 1;
        });
        when(drawRepository.insert(any())).thenAnswer(invocation -> {
            LotteryDraw draw = invocation.getArgument(0);
            draw.setId(88L);
            return 1;
        });
        when(prizeTierRepository.insertBatch(any())).thenReturn(2);
        when(favoriteDrawResultGenerateService.generateForDraw(any())).thenReturn(1);

        LotteryDrawSyncResult result = service.syncLatestDraw("ADMIN");

        ArgumentCaptor<LotteryDraw> drawCaptor = ArgumentCaptor.forClass(LotteryDraw.class);
        verify(favoriteDrawResultGenerateService).generateForDraw(drawCaptor.capture());
        assertThat(drawCaptor.getValue().getId()).isEqualTo(88L);
        assertThat(drawCaptor.getValue().getIssueNo()).isEqualTo("26076");
        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(result.successCount()).isEqualTo(1);
    }

    /**
     * 验证已存在开奖跳过时不会重复触发收藏开奖结果生成。
     */
    @Test
    void syncLatestDrawDoesNotGenerateFavoriteDrawResultsWhenDrawAlreadyExists() {
        SportteryCrawlerClient crawlerClient = org.mockito.Mockito.mock(SportteryCrawlerClient.class);
        LotteryDrawRepository drawRepository = org.mockito.Mockito.mock(LotteryDrawRepository.class);
        LotteryPrizeTierRepository prizeTierRepository = org.mockito.Mockito.mock(LotteryPrizeTierRepository.class);
        LotterySyncTaskRepository syncTaskRepository = org.mockito.Mockito.mock(LotterySyncTaskRepository.class);
        LotteryFavoriteDrawResultGenerateService favoriteDrawResultGenerateService =
                org.mockito.Mockito.mock(LotteryFavoriteDrawResultGenerateService.class);
        LotteryDrawSyncService service = new LotteryDrawSyncService(
                crawlerClient,
                drawRepository,
                prizeTierRepository,
                syncTaskRepository,
                favoriteDrawResultGenerateService);
        LotteryDraw existingDraw = new LotteryDraw();
        existingDraw.setId(88L);
        LotteryPrizeTier existingFirstTier = new LotteryPrizeTier();
        existingFirstTier.setPrizeName("一等奖");
        LotteryPrizeTier existingSecondTier = new LotteryPrizeTier();
        existingSecondTier.setPrizeName("二等奖");

        when(crawlerClient.fetchLatestDraw()).thenReturn(new CrawlerDrawResponse(sampleCrawlerDraw()));
        when(drawRepository.findByLotteryTypeAndIssueNo("DLT", "26076")).thenReturn(Optional.of(existingDraw));
        when(prizeTierRepository.findByDrawId(88L)).thenReturn(List.of(existingFirstTier, existingSecondTier));
        when(syncTaskRepository.insert(any())).thenAnswer(invocation -> {
            LotterySyncTask task = invocation.getArgument(0);
            task.setId(16L);
            return 1;
        });

        LotteryDrawSyncResult result = service.syncLatestDraw("ADMIN");

        verify(favoriteDrawResultGenerateService, never()).generateForDraw(any());
        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(result.successCount()).isZero();
        assertThat(result.skippedCount()).isEqualTo(1);
    }

    /**
     * 验证收藏开奖结果生成失败不影响开奖同步成功状态。
     */
    @Test
    void syncLatestDrawStillSucceedsWhenFavoriteDrawResultGenerationFails() {
        SportteryCrawlerClient crawlerClient = org.mockito.Mockito.mock(SportteryCrawlerClient.class);
        LotteryDrawRepository drawRepository = org.mockito.Mockito.mock(LotteryDrawRepository.class);
        LotteryPrizeTierRepository prizeTierRepository = org.mockito.Mockito.mock(LotteryPrizeTierRepository.class);
        LotterySyncTaskRepository syncTaskRepository = org.mockito.Mockito.mock(LotterySyncTaskRepository.class);
        LotteryFavoriteDrawResultGenerateService favoriteDrawResultGenerateService =
                org.mockito.Mockito.mock(LotteryFavoriteDrawResultGenerateService.class);
        LotteryDrawSyncService service = new LotteryDrawSyncService(
                crawlerClient,
                drawRepository,
                prizeTierRepository,
                syncTaskRepository,
                favoriteDrawResultGenerateService);

        when(crawlerClient.fetchLatestDraw()).thenReturn(new CrawlerDrawResponse(sampleCrawlerDraw()));
        when(drawRepository.findByLotteryTypeAndIssueNo("DLT", "26076")).thenReturn(Optional.empty());
        when(syncTaskRepository.insert(any())).thenAnswer(invocation -> {
            LotterySyncTask task = invocation.getArgument(0);
            task.setId(17L);
            return 1;
        });
        when(drawRepository.insert(any())).thenAnswer(invocation -> {
            LotteryDraw draw = invocation.getArgument(0);
            draw.setId(88L);
            return 1;
        });
        when(prizeTierRepository.insertBatch(any())).thenReturn(2);
        when(favoriteDrawResultGenerateService.generateForDraw(any()))
                .thenThrow(new BusinessException(ErrorCode.INTERNAL_ERROR, "收藏历史生成失败"));

        LotteryDrawSyncResult result = service.syncLatestDraw("ADMIN");

        ArgumentCaptor<LotterySyncTask> taskCaptor = ArgumentCaptor.forClass(LotterySyncTask.class);
        verify(syncTaskRepository).updateById(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getStatus()).isEqualTo("SUCCESS");
        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failedCount()).isZero();
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
     * 验证按期号范围同步启动时只创建待执行任务，并把范围边界写入请求参数。
     */
    @Test
    void startIssueRangeSyncCreatesPendingTaskWithoutFetchingCrawler() {
        SportteryCrawlerClient crawlerClient = org.mockito.Mockito.mock(SportteryCrawlerClient.class);
        LotteryDrawRepository drawRepository = org.mockito.Mockito.mock(LotteryDrawRepository.class);
        LotteryPrizeTierRepository prizeTierRepository = org.mockito.Mockito.mock(LotteryPrizeTierRepository.class);
        LotterySyncTaskRepository syncTaskRepository = org.mockito.Mockito.mock(LotterySyncTaskRepository.class);
        LotteryDrawSyncService service = new LotteryDrawSyncService(
                crawlerClient, drawRepository, prizeTierRepository, syncTaskRepository);

        when(syncTaskRepository.insert(any())).thenAnswer(invocation -> {
            LotterySyncTask task = invocation.getArgument(0);
            task.setId(21L);
            return 1;
        });

        LotteryDrawSyncResult result = service.startIssueRangeSync(
                "26070", "26076", 2, 20, 5, 1000, false, "ADMIN");

        ArgumentCaptor<LotterySyncTask> taskCaptor = ArgumentCaptor.forClass(LotterySyncTask.class);
        verify(syncTaskRepository).insert(taskCaptor.capture());
        verify(crawlerClient, never()).fetchHistoryPage(anyInt(), anyInt());
        LotterySyncTask pendingTask = taskCaptor.getValue();
        assertThat(pendingTask.getSyncType()).isEqualTo("ISSUE_RANGE");
        assertThat(pendingTask.getStatus()).isEqualTo("PENDING");
        assertThat(pendingTask.getRequestParams()).isEqualTo(
                "{\"startIssueNo\":\"26070\",\"endIssueNo\":\"26076\",\"startPage\":2,"
                        + "\"pageSize\":20,\"maxPages\":5,\"pageDelayMillis\":1000,"
                        + "\"stopWhenLastPage\":false}");
        assertThat(pendingTask.getStartPage()).isEqualTo(2);
        assertThat(pendingTask.getPageSize()).isEqualTo(20);
        assertThat(pendingTask.getMaxPages()).isEqualTo(5);
        assertThat(pendingTask.getPageDelayMillis()).isEqualTo(1000);
        assertThat(pendingTask.getStopWhenLastPage()).isFalse();
        assertThat(result.taskNo()).startsWith("DLT-ISSUE_RANGE-");
        assertThat(result.status()).isEqualTo("PENDING");
    }

    /**
     * 验证按日期范围同步启动时只创建待执行任务，并把日期边界写入请求参数。
     */
    @Test
    void startDateRangeSyncCreatesPendingTaskWithoutFetchingCrawler() {
        SportteryCrawlerClient crawlerClient = org.mockito.Mockito.mock(SportteryCrawlerClient.class);
        LotteryDrawRepository drawRepository = org.mockito.Mockito.mock(LotteryDrawRepository.class);
        LotteryPrizeTierRepository prizeTierRepository = org.mockito.Mockito.mock(LotteryPrizeTierRepository.class);
        LotterySyncTaskRepository syncTaskRepository = org.mockito.Mockito.mock(LotterySyncTaskRepository.class);
        LotteryDrawSyncService service = new LotteryDrawSyncService(
                crawlerClient, drawRepository, prizeTierRepository, syncTaskRepository);

        when(syncTaskRepository.insert(any())).thenAnswer(invocation -> {
            LotterySyncTask task = invocation.getArgument(0);
            task.setId(22L);
            return 1;
        });

        LotteryDrawSyncResult result = service.startDateRangeSync(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 11),
                2,
                20,
                5,
                1000,
                false,
                "ADMIN");

        ArgumentCaptor<LotterySyncTask> taskCaptor = ArgumentCaptor.forClass(LotterySyncTask.class);
        verify(syncTaskRepository).insert(taskCaptor.capture());
        verify(crawlerClient, never()).fetchHistoryPage(anyInt(), anyInt());
        LotterySyncTask pendingTask = taskCaptor.getValue();
        assertThat(pendingTask.getSyncType()).isEqualTo("DATE_RANGE");
        assertThat(pendingTask.getStatus()).isEqualTo("PENDING");
        assertThat(pendingTask.getRequestParams()).isEqualTo(
                "{\"startDate\":\"2026-07-01\",\"endDate\":\"2026-07-11\",\"startPage\":2,"
                        + "\"pageSize\":20,\"maxPages\":5,\"pageDelayMillis\":1000,"
                        + "\"stopWhenLastPage\":false}");
        assertThat(result.taskNo()).startsWith("DLT-DATE_RANGE-");
        assertThat(result.status()).isEqualTo("PENDING");
    }

    /**
     * 验证期号范围同步会拒绝空期号。
     */
    @Test
    void startIssueRangeSyncRejectsBlankIssueNo() {
        LotteryDrawSyncService service = newSyncServiceWithMocks();

        assertThatThrownBy(() -> service.startIssueRangeSync(
                " ",
                "26076",
                1,
                20,
                1,
                0,
                true,
                "ADMIN"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("起始期号不合法");
    }

    /**
     * 验证期号范围同步会拒绝非数字期号。
     */
    @Test
    void startIssueRangeSyncRejectsNonNumericIssueNo() {
        LotteryDrawSyncService service = newSyncServiceWithMocks();

        assertThatThrownBy(() -> service.startIssueRangeSync(
                "26070A",
                "26076",
                1,
                20,
                1,
                0,
                true,
                "ADMIN"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("起始期号不合法");
    }

    /**
     * 验证期号范围同步会拒绝起始期号大于结束期号。
     */
    @Test
    void startIssueRangeSyncRejectsReversedIssueRange() {
        LotteryDrawSyncService service = newSyncServiceWithMocks();

        assertThatThrownBy(() -> service.startIssueRangeSync(
                "26076",
                "26070",
                1,
                20,
                1,
                0,
                true,
                "ADMIN"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("起始期号不能大于结束期号");
    }

    /**
     * 验证日期范围同步会拒绝空日期。
     */
    @Test
    void startDateRangeSyncRejectsNullDate() {
        LotteryDrawSyncService service = newSyncServiceWithMocks();

        assertThatThrownBy(() -> service.startDateRangeSync(
                null,
                LocalDate.of(2026, 7, 11),
                1,
                20,
                1,
                0,
                true,
                "ADMIN"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("日期范围不能为空");
    }

    /**
     * 验证日期范围同步会拒绝起始日期晚于结束日期。
     */
    @Test
    void startDateRangeSyncRejectsReversedDateRange() {
        LotteryDrawSyncService service = newSyncServiceWithMocks();

        assertThatThrownBy(() -> service.startDateRangeSync(
                LocalDate.of(2026, 7, 11),
                LocalDate.of(2026, 7, 1),
                1,
                20,
                1,
                0,
                true,
                "ADMIN"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("起始日期不能晚于结束日期");
    }

    /**
     * 验证范围同步会拒绝非法分页参数。
     */
    @Test
    void startRangeSyncRejectsInvalidPagingParams() {
        LotteryDrawSyncService service = newSyncServiceWithMocks();

        assertThatThrownBy(() -> service.startIssueRangeSync(
                "26070",
                "26076",
                0,
                20,
                1,
                0,
                true,
                "ADMIN"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("历史同步任务参数不合法");

        assertThatThrownBy(() -> service.startDateRangeSync(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 11),
                1,
                0,
                1,
                0,
                true,
                "ADMIN"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("历史同步任务参数不合法");
    }

    /**
     * 验证失败历史任务可以从失败页创建新的待执行重试任务。
     */
    @Test
    void retrySyncTaskCreatesPendingTaskFromFailedPage() {
        SportteryCrawlerClient crawlerClient = org.mockito.Mockito.mock(SportteryCrawlerClient.class);
        LotteryDrawRepository drawRepository = org.mockito.Mockito.mock(LotteryDrawRepository.class);
        LotteryPrizeTierRepository prizeTierRepository = org.mockito.Mockito.mock(LotteryPrizeTierRepository.class);
        LotterySyncTaskRepository syncTaskRepository = org.mockito.Mockito.mock(LotterySyncTaskRepository.class);
        LotteryDrawSyncService service = new LotteryDrawSyncService(
                crawlerClient, drawRepository, prizeTierRepository, syncTaskRepository);
        LotterySyncTask failedTask = pendingHistoryTask("DLT-HISTORY-FAILED-001");
        failedTask.setStatus("FAILED");
        failedTask.setFailedPage(3);
        failedTask.setPageSize(20);
        failedTask.setMaxPages(5);
        failedTask.setPageDelayMillis(3000);
        failedTask.setStopWhenLastPage(false);

        when(syncTaskRepository.findByTaskNo("DLT-HISTORY-FAILED-001")).thenReturn(Optional.of(failedTask));
        when(syncTaskRepository.insert(any())).thenAnswer(invocation -> {
            LotterySyncTask task = invocation.getArgument(0);
            task.setId(20L);
            return 1;
        });

        LotteryDrawSyncResult result = service.retrySyncTask("DLT-HISTORY-FAILED-001", "ADMIN");

        ArgumentCaptor<LotterySyncTask> taskCaptor = ArgumentCaptor.forClass(LotterySyncTask.class);
        verify(syncTaskRepository).insert(taskCaptor.capture());
        verify(syncTaskRepository).updateById(failedTask);
        LotterySyncTask retryTask = taskCaptor.getValue();
        assertThat(failedTask.getStatus()).isEqualTo("RETRIED");
        assertThat(retryTask.getStatus()).isEqualTo("PENDING");
        assertThat(retryTask.getSyncType()).isEqualTo("HISTORY");
        assertThat(retryTask.getStartPage()).isEqualTo(3);
        assertThat(retryTask.getPageSize()).isEqualTo(20);
        assertThat(retryTask.getMaxPages()).isEqualTo(5);
        assertThat(retryTask.getPageDelayMillis()).isEqualTo(3000);
        assertThat(retryTask.getStopWhenLastPage()).isFalse();
        assertThat(result.taskNo()).startsWith("DLT-HISTORY-");
        assertThat(result.status()).isEqualTo("PENDING");
    }

    /**
     * 验证失败的期号范围任务可以保留范围参数并从失败页创建重试任务。
     */
    @Test
    void retrySyncTaskCreatesIssueRangeRetryTaskFromFailedPage() {
        SportteryCrawlerClient crawlerClient = org.mockito.Mockito.mock(SportteryCrawlerClient.class);
        LotteryDrawRepository drawRepository = org.mockito.Mockito.mock(LotteryDrawRepository.class);
        LotteryPrizeTierRepository prizeTierRepository = org.mockito.Mockito.mock(LotteryPrizeTierRepository.class);
        LotterySyncTaskRepository syncTaskRepository = org.mockito.Mockito.mock(LotterySyncTaskRepository.class);
        LotteryDrawSyncService service = new LotteryDrawSyncService(
                crawlerClient, drawRepository, prizeTierRepository, syncTaskRepository);
        LotterySyncTask failedTask = pendingIssueRangeTask("DLT-ISSUE-RANGE-FAILED-001");
        failedTask.setStatus("FAILED");
        failedTask.setFailedPage(3);
        failedTask.setPageSize(20);
        failedTask.setMaxPages(5);
        failedTask.setPageDelayMillis(3000);
        failedTask.setStopWhenLastPage(false);

        when(syncTaskRepository.findByTaskNo("DLT-ISSUE-RANGE-FAILED-001")).thenReturn(Optional.of(failedTask));
        when(syncTaskRepository.insert(any())).thenAnswer(invocation -> {
            LotterySyncTask task = invocation.getArgument(0);
            task.setId(23L);
            return 1;
        });

        LotteryDrawSyncResult result = service.retrySyncTask("DLT-ISSUE-RANGE-FAILED-001", "ADMIN");

        ArgumentCaptor<LotterySyncTask> taskCaptor = ArgumentCaptor.forClass(LotterySyncTask.class);
        verify(syncTaskRepository).insert(taskCaptor.capture());
        verify(syncTaskRepository).updateById(failedTask);
        LotterySyncTask retryTask = taskCaptor.getValue();
        assertThat(failedTask.getStatus()).isEqualTo("RETRIED");
        assertThat(retryTask.getStatus()).isEqualTo("PENDING");
        assertThat(retryTask.getSyncType()).isEqualTo("ISSUE_RANGE");
        assertThat(retryTask.getStartPage()).isEqualTo(3);
        assertThat(retryTask.getRequestParams()).contains("\"startIssueNo\":\"26070\"");
        assertThat(retryTask.getRequestParams()).contains("\"endIssueNo\":\"26076\"");
        assertThat(result.taskNo()).startsWith("DLT-ISSUE_RANGE-");
        assertThat(result.status()).isEqualTo("PENDING");
    }

    /**
     * 验证非失败状态的历史任务不能重试。
     */
    @Test
    void retrySyncTaskRejectsTaskThatIsNotFailed() {
        SportteryCrawlerClient crawlerClient = org.mockito.Mockito.mock(SportteryCrawlerClient.class);
        LotteryDrawRepository drawRepository = org.mockito.Mockito.mock(LotteryDrawRepository.class);
        LotteryPrizeTierRepository prizeTierRepository = org.mockito.Mockito.mock(LotteryPrizeTierRepository.class);
        LotterySyncTaskRepository syncTaskRepository = org.mockito.Mockito.mock(LotterySyncTaskRepository.class);
        LotteryDrawSyncService service = new LotteryDrawSyncService(
                crawlerClient, drawRepository, prizeTierRepository, syncTaskRepository);
        LotterySyncTask runningTask = pendingHistoryTask("DLT-HISTORY-RUNNING-001");
        runningTask.setStatus("RUNNING");

        when(syncTaskRepository.findByTaskNo("DLT-HISTORY-RUNNING-001")).thenReturn(Optional.of(runningTask));

        assertThatThrownBy(() -> service.retrySyncTask("DLT-HISTORY-RUNNING-001", "ADMIN"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只有失败的历史同步任务可以重试");

        verify(syncTaskRepository, never()).insert(any());
    }

    /**
     * 验证已经发起过重试的历史任务不能重复重试。
     */
    @Test
    void retrySyncTaskRejectsTaskThatHasBeenRetried() {
        SportteryCrawlerClient crawlerClient = org.mockito.Mockito.mock(SportteryCrawlerClient.class);
        LotteryDrawRepository drawRepository = org.mockito.Mockito.mock(LotteryDrawRepository.class);
        LotteryPrizeTierRepository prizeTierRepository = org.mockito.Mockito.mock(LotteryPrizeTierRepository.class);
        LotterySyncTaskRepository syncTaskRepository = org.mockito.Mockito.mock(LotterySyncTaskRepository.class);
        LotteryDrawSyncService service = new LotteryDrawSyncService(
                crawlerClient, drawRepository, prizeTierRepository, syncTaskRepository);
        LotterySyncTask retriedTask = pendingHistoryTask("DLT-HISTORY-RETRIED-001");
        retriedTask.setStatus("RETRIED");
        retriedTask.setFailedPage(3);

        when(syncTaskRepository.findByTaskNo("DLT-HISTORY-RETRIED-001")).thenReturn(Optional.of(retriedTask));

        assertThatThrownBy(() -> service.retrySyncTask("DLT-HISTORY-RETRIED-001", "ADMIN"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只有失败的历史同步任务可以重试");

        verify(syncTaskRepository, never()).insert(any());
        verify(syncTaskRepository, never()).updateById(any());
    }

    /**
     * 验证存在任意活跃任务时，失败任务重试也不能创建新的待执行任务。
     */
    @Test
    void retrySyncTaskRejectsWhenAnyActiveTaskExists() {
        SportteryCrawlerClient crawlerClient = org.mockito.Mockito.mock(SportteryCrawlerClient.class);
        LotteryDrawRepository drawRepository = org.mockito.Mockito.mock(LotteryDrawRepository.class);
        LotteryPrizeTierRepository prizeTierRepository = org.mockito.Mockito.mock(LotteryPrizeTierRepository.class);
        LotterySyncTaskRepository syncTaskRepository = org.mockito.Mockito.mock(LotterySyncTaskRepository.class);
        LotteryDrawSyncService service = new LotteryDrawSyncService(
                crawlerClient, drawRepository, prizeTierRepository, syncTaskRepository);
        LotterySyncTask failedTask = pendingHistoryTask("DLT-HISTORY-FAILED-003");
        failedTask.setStatus("FAILED");
        failedTask.setFailedPage(3);
        LotterySyncTask runningTask = pendingDateRangeTask("DLT-DATE-RANGE-RUNNING-003");
        runningTask.setStatus("RUNNING");

        when(syncTaskRepository.findByTaskNo("DLT-HISTORY-FAILED-003")).thenReturn(Optional.of(failedTask));
        when(syncTaskRepository.findAnyActiveTask("DLT")).thenReturn(Optional.of(runningTask));

        assertThatThrownBy(() -> service.retrySyncTask("DLT-HISTORY-FAILED-003", "ADMIN"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("当前已有同步任务正在执行");

        verify(syncTaskRepository).findAnyActiveTask("DLT");
        verify(syncTaskRepository, never()).insert(any());
        verify(syncTaskRepository, never()).updateById(failedTask);
    }

    /**
     * 验证同步任务列表会归一化分页参数、转换状态筛选，并返回分页响应。
     */
    @Test
    void listSyncTasksReturnsPagedTasksWithNormalizedParams() {
        SportteryCrawlerClient crawlerClient = org.mockito.Mockito.mock(SportteryCrawlerClient.class);
        LotteryDrawRepository drawRepository = org.mockito.Mockito.mock(LotteryDrawRepository.class);
        LotteryPrizeTierRepository prizeTierRepository = org.mockito.Mockito.mock(LotteryPrizeTierRepository.class);
        LotterySyncTaskRepository syncTaskRepository = org.mockito.Mockito.mock(LotterySyncTaskRepository.class);
        LotteryDrawSyncService service = new LotteryDrawSyncService(
                crawlerClient, drawRepository, prizeTierRepository, syncTaskRepository);
        LotterySyncTask failedTask = pendingHistoryTask("DLT-HISTORY-FAILED-001");
        failedTask.setStatus("FAILED");
        failedTask.setCurrentPage(3);
        failedTask.setLastSuccessPage(2);
        failedTask.setFailedPage(3);
        failedTask.setFailureReason("crawler timeout");

        when(syncTaskRepository.countByStatus("FAILED")).thenReturn(1L);
        when(syncTaskRepository.findPageByStatus("FAILED", 1, 20)).thenReturn(List.of(failedTask));

        LotterySyncTaskPageResponse response = service.listSyncTasks(0, 0, " failed ");

        assertThat(response.pageNo()).isEqualTo(1);
        assertThat(response.pageSize()).isEqualTo(20);
        assertThat(response.total()).isEqualTo(1L);
        assertThat(response.pages()).isEqualTo(1);
        assertThat(response.status()).isEqualTo("FAILED");
        assertThat(response.tasks()).hasSize(1);
        assertThat(response.tasks().get(0).taskNo()).isEqualTo("DLT-HISTORY-FAILED-001");
        assertThat(response.tasks().get(0).failedPage()).isEqualTo(3);
        assertThat(response.tasks().get(0).requestParamMap())
                .containsEntry("startPage", "1")
                .containsEntry("pageSize", "2")
                .containsEntry("maxPages", "2");
        verify(syncTaskRepository).countByStatus("FAILED");
        verify(syncTaskRepository).findPageByStatus("FAILED", 1, 20);
    }

    /**
     * 验证同步任务统计会聚合状态数量、今日成功数和最近失败摘要。
     */
    @Test
    void getSyncTaskStatisticsReturnsCountsAndLatestFailure() {
        SportteryCrawlerClient crawlerClient = org.mockito.Mockito.mock(SportteryCrawlerClient.class);
        LotteryDrawRepository drawRepository = org.mockito.Mockito.mock(LotteryDrawRepository.class);
        LotteryPrizeTierRepository prizeTierRepository = org.mockito.Mockito.mock(LotteryPrizeTierRepository.class);
        LotterySyncTaskRepository syncTaskRepository = org.mockito.Mockito.mock(LotterySyncTaskRepository.class);
        LotteryDrawSyncService service = new LotteryDrawSyncService(
                crawlerClient, drawRepository, prizeTierRepository, syncTaskRepository);
        LotterySyncTask successTask = pendingHistoryTask("DLT-HISTORY-SUCCESS-001");
        successTask.setStatus("SUCCESS");
        successTask.setFinishTime(LocalDateTime.of(2026, 7, 16, 10, 0));
        LotterySyncTask failedTask = pendingHistoryTask("DLT-HISTORY-FAILED-002");
        failedTask.setStatus("FAILED");
        failedTask.setFinishTime(LocalDateTime.of(2026, 7, 16, 11, 0));
        failedTask.setFailureReason("crawler timeout");

        when(syncTaskRepository.countByStatus("RUNNING")).thenReturn(1L);
        when(syncTaskRepository.countByStatus("PENDING")).thenReturn(2L);
        when(syncTaskRepository.countByStatus("FAILED")).thenReturn(3L);
        when(syncTaskRepository.countByStatusSince(org.mockito.ArgumentMatchers.eq("SUCCESS"), any()))
                .thenReturn(4L);
        when(syncTaskRepository.findLatestByStatus("SUCCESS")).thenReturn(Optional.of(successTask));
        when(syncTaskRepository.findLatestByStatus("FAILED")).thenReturn(Optional.of(failedTask));

        LotterySyncTaskStatisticsResponse response = service.getSyncTaskStatistics();

        assertThat(response.runningCount()).isEqualTo(1L);
        assertThat(response.pendingCount()).isEqualTo(2L);
        assertThat(response.failedCount()).isEqualTo(3L);
        assertThat(response.successCountToday()).isEqualTo(4L);
        assertThat(response.latestSuccessTime()).isEqualTo(LocalDateTime.of(2026, 7, 16, 10, 0));
        assertThat(response.latestFailureTime()).isEqualTo(LocalDateTime.of(2026, 7, 16, 11, 0));
        assertThat(response.latestFailureMessage()).isEqualTo("crawler timeout");
    }

    /**
     * 验证存在任意活跃任务时，不再创建新的统一历史同步任务。
     */
    @Test
    void startHistorySyncRejectsWhenAnyActiveTaskExists() {
        SportteryCrawlerClient crawlerClient = org.mockito.Mockito.mock(SportteryCrawlerClient.class);
        LotteryDrawRepository drawRepository = org.mockito.Mockito.mock(LotteryDrawRepository.class);
        LotteryPrizeTierRepository prizeTierRepository = org.mockito.Mockito.mock(LotteryPrizeTierRepository.class);
        LotterySyncTaskRepository syncTaskRepository = org.mockito.Mockito.mock(LotterySyncTaskRepository.class);
        LotteryDrawSyncService service = new LotteryDrawSyncService(
                crawlerClient, drawRepository, prizeTierRepository, syncTaskRepository);
        LotterySyncTask runningTask = pendingHistoryTask("DLT-HISTORY-RUNNING-001");
        runningTask.setStatus("RUNNING");
        when(syncTaskRepository.findAnyActiveTask("DLT")).thenReturn(Optional.of(runningTask));

        assertThatThrownBy(() -> service.startHistorySync(1, 20, 5, 0, true, "ADMIN"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("当前已有同步任务正在执行");

        verify(syncTaskRepository).findAnyActiveTask("DLT");
        verify(syncTaskRepository, never()).insert(any());
    }

    /**
     * 验证已有运行中的其他类型任务时，再创建日期范围任务会按全局活跃任务直接拒绝。
     */
    @Test
    void startDateRangeSyncRejectsWhenAnotherSyncTypeIsActive() {
        SportteryCrawlerClient crawlerClient = org.mockito.Mockito.mock(SportteryCrawlerClient.class);
        LotteryDrawRepository drawRepository = org.mockito.Mockito.mock(LotteryDrawRepository.class);
        LotteryPrizeTierRepository prizeTierRepository = org.mockito.Mockito.mock(LotteryPrizeTierRepository.class);
        LotterySyncTaskRepository syncTaskRepository = org.mockito.Mockito.mock(LotterySyncTaskRepository.class);
        LotteryDrawSyncService service = new LotteryDrawSyncService(
                crawlerClient, drawRepository, prizeTierRepository, syncTaskRepository);
        LotterySyncTask runningTask = pendingHistoryTask("DLT-HISTORY-RUNNING-001");
        runningTask.setStatus("RUNNING");
        when(syncTaskRepository.findAnyActiveTask("DLT")).thenReturn(Optional.of(runningTask));

        assertThatThrownBy(() -> service.startDateRangeSync(
                        LocalDate.of(2025, 6, 1),
                        LocalDate.of(2026, 6, 30),
                        1,
                        10,
                        5,
                        3000,
                        true,
                        "ADMIN"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("当前已有同步任务正在执行");

        verify(syncTaskRepository).findAnyActiveTask("DLT");
        verify(syncTaskRepository, never()).insert(any());
    }

    /**
     * 验证应用启动恢复会将遗留 PENDING/RUNNING 任务标记为失败并保留可重试进度。
     */
    @Test
    void recoverInterruptedActiveTasksMarksPendingAndRunningTasksFailed() {
        SportteryCrawlerClient crawlerClient = org.mockito.Mockito.mock(SportteryCrawlerClient.class);
        LotteryDrawRepository drawRepository = org.mockito.Mockito.mock(LotteryDrawRepository.class);
        LotteryPrizeTierRepository prizeTierRepository = org.mockito.Mockito.mock(LotteryPrizeTierRepository.class);
        LotterySyncTaskRepository syncTaskRepository = org.mockito.Mockito.mock(LotterySyncTaskRepository.class);
        LotteryDrawSyncService service = new LotteryDrawSyncService(
                crawlerClient, drawRepository, prizeTierRepository, syncTaskRepository);
        LotterySyncTask pendingTask = pendingHistoryTask("DLT-HISTORY-PENDING-002");
        LotterySyncTask runningTask = pendingHistoryTask("DLT-HISTORY-RUNNING-002");
        runningTask.setStatus("RUNNING");
        runningTask.setCurrentPage(4);
        runningTask.setLastSuccessPage(3);
        when(syncTaskRepository.findByStatus("PENDING")).thenReturn(List.of(pendingTask));
        when(syncTaskRepository.findByStatus("RUNNING")).thenReturn(List.of(runningTask));

        int recoveredCount = service.recoverInterruptedActiveTasks();

        assertThat(recoveredCount).isEqualTo(2);
        assertThat(pendingTask.getStatus()).isEqualTo("FAILED");
        assertThat(pendingTask.getFailedPage()).isEqualTo(1);
        assertThat(pendingTask.getFailureReason()).contains("应用重启导致任务中断");
        assertThat(pendingTask.getFinishTime()).isNotNull();
        assertThat(runningTask.getStatus()).isEqualTo("FAILED");
        assertThat(runningTask.getFailedPage()).isEqualTo(4);
        assertThat(runningTask.getLastSuccessPage()).isEqualTo(3);
        assertThat(runningTask.getFailureReason()).contains("应用重启导致任务中断");
        assertThat(runningTask.getFinishTime()).isNotNull();
        verify(syncTaskRepository).updateById(pendingTask);
        verify(syncTaskRepository).updateById(runningTask);
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
     * 验证通用任务执行入口会按同步类型分发到历史同步逻辑。
     */
    @Test
    void runTaskDispatchesHistoryTaskBySyncType() {
        SportteryCrawlerClient crawlerClient = org.mockito.Mockito.mock(SportteryCrawlerClient.class);
        LotteryDrawRepository drawRepository = org.mockito.Mockito.mock(LotteryDrawRepository.class);
        LotteryPrizeTierRepository prizeTierRepository = org.mockito.Mockito.mock(LotteryPrizeTierRepository.class);
        LotterySyncTaskRepository syncTaskRepository = org.mockito.Mockito.mock(LotterySyncTaskRepository.class);
        LotteryDrawSyncService service = new LotteryDrawSyncService(
                crawlerClient, drawRepository, prizeTierRepository, syncTaskRepository);
        LotterySyncTask task = pendingHistoryTask("DLT-HISTORY-ASYNC-003");

        when(syncTaskRepository.findByTaskNo("DLT-HISTORY-ASYNC-003")).thenReturn(Optional.of(task));
        when(crawlerClient.fetchHistoryPage(1, 2)).thenReturn(new CrawlerHistoryPageResponse(
                1,
                2,
                1,
                0,
                List.of()));

        service.runTask("DLT-HISTORY-ASYNC-003");

        verify(crawlerClient).fetchHistoryPage(1, 2);
        assertThat(task.getStatus()).isEqualTo("SUCCESS");
    }

    /**
     * 验证后台期号范围任务会逐页过滤范围内开奖，并到达范围下界后停止。
     */
    @Test
    void runIssueRangeTaskFiltersPagesUpdatesProgressAndMarksSuccess() {
        SportteryCrawlerClient crawlerClient = org.mockito.Mockito.mock(SportteryCrawlerClient.class);
        LotteryDrawRepository drawRepository = org.mockito.Mockito.mock(LotteryDrawRepository.class);
        LotteryPrizeTierRepository prizeTierRepository = org.mockito.Mockito.mock(LotteryPrizeTierRepository.class);
        LotterySyncTaskRepository syncTaskRepository = org.mockito.Mockito.mock(LotterySyncTaskRepository.class);
        LotteryDrawSyncService service = new LotteryDrawSyncService(
                crawlerClient, drawRepository, prizeTierRepository, syncTaskRepository);
        LotterySyncTask task = pendingIssueRangeTask("DLT-ISSUE-RANGE-ASYNC-001");
        LotteryDraw existingDraw = new LotteryDraw();
        existingDraw.setId(77L);
        LotteryPrizeTier existingFirstTier = new LotteryPrizeTier();
        existingFirstTier.setPrizeName("一等奖");
        LotteryPrizeTier existingSecondTier = new LotteryPrizeTier();
        existingSecondTier.setPrizeName("二等奖");

        when(syncTaskRepository.findByTaskNo("DLT-ISSUE-RANGE-ASYNC-001")).thenReturn(Optional.of(task));
        when(crawlerClient.fetchHistoryPage(1, 2)).thenReturn(new CrawlerHistoryPageResponse(
                1,
                2,
                3,
                6,
                List.of(sampleCrawlerDraw("26078"), sampleCrawlerDraw("26076"))));
        when(crawlerClient.fetchHistoryPage(2, 2)).thenReturn(new CrawlerHistoryPageResponse(
                2,
                2,
                3,
                6,
                List.of(sampleCrawlerDraw("26075"), sampleCrawlerDraw("26070"))));
        when(drawRepository.findByLotteryTypeAndIssueNo("DLT", "26076")).thenReturn(Optional.empty());
        when(drawRepository.findByLotteryTypeAndIssueNo("DLT", "26075")).thenReturn(Optional.of(existingDraw));
        when(drawRepository.findByLotteryTypeAndIssueNo("DLT", "26070")).thenReturn(Optional.empty());
        when(prizeTierRepository.findByDrawId(77L)).thenReturn(List.of(existingFirstTier, existingSecondTier));
        when(drawRepository.insert(any())).thenAnswer(invocation -> {
            LotteryDraw draw = invocation.getArgument(0);
            draw.setId(88L);
            return 1;
        });
        when(prizeTierRepository.insertBatch(any())).thenReturn(2);

        service.runTask("DLT-ISSUE-RANGE-ASYNC-001");

        verify(crawlerClient).fetchHistoryPage(1, 2);
        verify(crawlerClient).fetchHistoryPage(2, 2);
        verify(crawlerClient, never()).fetchHistoryPage(3, 2);
        assertThat(task.getStatus()).isEqualTo("SUCCESS");
        assertThat(task.getCurrentPage()).isEqualTo(2);
        assertThat(task.getLastSuccessPage()).isEqualTo(2);
        assertThat(task.getFailedPage()).isNull();
        assertThat(task.getSuccessCount()).isEqualTo(2);
        assertThat(task.getSkippedCount()).isEqualTo(1);
        assertThat(task.getFailedCount()).isZero();
        assertThat(task.getStartTime()).isNotNull();
        assertThat(task.getFinishTime()).isNotNull();
    }

    /**
     * 验证后台日期范围任务会逐页过滤范围内开奖，并到达日期下界后停止。
     */
    @Test
    void runDateRangeTaskFiltersPagesUpdatesProgressAndMarksSuccess() {
        SportteryCrawlerClient crawlerClient = org.mockito.Mockito.mock(SportteryCrawlerClient.class);
        LotteryDrawRepository drawRepository = org.mockito.Mockito.mock(LotteryDrawRepository.class);
        LotteryPrizeTierRepository prizeTierRepository = org.mockito.Mockito.mock(LotteryPrizeTierRepository.class);
        LotterySyncTaskRepository syncTaskRepository = org.mockito.Mockito.mock(LotterySyncTaskRepository.class);
        LotteryDrawSyncService service = new LotteryDrawSyncService(
                crawlerClient, drawRepository, prizeTierRepository, syncTaskRepository);
        LotterySyncTask task = pendingDateRangeTask("DLT-DATE-RANGE-ASYNC-001");

        when(syncTaskRepository.findByTaskNo("DLT-DATE-RANGE-ASYNC-001")).thenReturn(Optional.of(task));
        when(crawlerClient.fetchHistoryPage(1, 2)).thenReturn(new CrawlerHistoryPageResponse(
                1,
                2,
                3,
                6,
                List.of(
                        sampleCrawlerDraw("26078", LocalDate.of(2026, 7, 14)),
                        sampleCrawlerDraw("26076", LocalDate.of(2026, 7, 11)))));
        when(crawlerClient.fetchHistoryPage(2, 2)).thenReturn(new CrawlerHistoryPageResponse(
                2,
                2,
                3,
                6,
                List.of(
                        sampleCrawlerDraw("26075", LocalDate.of(2026, 7, 7)),
                        sampleCrawlerDraw("26070", LocalDate.of(2026, 6, 30)))));
        when(drawRepository.findByLotteryTypeAndIssueNo("DLT", "26076")).thenReturn(Optional.empty());
        when(drawRepository.findByLotteryTypeAndIssueNo("DLT", "26075")).thenReturn(Optional.empty());
        when(drawRepository.insert(any())).thenAnswer(invocation -> {
            LotteryDraw draw = invocation.getArgument(0);
            draw.setId(88L);
            return 1;
        });
        when(prizeTierRepository.insertBatch(any())).thenReturn(2);

        service.runTask("DLT-DATE-RANGE-ASYNC-001");

        verify(crawlerClient).fetchHistoryPage(1, 2);
        verify(crawlerClient).fetchHistoryPage(2, 2);
        verify(crawlerClient, never()).fetchHistoryPage(3, 2);
        assertThat(task.getStatus()).isEqualTo("SUCCESS");
        assertThat(task.getCurrentPage()).isEqualTo(2);
        assertThat(task.getLastSuccessPage()).isEqualTo(2);
        assertThat(task.getSuccessCount()).isEqualTo(2);
        assertThat(task.getSkippedCount()).isZero();
        assertThat(task.getFailedCount()).isZero();
    }

    /**
     * 构造一条用于同步服务测试的 crawler 开奖样例数据。
     */
    private CrawlerDraw sampleCrawlerDraw() {
        return sampleCrawlerDraw("26076");
    }

    /**
     * 构造一个使用 mock 依赖的同步服务。
     */
    private LotteryDrawSyncService newSyncServiceWithMocks() {
        return new LotteryDrawSyncService(
                org.mockito.Mockito.mock(SportteryCrawlerClient.class),
                org.mockito.Mockito.mock(LotteryDrawRepository.class),
                org.mockito.Mockito.mock(LotteryPrizeTierRepository.class),
                org.mockito.Mockito.mock(LotterySyncTaskRepository.class));
    }

    /**
     * 构造指定期号的 crawler 开奖样例数据。
     */
    private CrawlerDraw sampleCrawlerDraw(String issueNo) {
        return sampleCrawlerDraw(issueNo, LocalDate.of(2026, 7, 11));
    }

    /**
     * 构造指定期号和开奖日期的 crawler 开奖样例数据。
     */
    private CrawlerDraw sampleCrawlerDraw(String issueNo, LocalDate drawDate) {
        return new CrawlerDraw(
                "DLT",
                issueNo,
                drawDate,
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
        task.setRequestParams("{\"startPage\":1,\"pageSize\":2,\"maxPages\":2,"
                + "\"pageDelayMillis\":0,\"stopWhenLastPage\":true}");
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

    /**
     * 构造一个待后台执行的期号范围同步任务。
     */
    private LotterySyncTask pendingIssueRangeTask(String taskNo) {
        LotterySyncTask task = new LotterySyncTask();
        task.setId(21L);
        task.setTaskNo(taskNo);
        task.setLotteryType("DLT");
        task.setSyncType("ISSUE_RANGE");
        task.setTriggerSource("ADMIN");
        task.setStatus("PENDING");
        task.setRequestParams("{\"startIssueNo\":\"26070\",\"endIssueNo\":\"26076\",\"startPage\":1,"
                + "\"pageSize\":2,\"maxPages\":3,\"pageDelayMillis\":0,\"stopWhenLastPage\":true}");
        task.setStartPage(1);
        task.setPageSize(2);
        task.setMaxPages(3);
        task.setPageDelayMillis(0);
        task.setStopWhenLastPage(true);
        task.setSuccessCount(0);
        task.setSkippedCount(0);
        task.setFailedCount(0);
        return task;
    }

    /**
     * 构造一个待后台执行的日期范围同步任务。
     */
    private LotterySyncTask pendingDateRangeTask(String taskNo) {
        LotterySyncTask task = new LotterySyncTask();
        task.setId(22L);
        task.setTaskNo(taskNo);
        task.setLotteryType("DLT");
        task.setSyncType("DATE_RANGE");
        task.setTriggerSource("ADMIN");
        task.setStatus("PENDING");
        task.setRequestParams("{\"startDate\":\"2026-07-01\",\"endDate\":\"2026-07-11\",\"startPage\":1,"
                + "\"pageSize\":2,\"maxPages\":3,\"pageDelayMillis\":0,\"stopWhenLastPage\":true}");
        task.setStartPage(1);
        task.setPageSize(2);
        task.setMaxPages(3);
        task.setPageDelayMillis(0);
        task.setStopWhenLastPage(true);
        task.setSuccessCount(0);
        task.setSkippedCount(0);
        task.setFailedCount(0);
        return task;
    }
}

