package com.Dog.Doman.dto.postgreSQL;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

// lombok 自动创建方法
@Data
// MybatisPlus 连接数据库的表名
@TableName("users")
public class PgUser {
    @TableId(type = IdType.AUTO)
    private Long userId;

    @TableField("username")
    private String username;

    @TableField("password")
    private String password;

    @TableField("email")
    private String email;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("esi_refresh_token")
    private String esiRefreshToken;
}
