package com.common.sandbox.model;

import java.io.Serializable;

public class ItemDefinition implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;           // Identificador único do tipo de item (ex: "simple_sword")
    private String name;         // Nome para exibição (ex: "Espada Simples")
    private String category;     // "weapon", "consumable", "quest", "decoration"
    private String spritesheet;  // Caminho do arquivo de sprite
    private int tileX;           // Posição X do sprite no spritesheet (em tiles de 32px)
    private int tileY;           // Posição Y do sprite no spritesheet
    private int width = 32;      // Largura em pixels (padrão 32)
    private int height = 32;     // Altura em pixels (padrão 32)

    // Propriedades específicas (podem ser expandidas)
    private int damage;          // Para armas
    private int healAmount;      // Para curativos
    private int duration;        // Duração em segundos (para poções/buffs)

    // Construtor padrão para Kryo
    public ItemDefinition() {}

    // Getters e Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSpritesheet() { return spritesheet; }
    public void setSpritesheet(String spritesheet) { this.spritesheet = spritesheet; }
    public int getTileX() { return tileX; }
    public void setTileX(int tileX) { this.tileX = tileX; }
    public int getTileY() { return tileY; }
    public void setTileY(int tileY) { this.tileY = tileY; }
    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }
    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }
    public int getDamage() { return damage; }
    public void setDamage(int damage) { this.damage = damage; }
    public int getHealAmount() { return healAmount; }
    public void setHealAmount(int healAmount) { this.healAmount = healAmount; }
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
}