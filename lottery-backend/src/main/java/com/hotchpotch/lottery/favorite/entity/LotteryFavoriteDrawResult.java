package com.hotchpotch.lottery.favorite.entity;

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
 * 收藏号码开奖结果实体。
 */
@Getter
@Setter
@TableName("lottery_favorite_draw_results")
public class LotteryFavoriteDrawResult {

    /** 主键 ID。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 收藏号码 ID，由业务代码保证关联有效。 */
    @TableField("favorite_id")
    private Long favoriteId;

    /** 用户 ID，冗余便于权限隔离和分页查询。 */
    @TableField("user_id")
    private Long userId;

    /** 开奖主表 ID，由业务代码保证关联有效。 */
    @TableField("draw_id")
    private Long drawId;

    /** 彩票类型编码，MVP 固定为 DLT。 */
    @TableField("lottery_type")
    private String lotteryType;

    /** 开奖期号。 */
    @TableField("issue_no")
    private String issueNo;

    /** 开奖日期。 */
    @TableField("draw_date")
    private LocalDate drawDate;

    /** 收藏前区号码快照。 */
    @TableField("favorite_front_numbers")
    private String favoriteFrontNumbers;

    /** 收藏后区号码快照。 */
    @TableField("favorite_back_numbers")
    private String favoriteBackNumbers;

    /** 开奖前区号码快照。 */
    @TableField("draw_front_numbers")
    private String drawFrontNumbers;

    /** 开奖后区号码快照。 */
    @TableField("draw_back_numbers")
    private String drawBackNumbers;

    /** 前区命中数量。 */
    @TableField("front_hit_count")
    private Integer frontHitCount;

    /** 后区命中数量。 */
    @TableField("back_hit_count")
    private Integer backHitCount;

    /** 是否中奖。 */
    @TableField("winning")
    private Boolean winning;

    /** 奖级等级，1-9；未中奖为空。 */
    @TableField("prize_level")
    private Integer prizeLevel;

    /** 奖级名称，如 一等奖、九等奖、未中奖。 */
    @TableField("prize_name")
    private String prizeName;

    /** 奖级规则版本。 */
    @TableField("rule_version")
    private String ruleVersion;

    /** 单注奖金，未知时为空。 */
    @TableField("stake_amount")
    private BigDecimal stakeAmount;

    /** 计算时间。 */
    @TableField("calculated_time")
    private LocalDateTime calculatedTime;

    /** 创建时间。 */
    @TableField("create_time")
    private LocalDateTime createTime;

    /** 更新时间。 */
    @TableField("update_time")
    private LocalDateTime updateTime;
}
