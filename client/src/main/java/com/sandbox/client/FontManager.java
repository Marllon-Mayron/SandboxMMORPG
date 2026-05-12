package com.sandbox.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class FontManager {
    private static final Logger logger = LoggerFactory.getLogger(FontManager.class);

    private static FontManager instance;
    private final Map<String, BitmapFont> fonts;
    private final Map<String, FreeTypeFontGenerator> generators;
    private SpriteBatch batch; // Adicionado o batch aqui

    // Font sizes (pixels)
    private static final int FONT_SIZE_TINY = 12;
    private static final int FONT_SIZE_SMALL = 14;
    private static final int FONT_SIZE_NORMAL = 16;
    private static final int FONT_SIZE_MEDIUM = 18;
    private static final int FONT_SIZE_LARGE = 22;
    private static final int FONT_SIZE_HUGE = 28;
    private static final int FONT_SIZE_TITLE = 36;

    // Font keys
    public static final String TINY = "tiny";
    public static final String SMALL = "small";
    public static final String NORMAL = "normal";
    public static final String MEDIUM = "medium";
    public static final String LARGE = "large";
    public static final String HUGE = "huge";
    public static final String TITLE = "title";

    // Font styles
    public static final String DEFAULT = "default";
    public static final String BOLD = "bold";
    public static final String OUTLINED = "outlined";

    private FontManager() {
        fonts = new HashMap<>();
        generators = new HashMap<>();
        batch = new SpriteBatch(); // Inicializa o batch
        loadFonts();
    }

    public static FontManager getInstance() {
        if (instance == null) {
            instance = new FontManager();
        }
        return instance;
    }

    private void loadFonts() {
        // Try multiple font paths (fallback mechanism)
        String[] fontPaths = {
                "fonts/arial.ttf",
                "fonts/Roboto-Regular.ttf",
                "fonts/OpenSans-Regular.ttf",
                "fonts/arialbd.ttf",
                "fonts/segoeui.ttf",
                "fonts/DejaVuSans.ttf"
        };

        String fontPath = findExistingFont(fontPaths);

        if (fontPath == null) {
            logger.error("No font file found! Using default BitmapFont (may be blurry)");
            createFallbackFonts();
            return;
        }

        logger.info("Loading fonts from: {}", fontPath);

        try {
            FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal(fontPath));
            generators.put(DEFAULT, generator);

            // Try to load bold font
            String[] boldPaths = {
                    "fonts/arialbd.ttf",
                    "fonts/Roboto-Bold.ttf",
                    "fonts/OpenSans-Bold.ttf",
                    "fonts/segoeuib.ttf"
            };
            String boldPath = findExistingFont(boldPaths);
            if (boldPath != null) {
                FreeTypeFontGenerator boldGen = new FreeTypeFontGenerator(Gdx.files.internal(boldPath));
                generators.put(BOLD, boldGen);
                logger.info("Loaded bold font from: {}", boldPath);
            } else {
                generators.put(BOLD, generator); // Use same as default
            }

            // Generate all font sizes
            generateFonts(generator);

        } catch (Exception e) {
            logger.error("Failed to load TrueType font: {}", e.getMessage());
            createFallbackFonts();
        }
    }

    private String findExistingFont(String[] paths) {
        for (String path : paths) {
            if (Gdx.files.internal(path).exists()) {
                return path;
            }
        }
        return null;
    }

    private void generateFonts(FreeTypeFontGenerator generator) {
        // NORMAL size - SMALL
        FreeTypeFontParameter smallParam = createDefaultParameter();
        smallParam.size = FONT_SIZE_SMALL;
        smallParam.magFilter = Texture.TextureFilter.Linear;
        smallParam.minFilter = Texture.TextureFilter.Linear;
        BitmapFont smallFont = generator.generateFont(smallParam);
        smallFont.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        fonts.put(SMALL, smallFont);

        // NORMAL size - NORMAL (main font)
        FreeTypeFontParameter normalParam = createDefaultParameter();
        normalParam.size = FONT_SIZE_NORMAL;
        normalParam.magFilter = Texture.TextureFilter.Linear;
        normalParam.minFilter = Texture.TextureFilter.Linear;
        BitmapFont normalFont = generator.generateFont(normalParam);
        normalFont.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        fonts.put(NORMAL, normalFont);

        // NORMAL size - MEDIUM
        FreeTypeFontParameter mediumParam = createDefaultParameter();
        mediumParam.size = FONT_SIZE_MEDIUM;
        mediumParam.magFilter = Texture.TextureFilter.Linear;
        mediumParam.minFilter = Texture.TextureFilter.Linear;
        BitmapFont mediumFont = generator.generateFont(mediumParam);
        mediumFont.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        fonts.put(MEDIUM, mediumFont);

        // NORMAL size - LARGE
        FreeTypeFontParameter largeParam = createDefaultParameter();
        largeParam.size = FONT_SIZE_LARGE;
        largeParam.magFilter = Texture.TextureFilter.Linear;
        largeParam.minFilter = Texture.TextureFilter.Linear;
        BitmapFont largeFont = generator.generateFont(largeParam);
        largeFont.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        fonts.put(LARGE, largeFont);

        // TITLE font (with border for better visibility)
        if (generators.containsKey(BOLD)) {
            FreeTypeFontGenerator boldGen = generators.get(BOLD);
            FreeTypeFontParameter titleParam = createDefaultParameter();
            titleParam.size = FONT_SIZE_TITLE;
            titleParam.borderWidth = 2;
            titleParam.borderColor = Color.BLACK;
            titleParam.borderStraight = true;
            titleParam.magFilter = Texture.TextureFilter.Linear;
            titleParam.minFilter = Texture.TextureFilter.Linear;
            BitmapFont titleFont = boldGen.generateFont(titleParam);
            titleFont.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            fonts.put(TITLE, titleFont);
        } else {
            FreeTypeFontParameter titleParam = createDefaultParameter();
            titleParam.size = FONT_SIZE_TITLE;
            titleParam.borderWidth = 2;
            titleParam.borderColor = Color.BLACK;
            titleParam.magFilter = Texture.TextureFilter.Linear;
            titleParam.minFilter = Texture.TextureFilter.Linear;
            BitmapFont titleFont = generator.generateFont(titleParam);
            titleFont.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            fonts.put(TITLE, titleFont);
        }

        // OUTLINED font - for better readability on dark backgrounds
        FreeTypeFontParameter outlinedParam = createDefaultParameter();
        outlinedParam.size = FONT_SIZE_NORMAL;
        outlinedParam.borderWidth = 1.5f;
        outlinedParam.borderColor = Color.BLACK;
        outlinedParam.borderStraight = true;
        outlinedParam.magFilter = Texture.TextureFilter.Linear;
        outlinedParam.minFilter = Texture.TextureFilter.Linear;
        BitmapFont outlinedFont = generator.generateFont(outlinedParam);
        outlinedFont.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        fonts.put(OUTLINED, outlinedFont);

        logger.info("Generated {} fonts successfully", fonts.size());
    }

    private FreeTypeFontParameter createDefaultParameter() {
        FreeTypeFontParameter param = new FreeTypeFontParameter();
        param.color = Color.WHITE;
        param.gamma = 1.8f;
        param.renderCount = 2; // Better quality
        param.genMipMaps = true; // For better scaling
        param.magFilter = Texture.TextureFilter.MipMapLinearLinear;
        param.minFilter = Texture.TextureFilter.MipMapLinearLinear;
        return param;
    }

    private void createFallbackFonts() {
        logger.warn("Creating fallback bitmap fonts (will be low quality)");

        BitmapFont normalFont = new BitmapFont();
        normalFont.getData().setScale(1.0f);
        fonts.put(NORMAL, normalFont);

        BitmapFont smallFont = new BitmapFont();
        smallFont.getData().setScale(0.8f);
        fonts.put(SMALL, smallFont);

        BitmapFont titleFont = new BitmapFont();
        titleFont.getData().setScale(1.8f);
        fonts.put(TITLE, titleFont);

        fonts.put(OUTLINED, normalFont);
        fonts.put(MEDIUM, normalFont);
        fonts.put(LARGE, normalFont);
    }

    public BitmapFont getFont() {
        return getFont(NORMAL);
    }

    public BitmapFont getFont(String size) {
        BitmapFont font = fonts.get(size);
        if (font == null) {
            font = fonts.get(NORMAL);
        }
        return font;
    }

    public BitmapFont getFontWithColor(String size, Color color) {
        BitmapFont original = getFont(size);
        if (original == null) return null;

        // Create a new font instance with different color
        BitmapFont colored = new BitmapFont(original.getData(), original.getRegion(), false);
        colored.setColor(color);
        return colored;
    }

    public float getTextWidth(String size, String text) {
        BitmapFont font = getFont(size);
        if (font == null) return 0;
        GlyphLayout layout = new GlyphLayout(font, text);
        return layout.width;
    }

    public float getTextHeight(String size, String text) {
        BitmapFont font = getFont(size);
        if (font == null) return 0;
        GlyphLayout layout = new GlyphLayout(font, text);
        return layout.height;
    }

    // ==================== DRAWING METHODS ====================

    // Start and end batch for external use
    public void begin() {
        batch.begin();
    }

    public void end() {
        batch.end();
    }

    public SpriteBatch getBatch() {
        return batch;
    }

    // Simple draw
    public void draw(String text, float x, float y) {
        draw(NORMAL, text, x, y, Color.WHITE);
    }

    public void draw(String text, float x, float y, Color color) {
        draw(NORMAL, text, x, y, color);
    }

    public void draw(String size, String text, float x, float y) {
        draw(size, text, x, y, Color.WHITE);
    }

    public void draw(String size, String text, float x, float y, Color color) {
        BitmapFont font = getFont(size);
        if (font != null) {
            font.setColor(color);
            font.draw(batch, text, x, y);
            font.setColor(Color.WHITE); // Reset
        }
    }

    // Draw with shadow
    public void drawWithShadow(String text, float x, float y, Color color) {
        drawWithShadow(NORMAL, text, x, y, color);
    }

    public void drawWithShadow(String size, String text, float x, float y, Color color) {
        BitmapFont font = getFont(size);
        if (font != null) {
            font.setColor(Color.BLACK);
            font.draw(batch, text, x + 2, y - 2);
            font.setColor(color);
            font.draw(batch, text, x, y);
            font.setColor(Color.WHITE);
        }
    }

    // Draw centered
    public void drawCentered(String text, float centerX, float centerY, Color color) {
        drawCentered(NORMAL, text, centerX, centerY, color);
    }

    public void drawCentered(String size, String text, float centerX, float centerY, Color color) {
        BitmapFont font = getFont(size);
        if (font != null) {
            GlyphLayout layout = new GlyphLayout(font, text);
            float x = centerX - layout.width / 2;
            float y = centerY + layout.height / 2;
            font.setColor(color);
            font.draw(batch, text, x, y);
            font.setColor(Color.WHITE);
        }
    }

    // Draw right-aligned
    public void drawRight(String size, String text, float rightX, float y, Color color) {
        BitmapFont font = getFont(size);
        if (font != null) {
            GlyphLayout layout = new GlyphLayout(font, text);
            float x = rightX - layout.width;
            font.setColor(color);
            font.draw(batch, text, x, y);
            font.setColor(Color.WHITE);
        }
    }

    public void dispose() {
        for (BitmapFont font : fonts.values()) {
            font.dispose();
        }
        for (FreeTypeFontGenerator generator : generators.values()) {
            generator.dispose();
        }
        if (batch != null) {
            batch.dispose();
        }
        fonts.clear();
        generators.clear();
        logger.info("FontManager disposed");
    }
}