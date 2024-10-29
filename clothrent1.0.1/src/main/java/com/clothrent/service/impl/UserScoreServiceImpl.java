package com.clothrent.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.clothrent.entity.UserScore;
import com.clothrent.mapper.UserScoreMapper;
import com.clothrent.service.UserScoreService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 会员积分表 服务实现类
 * </p>
 *
 * @author liuqiming
 * @since 2023-02-06
 */
@Service
public class UserScoreServiceImpl extends ServiceImpl<UserScoreMapper, UserScore> implements UserScoreService {

}
