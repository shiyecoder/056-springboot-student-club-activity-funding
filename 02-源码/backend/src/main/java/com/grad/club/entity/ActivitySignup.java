package com.grad.club.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ActivitySignup 实体，字段与 sql/init.sql 中 `activity_signup` 一一对应。
 */
@Data
@TableName("activity_signup")
public class ActivitySignup {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String signupNo;
    private Long activityId;
    private Long studentId;
    private String signupType;
    private java.time.LocalDateTime signupTime;
    private java.time.LocalDateTime checkinTime;
    private java.time.LocalDateTime checkoutTime;
    private String checkinStatus;
    private String checkinCode;
    private Integer cancelFlag;
    private Long createBy;
    private java.time.LocalDateTime createTime;
    private Long updateBy;
    private java.time.LocalDateTime updateTime;
    private Integer deleted;
    private Integer version;
}
