package com.example.qigd_ljl.entity;

public enum AssignmentMode {

    MANUAL("手动分配"),
    AUTO("自动分配"),
    ROTATION("轮询分配"),
    SKILL_BASED("技能匹配");

    private final String label;

    AssignmentMode(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}