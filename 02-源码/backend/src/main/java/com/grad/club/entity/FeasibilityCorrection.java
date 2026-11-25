package com.grad.club.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * FeasibilityCorrection 实体，字段与 sql/init.sql 中 `feasibility_correction` 一一对应。
 */
@Data
@TableName("feasibility_correction")
public class FeasibilityCorrection {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long snapshotId;
    private Long activityId;
    private java.math.BigDecimal originScore;
    private String originLevel;
    private String newLevel;
    private String reason;
    private Long operatorId;
    private java.time.LocalDateTime operTime;
    private Long createBy;
    private java.time.LocalDateTime createTime;
    private Long updateBy;
    private java.time.LocalDateTime updateTime;
    private Integer deleted;
    private Integer version;
}
