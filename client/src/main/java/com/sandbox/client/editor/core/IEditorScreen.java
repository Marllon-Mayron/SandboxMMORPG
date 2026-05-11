// File: IEditorScreen.java
package com.sandbox.client.editor.core;

public interface IEditorScreen {
    boolean isMouseOverUI(int screenX, int screenY);
    void refreshUI();
    void saveMap();
}