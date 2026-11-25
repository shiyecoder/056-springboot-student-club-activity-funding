package com.grad.club.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Budget 实体，字段与 sql/init.sql 中 `budget` 一一对应。
 */
@Data
@TableName("budget")
public class Budget {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String budgetNo;
    private Long activityId;
    private Long clubId;
    private String itemName;
    private String itemType;
    private java.math.BigDecimal applyAmount;
    private String amountBasis;
    private java.math.BigDecimal approveAmount;
    private Long auditorId;
    private java.time.LocalDateTime auditTime;
    private String status;
    private String auditRemark;
    private Long createBy;
    private java.time.LocalDateTime createTime;
    private Long updateBy;
    private java.time.LocalDateTime updateTime;
    private Integer deleted;
    private Integer version;
}
