package com.grad.club.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.club.entity.SysFile;
import org.apache.ibatis.annotations.Mapper;

/** SysFile 数据访问，继承 BaseMapper 提供单表 CRUD 与条件构造。 */
@Mapper
public interface SysFileMapper extends BaseMapper<SysFile> {
}
