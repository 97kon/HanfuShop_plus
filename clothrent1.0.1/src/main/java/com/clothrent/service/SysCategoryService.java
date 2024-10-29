package com.clothrent.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.clothrent.entity.SysCategory;

/**
 * <p>
 * 分类表 服务类
 * </p>
 *
 * @author liuqiming
 * @since 2023-02-06
 */
public interface SysCategoryService extends IService<SysCategory> {

    SysCategory findByCode(String code);

}
