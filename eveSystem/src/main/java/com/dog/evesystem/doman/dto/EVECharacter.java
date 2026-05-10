package com.dog.evesystem.doman.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("eve_character")
public class EVECharacter {
    @TableId(type = IdType.AUTO)
    private Long characterId;

    @TableField("user_id")
    private Long userId;

    @TableField("character_name")
    private String characterName;

    @TableField("expires_on")
    private LocalDateTime expiresOn;

    @TableField("scopes")
    private String scopes;

    @TableField("token_type")
    private String tokenType;

    @TableField("character_owner_hash")
    private String characterOwnerHash;

    @TableField("intellectual_property")
    private String intellectualProperty;
}
