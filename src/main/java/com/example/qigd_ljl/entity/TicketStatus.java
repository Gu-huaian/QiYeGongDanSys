package com.example.qigd_ljl.entity;

public enum TicketStatus {

    PENDING("待处理"),
    ASSIGNED("已分配"),
    PROCESSING("处理中"),
    PAUSED("已暂停"),
    RESOLVED("已解决"),
    CLOSED("已关闭"),
    REOPENED("已重新打开");

    private final String label;

    TicketStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}