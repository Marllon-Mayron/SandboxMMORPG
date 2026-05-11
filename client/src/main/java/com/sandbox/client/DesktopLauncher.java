package com.sandbox.client;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public class DesktopLauncher {
    public static void main(String[] args) {
        // Adicionar flag para permitir reflexão em módulos internos do Java
        System.setProperty("jdk.unsupported.allowIllegalAccess", "true");

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();

        config.setTitle("Sandbox Experiment");
        config.setWindowedMode(1280, 720);
        config.setForegroundFPS(60);
        config.setIdleFPS(30);
        config.setResizable(true);

        new Lwjgl3Application(new SandboxClient(), config);
    }
}