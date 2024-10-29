package com.clothrent.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.clothrent.entity.SysCart;
import com.clothrent.entity.SysOrder;
import com.clothrent.entity.UserAddress;

import java.util.List;

/**
 * <p>
 * 订单表 服务类
 * </p>
 *
 * @author liuqiming
 * @since 2023-02-06
 */
public interface SysOrderService extends IService<SysOrder> {

    SysOrder generateOrder(List<SysCart> cartList, UserAddress userAddress,String remark,String ifhz,String ifpic,String rentDay);

    SysOrder generateOrder(Long goodsId, Long number, UserAddress userAddress,String remark,String clothSize,String ifhz,String ifpic,String rentDay);
}
