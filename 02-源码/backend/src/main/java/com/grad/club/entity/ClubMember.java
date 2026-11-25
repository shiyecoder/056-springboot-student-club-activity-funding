package com.grad.club.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ClubMember 实体，字段与 sql/init.sql 中 `club_member` 一一对应。
 */
@Data
@TableName("club_member")
public class ClubMember {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String memberNo;
    private Long clubId;
    private Long studentId;
    private Long deptId;
    private String deptName;
    private java.time.LocalDate joinDate;
    private String memberRole;
    private Integer activityCount;
    private java.math.BigDecimal serviceHours;
    private String status;
    private String quitReason;
    private String avatarUrl;
    private Long createBy;
    private java.time.LocalDateTime createTime;
    private Long updateBy;
    private java.time.LocalDateTime updateTime;
    private Integer deleted;
    private Integer version;
}
