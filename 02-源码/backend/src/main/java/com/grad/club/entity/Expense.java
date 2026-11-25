package com.grad.club.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Expense 实体，字段与 sql/init.sql 中 `expense` 一一对应。
 */
@Data
@TableName("expense")
public class Expense {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String expenseNo;
    private Long activityId;
    private Long clubId;
    private Long budgetId;
    private Long applicantId;
    private java.math.BigDecimal amount;
    private Integer invoiceCount;
    private String invoiceUrls;
    private String payType;
    private java.time.LocalDateTime submitTime;
    private java.time.LocalDateTime payTime;
    private String status;
    private String auditRemark;
    private Long createBy;
    private java.time.LocalDateTime createTime;
    private Long updateBy;
    private java.time.LocalDateTime updateTime;
    private Integer deleted;
    private Integer version;
}
