package com.grad.club.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.club.entity.ActivitySignup;
import org.apache.ibatis.annotations.Mapper;

/** ActivitySignup 数据访问，继承 BaseMapper 提供单表 CRUD 与条件构造。 */
@Mapper
public interface ActivitySignupMapper extends BaseMapper<ActivitySignup> {
}
