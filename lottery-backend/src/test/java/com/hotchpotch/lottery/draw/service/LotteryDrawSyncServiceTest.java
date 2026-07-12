package com.hotchpotch.lottery.draw.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hotchpotch.lottery.common.exception.BusinessException;
import com.hotchpotch.lottery.common.exception.ErrorCode;
import com.hotchpotch.lottery.crawler.client.SportteryCrawlerClient;
import com.hotchpotch.lottery.crawler.record.CrawlerDraw;
import com.hotchpotch.lottery.crawler.record.CrawlerDrawResponse;
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

        when(crawlerClient.fetchLatestDraw()).thenReturn(new CrawlerDrawResponse(sampleCrawlerDraw()));
        when(drawRepository.findByLotteryTypeAndIssueNo("DLT", "26076")).thenReturn(Optional.of(existingDraw));
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
     * 构造一条用于同步服务测试的 crawler 开奖样例数据。
     */
    private CrawlerDraw sampleCrawlerDraw() {
        return new CrawlerDraw(
                "DLT",
                "26076",
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
}
