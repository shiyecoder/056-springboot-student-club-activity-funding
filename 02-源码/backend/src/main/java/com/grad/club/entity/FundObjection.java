package com.grad.club.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * FundObjection 实体，字段与 sql/init.sql 中 `fund_objection` 一一对应。
 */
@Data
@TableName("fund_objection")
public class FundObjection {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String objectionNo;
    private Long disclosureId;
    private Long clubId;
    private String itemType;
    private String itemName;
    private java.math.BigDecimal amount;
    private String reason;
    private Long applicantId;
    private String handleStatus;
    private String replyContent;
    private java.time.LocalDateTime replyTime;
    private Long handlerId;
    private Long createBy;
    private java.time.LocalDateTime createTime;
    private Long updateBy;
    private java.time.LocalDateTime updateTime;
    private Integer deleted;
    private Integer version;
}
