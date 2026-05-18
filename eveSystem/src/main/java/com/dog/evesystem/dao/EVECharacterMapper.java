package com.dog.evesystem.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.Dog.Doman.dto.postgreSQL.PgEVECharacter;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EVECharacterMapper extends BaseMapper<PgEVECharacter> {
}
