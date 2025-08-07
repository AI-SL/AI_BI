package com.laow.springbootinit.model.vo;

import lombok.Data;

/**
 * BI的返回结果
 */
@Data
public class BiResponse {
    /**
     * 生成的图表数据
     */
    private String genChart;

    /**
     * 生成的分析结果
     */
    private String genResult;

    /**
     * 图表id
     */
    private Long chartId;
}
