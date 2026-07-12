package com.hotchpotch.lottery.draw.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 开奖奖级明细实体。
 */
@Getter
@Setter
@TableName("lottery_prize_tiers")
public class LotteryPrizeTier {

    /** 主键 ID。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 开奖主表 ID，由业务代码保证关联有效。 */
    @TableField("draw_id")
    private Long drawId;

    /** 彩票类型编码，冗余便于查询。 */
    @TableField("lottery_type")
    private String lotteryType;

    /** 开奖期号，冗余便于查询。 */
    @TableField("issue_no")
    private String issueNo;

    /** 奖级名称。 */
    @TableField("prize_name")
    private String prizeName;

    /** 中奖注数。 */
    @TableField("stake_count")
    private Integer stakeCount;

    /** 单注奖金。 */
    @TableField("stake_amount")
    private BigDecimal stakeAmount;

    /** 当前奖级总奖金。 */
    @TableField("total_prize_amount")
    private BigDecimal totalPrizeAmount;

    /** 官方返回排序值。 */
    @TableField("sort_order")
    private Integer sortOrder;

    /** 官方返回奖级分组编码。 */
    @TableField("prize_group")
    private String prizeGroup;

    /** 创建时间。 */
    @TableField("create_time")
    private LocalDateTime createTime;

    /** 更新时间。 */
    @TableField("update_time")
    private LocalDateTime updateTime;
}
