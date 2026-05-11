package com.sandbox.client.editor.models;

import com.common.sandbox.model.TileTag;
import java.io.Serializable;

public class TileRef implements Serializable {
    private String spritesheetPath;
    private int tileId;
    private TileTag tag;

    public TileRef() {
        this.spritesheetPath = "";
        this.tileId = 0;
        this.tag = new TileTag("default");
    }

    public TileRef(String spritesheetPath, int tileId) {
        this.spritesheetPath = spritesheetPath != null ? spritesheetPath : "";
        this.tileId = tileId;
        this.tag = new TileTag("default");
    }

    public TileRef(String spritesheetPath, int tileId, TileTag tag) {
        this.spritesheetPath = spritesheetPath;
        this.tileId = tileId;
        this.tag = tag;
    }

    public String getSpritesheetPath() { return spritesheetPath; }

    public void setSpritesheetPath(String spritesheetPath) { this.spritesheetPath = spritesheetPath; }

    public int getTileId() { return tileId; }
    public void setTileId(int tileId) { this.tileId = tileId; }

    public TileTag getTag() { return tag; }
    public void setTag(TileTag tag) { this.tag = tag; }

    public boolean isValid() {
        return spritesheetPath != null && !spritesheetPath.isEmpty() && tileId > 0;
    }

    public void clear() {
        spritesheetPath = "";
        tileId = 0;
        tag = new TileTag("default");
    }
}