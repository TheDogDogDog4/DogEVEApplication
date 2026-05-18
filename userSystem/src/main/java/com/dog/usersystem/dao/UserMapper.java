package com.dog.usersystem.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.Dog.Doman.dto.postgreSQL.PgUser;
import org.apache.ibatis.annotations.Mapper;

// User 类的 Mapper 映射文件
@Mapper
public interface UserMapper extends BaseMapper<PgUser> {
}
