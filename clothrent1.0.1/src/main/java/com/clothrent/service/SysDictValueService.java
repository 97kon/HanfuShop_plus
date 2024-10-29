package com.clothrent.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.clothrent.entity.SysDictValue;

import java.util.List;

/**
 * <p>
 * 字典值表 服务类
 * </p>
 *
 * @author liuqiming
 * @since 2021-12-11
 */
public interface SysDictValueService extends IService<SysDictValue> {

    List<SysDictValue> getValueByCode(String dictCode);

}
