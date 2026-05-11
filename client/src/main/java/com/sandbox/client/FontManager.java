// FontManager.java
package com.sandbox.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;

public class FontManager {
    private static BitmapFont defaultFont;
    private static BitmapFont smallFont;

    public static void loadFonts() {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/arial.ttf"));

        // Fonte normal
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();
        parameter.size = 18;
        defaultFont = generator.generateFont(parameter);

        // Fonte pequena
        FreeTypeFontParameter smallParam = new FreeTypeFontParameter();
        smallParam.size = 14;
        smallFont = generator.generateFont(smallParam);

        generator.dispose();
    }

    public static BitmapFont getDefaultFont() {
        if (defaultFont == null) {
            loadFonts();
        }
        return defaultFont;
    }

    public static BitmapFont getSmallFont() {
        if (smallFont == null) {
            loadFonts();
        }
        return smallFont;
    }
}