package com.example.demo.service.impl;

import com.example.demo.entity.War;
import com.example.demo.mapper.WarMapper;
import com.example.demo.service.IWarService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author yangfan
 * @since 2022-01-17
 */
@Service
public class WarServiceImpl extends ServiceImpl<WarMapper, War> implements IWarService {

}
