// File: ToolType.java
package com.sandbox.client.editor.models;

public enum ToolType {
    BRUSH("Brush", "Pencil tool - paint single tiles"),
    BUCKET("Bucket", "Flood fill - fills connected area with selected tile");

    private final String name;
    private final String description;

    ToolType(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
}