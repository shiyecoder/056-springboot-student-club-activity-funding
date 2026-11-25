package com.grad.club.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * FundFlow 实体，字段与 sql/init.sql 中 `fund_flow` 一一对应。
 */
@Data
@TableName("fund_flow")
public class FundFlow {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String flowNo;
    private Long clubId;
    private String bizType;
    private Long bizId;
    private String direction;
    private java.math.BigDecimal amount;
    private java.math.BigDecimal balanceAfter;
    private java.time.LocalDate occurDate;
    private Long operatorId;
    private String remark;
    private Long createBy;
    private java.time.LocalDateTime createTime;
    private Long updateBy;
    private java.time.LocalDateTime updateTime;
    private Integer deleted;
    private Integer version;
}
