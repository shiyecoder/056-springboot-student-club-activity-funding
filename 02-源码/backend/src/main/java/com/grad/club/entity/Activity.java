package com.grad.club.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Activity 实体，字段与 sql/init.sql 中 `activity` 一一对应。
 */
@Data
@TableName("activity")
public class Activity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String activityNo;
    private Long clubId;
    private String activityName;
    private String activityType;
    private java.time.LocalDateTime startTime;
    private java.time.LocalDateTime endTime;
    private String placeName;
    private Integer placeCapacity;
    private Integer expectCount;
    private Integer actualCount;
    private java.math.BigDecimal budgetTotal;
    private java.math.BigDecimal spentAmount;
    private java.math.BigDecimal feasibilityScore;
    private String planDesc;
    private String coverUrl;
    private String status;
    private String rejectReason;
    private Integer submitCount;
    private Long createBy;
    private java.time.LocalDateTime createTime;
    private Long updateBy;
    private java.time.LocalDateTime updateTime;
    private Integer deleted;
    private Integer version;
}
