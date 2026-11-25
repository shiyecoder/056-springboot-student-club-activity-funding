package com.grad.club.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.club.entity.FeasibilitySnapshot;
import org.apache.ibatis.annotations.Mapper;

/** FeasibilitySnapshot 数据访问，继承 BaseMapper 提供单表 CRUD 与条件构造。 */
@Mapper
public interface FeasibilitySnapshotMapper extends BaseMapper<FeasibilitySnapshot> {
}
