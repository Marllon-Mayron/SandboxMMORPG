package com.sandbox.client.editor.utils;

import com.sandbox.client.editor.models.ChunkData;
import com.sandbox.client.editor.models.LayerType;
import com.sandbox.client.editor.models.TileRef;
import java.util.LinkedList;
import java.util.Queue;

public class FloodFill {
    private static final int CHUNK_SIZE = 32;

    public static void fill(
            ChunkData chunk,
            LayerType layer,
            int startX, int startY,
            TileRef newTile
    ) {
        if (chunk == null || newTile == null) return;

        TileRef targetTile = chunk.getTile(layer, startX, startY);

        // Se o tile alvo é igual ao novo tile, não faz nada
        if (targetTile.isValid() && newTile.isValid() &&
                targetTile.getSpritesheetPath().equals(newTile.getSpritesheetPath())
                && targetTile.getTileId() == newTile.getTileId()) {
            return;
        }

        // Verificar se o tile de destino existe (se não, tratamos como "vazio")
        boolean targetIsEmpty = !targetTile.isValid();

        Queue<Point> queue = new LinkedList<>();
        boolean[][] visited = new boolean[CHUNK_SIZE][CHUNK_SIZE];

        queue.add(new Point(startX, startY));
        visited[startX][startY] = true;

        while (!queue.isEmpty()) {
            Point p = queue.poll();

            // Verificar se este tile deve ser preenchido
            TileRef currentTile = chunk.getTile(layer, p.x, p.y);
            boolean currentIsEmpty = !currentTile.isValid();

            if (targetIsEmpty) {
                // Se alvo é vazio, preenche apenas tiles vazios
                if (!currentIsEmpty) continue;
            } else {
                // Se alvo tem um tile, preenche apenas tiles iguais
                if (currentIsEmpty) continue;
                if (!currentTile.getSpritesheetPath().equals(targetTile.getSpritesheetPath())
                        || currentTile.getTileId() != targetTile.getTileId()) {
                    continue;
                }
            }

            // Preencher o tile
            chunk.setTile(layer, p.x, p.y, newTile);

            // Verificar vizinhos
            if (p.x + 1 < CHUNK_SIZE && !visited[p.x + 1][p.y]) {
                visited[p.x + 1][p.y] = true;
                queue.add(new Point(p.x + 1, p.y));
            }
            if (p.x - 1 >= 0 && !visited[p.x - 1][p.y]) {
                visited[p.x - 1][p.y] = true;
                queue.add(new Point(p.x - 1, p.y));
            }
            if (p.y + 1 < CHUNK_SIZE && !visited[p.x][p.y + 1]) {
                visited[p.x][p.y + 1] = true;
                queue.add(new Point(p.x, p.y + 1));
            }
            if (p.y - 1 >= 0 && !visited[p.x][p.y - 1]) {
                visited[p.x][p.y - 1] = true;
                queue.add(new Point(p.x, p.y - 1));
            }
        }
    }

    private static class Point {
        int x, y;
        Point(int x, int y) { this.x = x; this.y = y; }
    }
}