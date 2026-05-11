package com.sandbox.client.editor.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.sandbox.client.SandboxClient;
import com.sandbox.client.editor.models.EditorState;
import com.sandbox.client.editor.models.SpritesheetData;
import com.sandbox.client.editor.models.TileRef;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

public class SpritesheetPanel {
    private Table table;
    private Skin skin;
    private SandboxClient game;
    private EditorState state;
    private Label selectedSpriteLabel;
    private Table spritesheetListTable;
    private ScrollPane spritesheetScroll;
    private TextField newSpritesheetNameField;

    public SpritesheetPanel(Skin skin, SandboxClient game, EditorState state, Label selectedSpriteLabel) {
        this.skin = skin;
        this.game = game;
        this.state = state;
        this.selectedSpriteLabel = selectedSpriteLabel;
        createPanel();
    }

    private void createPanel() {
        table = new Table();

        Label sectionLabel = new Label("SPRITESHEETS", skin, "section");
        sectionLabel.setAlignment(com.badlogic.gdx.utils.Align.left);
        table.add(sectionLabel).left().padBottom(5);
        table.row();

        Table addSheetTable = new Table();
        newSpritesheetNameField = new TextField("", skin);
        newSpritesheetNameField.setMessageText("name (optional)");
        addSheetTable.add(newSpritesheetNameField).width(150).padRight(5);

        TextButton loadSheetBtn = new TextButton("Load Image", skin, "primary");
        loadSheetBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                loadNewSpritesheet();
            }
        });
        addSheetTable.add(loadSheetBtn).width(100);
        table.add(addSheetTable).left().padBottom(10);
        table.row();

        spritesheetListTable = new Table();
        spritesheetScroll = new ScrollPane(spritesheetListTable, skin);
        spritesheetScroll.setHeight(80);
        table.add(spritesheetScroll).width(340).height(80).padBottom(10);
        table.row();

        // Botão para recarregar spritesheets da pasta assets
        TextButton reloadBtn = new TextButton("Reload from Assets", skin, "default");
        table.add(reloadBtn).width(340).padBottom(10);
        table.row();

        refresh();
    }

    /**
     * Encontra o caminho relativo à pasta assets a partir de um caminho absoluto
     */
    private String findRelativePathToAssets(String absolutePath) {
        // Normalizar separadores de caminho
        String normalizedPath = absolutePath.replace("\\", "/");

        // Procurar pela pasta "assets" no caminho
        String assetsMarker = "assets/";
        int assetsIndex = normalizedPath.lastIndexOf(assetsMarker);

        if (assetsIndex >= 0) {
            // Pega tudo depois de "assets/"
            String relativePath = normalizedPath.substring(assetsIndex + assetsMarker.length());
            return relativePath;
        }

        // Tentar com "assets" sem barra
        assetsMarker = "assets";
        assetsIndex = normalizedPath.lastIndexOf(assetsMarker);
        if (assetsIndex >= 0) {
            String relativePath = normalizedPath.substring(assetsIndex + assetsMarker.length() + 1);
            return relativePath;
        }

        // Fallback: usar apenas o nome do arquivo na pasta world/
        String fileName = new File(absolutePath).getName();
        return "world/" + fileName;
    }

    /**
     * Carrega um novo spritesheet via JFileChooser
     */
    private void loadNewSpritesheet() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Image files", "png", "jpg", "jpeg", "bmp", "gif");
        fileChooser.setFileFilter(filter);
        fileChooser.setDialogTitle("Select Sprite Sheet");

        // Definir diretório inicial como a pasta assets do projeto
        try {
            String userDir = System.getProperty("user.dir");
            File assetsDir = findAssetsDirectory(new File(userDir));
            if (assetsDir != null && assetsDir.exists()) {
                fileChooser.setCurrentDirectory(assetsDir);
            }
        } catch (Exception e) {
            // Ignorar erro ao definir diretório inicial
        }

        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            String absolutePath = file.getAbsolutePath();

            // Calcular o caminho relativo à pasta assets
            String relativePath = findRelativePathToAssets(absolutePath);

            String name = newSpritesheetNameField.getText().trim();
            if (name.isEmpty()) {
                name = file.getName().replaceFirst("[.][^.]+$", "");
            }

            try {
                Texture texture = new Texture(Gdx.files.absolute(absolutePath));
                texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

                // Usar o path relativo como identificador único
                SpritesheetData newSheet = new SpritesheetData(name, relativePath, texture);
                state.getSpritesheets().put(relativePath, newSheet);
                state.setCurrentSpritesheet(newSheet);

                // Forçar atualização do TilePalette
                if (game.getScreen() instanceof com.sandbox.client.editor.MapEditorScreen) {
                    com.sandbox.client.editor.MapEditorScreen editorScreen =
                            (com.sandbox.client.editor.MapEditorScreen) game.getScreen();
                    editorScreen.refreshTilePalette();
                }

                newSpritesheetNameField.setText("");
                refresh();

                System.out.println("✅ Spritesheet loaded: " + name);
                System.out.println("   Absolute path: " + absolutePath);
                System.out.println("   Relative path: " + relativePath);
                System.out.println("   Sprites: " + newSheet.getTotalSprites());

            } catch (Exception e) {
                System.err.println("❌ Error loading spritesheet: " + e.getMessage());
                e.printStackTrace();

                // Mostrar diálogo de erro (opcional)
                showErrorDialog("Failed to load spritesheet", e.getMessage());
            }
        }
    }

    /**
     * Encontra o diretório assets a partir de um diretório base
     */
    private File findAssetsDirectory(File startDir) {
        File current = startDir;
        for (int i = 0; i < 10 && current != null; i++) {
            // Procurar por common/assets
            File commonAssets = new File(current, "common/assets");
            if (commonAssets.exists() && commonAssets.isDirectory()) {
                return commonAssets;
            }
            // Procurar por assets diretamente
            File assets = new File(current, "assets");
            if (assets.exists() && assets.isDirectory()) {
                return assets;
            }
            // Subir um nível
            current = current.getParentFile();
        }
        return null;
    }

    /**
     * Mostra diálogo de erro (opcional)
     */
    private void showErrorDialog(String title, String message) {
        // Implementar se desejar um diálogo gráfico
        System.err.println(title + ": " + message);
    }

    public void refresh() {
        spritesheetListTable.clear();

        if (state.getSpritesheets().isEmpty()) {
            Label emptyLabel = new Label("No spritesheets loaded.\nClick 'Load Image' or 'Reload from Assets'", skin, "status");
            emptyLabel.setWrap(true);
            spritesheetListTable.add(emptyLabel).width(320).pad(10);
            return;
        }

        for (SpritesheetData sheet : state.getSpritesheets().values()) {
            Table itemTable = new Table();

            String displayName = sheet.getName();
            if (sheet.getPath() != null && !sheet.getPath().isEmpty()) {
                displayName += "\n(" + sheet.getPath() + ")";
            }
            displayName += " - " + sheet.getCols() + "x" + sheet.getRows();

            Label nameLabel = new Label(displayName, skin);
            nameLabel.setFontScale(0.8f);
            itemTable.add(nameLabel).left().expandX();

            if (sheet.isDefault()) {
                Label defaultLabel = new Label("[DEFAULT]", skin, "status");
                defaultLabel.setColor(com.badlogic.gdx.graphics.Color.GOLD);
                itemTable.add(defaultLabel).padRight(5);
            }

            TextButton selectBtn = new TextButton("Use", skin, "primary");
            final SpritesheetData selected = sheet;
            selectBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    state.setCurrentSpritesheet(selected);
                    if (game.getScreen() instanceof com.sandbox.client.editor.MapEditorScreen) {
                        com.sandbox.client.editor.MapEditorScreen editorScreen =
                                (com.sandbox.client.editor.MapEditorScreen) game.getScreen();
                        editorScreen.refreshTilePalette();
                    }
                    System.out.println("Selected spritesheet: " + selected.getName() + " (" + selected.getPath() + ")");
                }
            });
            itemTable.add(selectBtn).width(50).padRight(5);

            if (!sheet.isDefault() && state.getSpritesheets().size() > 1) {
                TextButton removeBtn = new TextButton("X", skin, "danger");
                final String pathToRemove = sheet.getPath();
                removeBtn.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        // Remover spritesheet
                        SpritesheetData removed = state.getSpritesheets().remove(pathToRemove);
                        if (removed != null) {
                            removed.dispose();
                            System.out.println("🗑️ Removed spritesheet: " + removed.getName() + " (" + pathToRemove + ")");

                            // Se o spritesheet removido era o atual, selecionar outro
                            if (state.getCurrentSpritesheet() == removed && !state.getSpritesheets().isEmpty()) {
                                SpritesheetData first = state.getSpritesheets().values().iterator().next();
                                state.setCurrentSpritesheet(first);
                            }

                            refresh();

                            if (game.getScreen() instanceof com.sandbox.client.editor.MapEditorScreen) {
                                com.sandbox.client.editor.MapEditorScreen editorScreen =
                                        (com.sandbox.client.editor.MapEditorScreen) game.getScreen();
                                editorScreen.refreshTilePalette();
                            }
                        }
                    }
                });
                itemTable.add(removeBtn).width(30);
            }

            spritesheetListTable.add(itemTable).width(320).padBottom(3);
            spritesheetListTable.row();
        }
    }

    public Table getTable() {
        return table;
    }
}