package com.laow.springbootinit.model.dto.chart;

import com.laow.springbootinit.common.PageRequest;

import java.io.Serializable;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 查询请求
 *
 * @author <a href="https://github.com/AI-SL">laow</a>
 * @from laow
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ChartQueryRequest extends PageRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 名称
     */
    private String name;

    /**
     * 分析目标
     */
    private String goal;

    /**
     * 图表类型
     */
    private String chartType;

    /**
     * 状态（wait-待处理 succeed-成功  failed-失败 running-执行中）
     */
    private Integer status;

    /**
     * 执行信息
     */
    private String execMessage;

    /**
     * 用户 id
     */
    private Long userId;

    private static final long serialVersionUID = 1L;
}