package com.example.todo.model;

public enum Priority {
    HIGH("High", "danger"),
    MEDIUM("Medium", "warning"),
    LOW("Low", "success");

    private final String displayName;
    private final String cssClass;

    Priority(String displayName, String cssClass) {
        this.displayName = displayName;
        this.cssClass = cssClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCssClass() {
        return cssClass;
    }
}