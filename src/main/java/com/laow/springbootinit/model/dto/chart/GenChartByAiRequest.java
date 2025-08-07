package com.laow.springbootinit.model.dto.chart;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新请求
 *
 * @author <a href="https://github.com/AI-SL">laow</a>
 * @from laow
 */
@Data
public class GenChartByAiRequest implements Serializable {

    /**
     * 名称
     */
    private String name;

    /**
     * 目标
     */
    private String goal;

    /**
     * 图表类型
     */
    private String chartType;

    private static final long serialVersionUID = 1L;
}