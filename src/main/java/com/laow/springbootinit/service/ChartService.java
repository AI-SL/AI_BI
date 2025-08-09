package com.laow.springbootinit.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.laow.springbootinit.common.TaskStatus;
import com.laow.springbootinit.manager.SparkX1Manager;
import com.laow.springbootinit.model.entity.Chart;
import com.laow.springbootinit.model.dto.chart.ChartQueryRequest;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author laow
* @description 针对表【chart(图表信息表)】的数据库操作Service
* @createDate 2025-08-03 18:05:46
*/
public interface ChartService extends IService<Chart> {
    /**
     * 获取查询包装类
     * @param chartQueryRequest
     * @return
     */
    QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest);

    /**
     * 处理图表更新失败错误
     * @param chartId
     * @param execMessage
     */
    void handleChartUpdateError(Long chartId, String execMessage);

    /**
     * 构建用户输入
     * @param chart
     * @return
     */
    List<SparkX1Manager.Message> buildUserInput(Chart chart);
}
