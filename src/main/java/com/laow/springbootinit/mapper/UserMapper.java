package com.laow.springbootinit.mapper;

import com.laow.springbootinit.model.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author laow
* @description 针对表【user(用户)】的数据库操作Mapper
* @createDate 2025-08-03 18:05:46
* @Entity laow.springbootinit.model.entity.User
*/

@Mapper
public interface UserMapper extends BaseMapper<User> {

}




