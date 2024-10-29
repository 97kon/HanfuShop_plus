package com.clothrent.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.clothrent.entity.SysOrderItem;

import java.util.List;

/**
 * <p>
 * 订单明细表 服务类
 * </p>
 *
 * @author liuqiming
 * @since 2023-02-06
 */
public interface SysOrderItemService extends IService<SysOrderItem> {

    List<SysOrderItem> getByOrderId(Long orderId);

}
