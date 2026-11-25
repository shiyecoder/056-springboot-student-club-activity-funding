package com.grad.club.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * FundDisclosure 实体，字段与 sql/init.sql 中 `fund_disclosure` 一一对应。
 */
@Data
@TableName("fund_disclosure")
public class FundDisclosure {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String disclosureNo;
    private Long clubId;
    private String period;
    private java.math.BigDecimal incomeAmount;
    private java.math.BigDecimal expenseAmount;
    private java.math.BigDecimal balanceAmount;
    private Integer activityCount;
    private java.time.LocalDate publishStart;
    private java.time.LocalDate publishEnd;
    private Integer objectionCount;
    private String status;
    private String noticeUrl;
    private Long createBy;
    private java.time.LocalDateTime createTime;
    private Long updateBy;
    private java.time.LocalDateTime updateTime;
    private Integer deleted;
    private Integer version;
}
