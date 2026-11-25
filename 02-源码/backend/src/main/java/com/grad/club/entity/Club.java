package com.grad.club.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Club 实体，字段与 sql/init.sql 中 `club` 一一对应。
 */
@Data
@TableName("club")
public class Club {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String clubNo;
    private String clubName;
    private String clubType;
    private java.time.LocalDate foundedDate;
    private Long advisorId;
    private Long presidentId;
    private Integer memberCount;
    private java.math.BigDecimal fundLimit;
    private Integer starLevel;
    private Integer reviewYear;
    private String status;
    private String reviewRemark;
    private String coverUrl;
    private Long createBy;
    private java.time.LocalDateTime createTime;
    private Long updateBy;
    private java.time.LocalDateTime updateTime;
    private Integer deleted;
    private Integer version;
}
