package com.dog.evesystem.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dog.evesystem.doman.dto.EVECharacter;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EVECharacterMapper extends BaseMapper<EVECharacter> {
}
