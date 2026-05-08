package com.dog.usersystem.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dog.usersystem.doman.po.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

// User 类的 Mapper 映射文件
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
