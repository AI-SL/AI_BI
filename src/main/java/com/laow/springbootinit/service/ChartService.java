package com.laow.springbootinit.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.laow.springbootinit.model.entity.Chart;
import com.laow.springbootinit.model.dto.chart.ChartQueryRequest;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author laow
* @description 针对表【chart(图表信息表)】的数据库操作Service
* @createDate 2025-08-03 18:05:46
*/
public interface ChartService extends IService<Chart> {
    QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest);
}
