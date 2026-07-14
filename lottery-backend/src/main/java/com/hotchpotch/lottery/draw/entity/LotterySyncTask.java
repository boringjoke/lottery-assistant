package com.hotchpotch.lottery.draw.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 彩票开奖同步任务实体。
 */
@Getter
@Setter
@TableName("lottery_sync_tasks")
public class LotterySyncTask {

    /** 主键 ID。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 任务编号。 */
    @TableField("task_no")
    private String taskNo;

    /** 彩票类型编码。 */
    @TableField("lottery_type")
    private String lotteryType;

    /** 同步类型，取值见 LotterySyncType。 */
    @TableField("sync_type")
    private String syncType;

    /** 触发来源，取值见 LotterySyncTriggerSource。 */
    @TableField("trigger_source")
    private String triggerSource;

    /** 任务状态，取值见 LotterySyncTaskStatus。 */
    @TableField("status")
    private String status;

    /** 同步请求参数 JSON。 */
    @TableField("request_params")
    private String requestParams;

    /** 历史同步起始页码。 */
    @TableField("start_page")
    private Integer startPage;

    /** 当前同步页码。 */
    @TableField("current_page")
    private Integer currentPage;

    /** 最后成功同步页码。 */
    @TableField("last_success_page")
    private Integer lastSuccessPage;

    /** 失败页码。 */
    @TableField("failed_page")
    private Integer failedPage;

    /** 每页数量。 */
    @TableField("page_size")
    private Integer pageSize;

    /** 最大同步页数。 */
    @TableField("max_pages")
    private Integer maxPages;

    /** 每页间隔毫秒。 */
    @TableField("page_delay_millis")
    private Integer pageDelayMillis;

    /** 遇到最后一页时是否停止。 */
    @TableField("stop_when_last_page")
    private Boolean stopWhenLastPage;

    /** 成功数量。 */
    @TableField("success_count")
    private Integer successCount;

    /** 跳过数量。 */
    @TableField("skipped_count")
    private Integer skippedCount;

    /** 失败数量。 */
    @TableField("failed_count")
    private Integer failedCount;

    /** 失败原因摘要。 */
    @TableField("failure_reason")
    private String failureReason;

    /** 开始时间。 */
    @TableField("start_time")
    private LocalDateTime startTime;

    /** 结束时间。 */
    @TableField("finish_time")
    private LocalDateTime finishTime;

    /** 创建时间。 */
    @TableField("create_time")
    private LocalDateTime createTime;

    /** 更新时间。 */
    @TableField("update_time")
    private LocalDateTime updateTime;
}
