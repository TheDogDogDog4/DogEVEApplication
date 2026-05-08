package com.dog.usersystem.doman.vo.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

// 用户登录请求类
@Data
public class UserLoginReq {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 14, message = "用户名必须在4 ~ 14内")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含大小写字母和数字")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度必须在6-32位")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$", message = "密码必须包含大小写字母和数字")
    private String password;
}
