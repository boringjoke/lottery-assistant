package com.hotchpotch.lottery.draw.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 彩票开奖主表实体。
 */
@Getter
@Setter
@TableName("lottery_draws")
public class LotteryDraw {

    /** 主键 ID。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 彩票类型编码，MVP 固定为 DLT。 */
    @TableField("lottery_type")
    private String lotteryType;

    /** 开奖期号。 */
    @TableField("issue_no")
    private String issueNo;

    /** 开奖日期。 */
    @TableField("draw_date")
    private LocalDate drawDate;

    /** 前区号码，升序逗号分隔。 */
    @TableField("front_numbers")
    private String frontNumbers;

    /** 后区号码，升序逗号分隔。 */
    @TableField("back_numbers")
    private String backNumbers;

    /** 开奖后奖池金额。 */
    @TableField("pool_balance")
    private BigDecimal poolBalance;

    /** 本期销售金额。 */
    @TableField("sales_amount")
    private BigDecimal salesAmount;

    /** 数据来源接口地址。 */
    @TableField("source_url")
    private String sourceUrl;

    /** 官方开奖公告 PDF 地址。 */
    @TableField("pdf_url")
    private String pdfUrl;

    /** 抓取时间。 */
    @TableField("fetched_time")
    private LocalDateTime fetchedTime;

    /** 创建时间。 */
    @TableField("create_time")
    private LocalDateTime createTime;

    /** 更新时间。 */
    @TableField("update_time")
    private LocalDateTime updateTime;
}
