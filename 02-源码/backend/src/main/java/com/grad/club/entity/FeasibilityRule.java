package com.grad.club.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * FeasibilityRule 实体，字段与 sql/init.sql 中 `feasibility_rule` 一一对应。
 */
@Data
@TableName("feasibility_rule")
public class FeasibilityRule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String ruleName;
    private String ruleVersion;
    private java.math.BigDecimal weightBudget;
    private java.math.BigDecimal weightVenue;
    private java.math.BigDecimal weightCapacity;
    private java.math.BigDecimal weightHistory;
    private java.math.BigDecimal weightClub;
    private java.math.BigDecimal levelA;
    private java.math.BigDecimal levelB;
    private java.math.BigDecimal levelC;
    private String status;
    private java.time.LocalDateTime effectiveTime;
    private String remark;
    private Long createBy;
    private java.time.LocalDateTime createTime;
    private Long updateBy;
    private java.time.LocalDateTime updateTime;
    private Integer deleted;
    private Integer version;
}
