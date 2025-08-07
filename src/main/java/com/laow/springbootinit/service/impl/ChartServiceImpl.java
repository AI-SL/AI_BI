package com.laow.springbootinit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.laow.springbootinit.constant.CommonConstant;
import com.laow.springbootinit.mapper.ChartMapper;
import com.laow.springbootinit.model.entity.Chart;
import com.laow.springbootinit.utils.SqlUtils;
import com.laow.springbootinit.model.dto.chart.ChartQueryRequest;
import com.laow.springbootinit.service.ChartService;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;


/**
* @author laow
* @description 针对表【chart(图表信息表)】的数据库操作Service实现
* @createDate 2025-08-03 18:05:46
*/
@Service
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
    implements ChartService{
    /**
     * 获取查询包装类
     * @param chartQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        if (chartQueryRequest == null) {
            return queryWrapper;
        }

        String sortField = chartQueryRequest.getSortField();
        String name = chartQueryRequest.getName();
        String sortOrder = chartQueryRequest.getSortOrder();
        Long id = chartQueryRequest.getId();
        String goal = chartQueryRequest.getGoal();
        String chartType = chartQueryRequest.getChartType();
        Long userId = chartQueryRequest.getUserId();
        int current = chartQueryRequest.getCurrent();
        int pageSize = chartQueryRequest.getPageSize();
        // 拼接查询条件
        queryWrapper.eq(id != null && id>0, "id", id);
        queryWrapper.like(StringUtils.isNotBlank(name), "name", name);
        queryWrapper.eq(StringUtils.isNotBlank(goal), "goal", goal);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(StringUtils.isNotBlank(chartType), "chartType", chartType);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

}




