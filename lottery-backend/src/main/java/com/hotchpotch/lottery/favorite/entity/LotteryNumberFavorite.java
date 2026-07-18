package com.hotchpotch.lottery.favorite.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户收藏号码实体。
 */
@Getter
@Setter
@TableName("lottery_number_favorites")
public class LotteryNumberFavorite {

    /** 主键 ID。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户 ID，由业务代码保证关联有效。 */
    @TableField("user_id")
    private Long userId;

    /** 彩票类型编码，MVP 固定为 DLT。 */
    @TableField("lottery_type")
    private String lotteryType;

    /** 前区号码，升序逗号分隔。 */
    @TableField("front_numbers")
    private String frontNumbers;

    /** 后区号码，升序逗号分隔。 */
    @TableField("back_numbers")
    private String backNumbers;

    /** 收藏名称，用户可编辑。 */
    @TableField("favorite_name")
    private String favoriteName;

    /** 备注。 */
    @TableField("remark")
    private String remark;

    /** 收藏状态，取值如 ACTIVE、CANCELLED。 */
    @TableField("status")
    private String status;

    /** 首次收藏时间。 */
    @TableField("favorite_time")
    private LocalDateTime favoriteTime;

    /** 当前生效时间；重新启用时更新。 */
    @TableField("effective_time")
    private LocalDateTime effectiveTime;

    /** 取消收藏时间。 */
    @TableField("cancel_time")
    private LocalDateTime cancelTime;

    /** 创建时间。 */
    @TableField("create_time")
    private LocalDateTime createTime;

    /** 更新时间。 */
    @TableField("update_time")
    private LocalDateTime updateTime;
}
