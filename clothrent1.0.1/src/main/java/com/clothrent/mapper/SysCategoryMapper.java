package com.clothrent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.clothrent.entity.SysCategory;

/**
 * <p>
 * 分类表 Mapper 接口
 * </p>
 *
 * @author liuqiming
 * @since 2023-02-06
 */
public interface SysCategoryMapper extends BaseMapper<SysCategory> {

    SysCategory findByCode(String code);

}
