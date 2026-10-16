package com.example.qigd_ljl.common;

public enum ErrorCode {

    SUCCESS(200, "成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权访问"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "资源冲突"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_ALREADY_EXISTS(1002, "用户已存在"),
    PASSWORD_ERROR(1003, "密码错误"),
    TICKET_NOT_FOUND(2001, "工单不存在"),
    TICKET_STATUS_ERROR(2002, "工单状态流转错误"),
    TICKET_ALREADY_CLOSED(2003, "工单已关闭，不能再操作"),
    CATEGORY_NOT_FOUND(3001, "分类不存在"),
    PRIORITY_NOT_FOUND(3002, "优先级不存在"),
    ROLE_NOT_FOUND(4001, "角色不存在");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}