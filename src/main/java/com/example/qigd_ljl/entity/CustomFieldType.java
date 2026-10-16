package com.example.qigd_ljl.entity;

public enum CustomFieldType {

    TEXT("文本"),
    TEXTAREA("多行文本"),
    NUMBER("数字"),
    SELECT("下拉选择"),
    MULTI_SELECT("多选"),
    DATE("日期"),
    DATETIME("日期时间"),
    BOOLEAN("开关");

    private final String label;

    CustomFieldType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}