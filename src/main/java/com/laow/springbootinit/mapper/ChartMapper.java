package com.laow.springbootinit.mapper;

import com.laow.springbootinit.model.entity.Chart;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author laow
* @description 针对表【chart(图表信息表)】的数据库操作Mapper
* @createDate 2025-08-03 18:05:46
* @Entity laow.springbootinit.model.entity.Chart
*/

@Mapper
public interface ChartMapper extends BaseMapper<Chart> {

}




