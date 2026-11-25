package com.grad.club.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * FeasibilitySnapshot 实体，字段与 sql/init.sql 中 `feasibility_snapshot` 一一对应。
 */
@Data
@TableName("feasibility_snapshot")
public class FeasibilitySnapshot {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long activityId;
    private Long ruleId;
    private String ruleVersion;
    private java.math.BigDecimal scoreBudget;
    private java.math.BigDecimal scoreVenue;
    private java.math.BigDecimal scoreCapacity;
    private java.math.BigDecimal scoreHistory;
    private java.math.BigDecimal scoreClub;
    private java.math.BigDecimal totalScore;
    private String level;
    private String suggestion;
    private String unsatisfied;
    private String inputSnapshot;
    private java.time.LocalDateTime calcTime;
    private Long createBy;
    private java.time.LocalDateTime createTime;
    private Long updateBy;
    private java.time.LocalDateTime updateTime;
    private Integer deleted;
    private Integer version;
}
