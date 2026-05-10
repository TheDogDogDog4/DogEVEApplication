package com.Dog.Doman;

import lombok.Getter;

@Getter
public enum ResultEnum {

    SUCCESS(200, "操作成功"),
    PARAM_ERROR(400, "参数校验失败"),
    UNAUTHORIZED(401, "未登录，请先登录"),
    FORBIDDEN(403, "权限不足，无法访问"),
    BUSINESS_ERROR(422, "业务处理失败"),
    SYSTEM_ERROR(500, "服务器内部错误"),
    USERNAME_EXISTS(409, "用户名存在"),
    USERNAME_NOT_EXITS(400, "用户名不存在, 请先注册"),
    PASSWORD_INCORRECT(400, "密码错误"),
    STATE_INCORRECT(400, "识别码错误"),
    ESI_AUTH_REDIRECT_ERROR(5001, "ESI授权跳转失败"),
    ESI_STATE_INVALID(5002, "授权STATE无效或已过期"),
    ESI_TOKEN_REQUEST_ERROR(5003, "调用ESI获取令牌失败"),
    ESI_TOKEN_INVALID_ERROR(5004, "ESI令牌无效或为空"),
    ESI_TOKEN_REFRESH_ERROR(5005, "ESI令牌刷新失败"),
    ESI_USER_INFO_ERROR(5006, "获取ESI角色信息失败");

    private final int code;
    private final String msg;

    ResultEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
