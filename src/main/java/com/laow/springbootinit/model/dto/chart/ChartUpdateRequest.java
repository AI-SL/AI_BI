package com.laow.springbootinit.model.dto.chart;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

/**
 * 更新请求
 *
 * @author <a href="https://github.com/AI-SL">laow</a>
 * @from laow
 */
@Data
public class ChartUpdateRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 图表名称
     */
    private String name;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 标签列表
     */
    private List<String> tags;

    private static final long serialVersionUID = 1L;
}