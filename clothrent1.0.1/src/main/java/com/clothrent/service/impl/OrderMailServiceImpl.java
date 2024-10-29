package com.clothrent.service.impl;

import com.clothrent.entity.OrderMail;
import com.clothrent.mapper.OrderMailMapper;
import com.clothrent.service.OrderMailService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 订单邮件记录表 服务实现类
 * </p>
 *
 * @author liuqiming
 * @since 2023-12-02
 */
@Service
public class OrderMailServiceImpl extends ServiceImpl<OrderMailMapper, OrderMail> implements OrderMailService {

}
