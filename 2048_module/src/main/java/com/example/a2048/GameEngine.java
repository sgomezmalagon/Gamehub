package com.example.a2048;

import java.util.ArrayList;
import java.util.Random;

public class GameEngine {

    private int[][] matrix = new int[4][4];
    private Random random = new Random();
    public int score = 0;

    public GameEngine() {
        resetGame();
    }

    // Reinicia el tablero
    public void resetGame() {
        matrix = new int[4][4];
        score = 0;
        spawnRandom(); // Dos números iniciales
        spawnRandom();
    }

    public int[][] getMatrix() {
        return matrix;
    }

    // Busca una casilla vacía (0) y pone un 2 (90%) o un 4 (10%)
    private void spawnRandom() {
        ArrayList<int[]> emptyCells = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if (matrix[i][j] == 0) {
                    emptyCells.add(new int[]{i, j});
                }
            }
        }
        if (!emptyCells.isEmpty()) {
            int[] pos = emptyCells.get(random.nextInt(emptyCells.size()));
            matrix[pos[0]][pos[1]] = (random.nextDouble() < 0.9) ? 2 : 4;
        }
    }

    // --- LÓGICA DE MOVIMIENTO ---
    // Procesa una sola línea (fila o columna)
    private int[] compressAndMerge(int[] line) {
        int[] newLine = new int[4];
        int position = 0;
        boolean merged = false;
        for (int i = 0; i < 4; i++) {
            if (line[i] != 0) {
                if (position > 0 && line[i] == newLine[position - 1] && !merged) {
                    newLine[position - 1] = line[i] * 2;
                    score += newLine[position - 1];
                    merged = true;
                } else {
                    newLine[position] = line[i];
                    position++;
                    merged = false;
                }
            }
        }
        return newLine;
    }

    // MOVER IZQUIERDA
    public void moveLeft() {
        boolean hasChanged = false;
        for (int i = 0; i < 4; i++) {
            int[] oldRow = matrix[i].clone();
            matrix[i] = compressAndMerge(matrix[i]);
            if (!java.util.Arrays.equals(oldRow, matrix[i])) hasChanged = true;
        }
        if (hasChanged) spawnRandom();
    }

    // MOVER DERECHA
    public void moveRight() {
        boolean hasChanged = false;
        for (int i = 0; i < 4; i++) {
            int[] row = matrix[i];
            int[] reversedRow = {row[3], row[2], row[1], row[0]};
            int[] oldRow = matrix[i].clone();
            int[] processed = compressAndMerge(reversedRow);
            matrix[i][0] = processed[3];
            matrix[i][1] = processed[2];
            matrix[i][2] = processed[1];
            matrix[i][3] = processed[0];
            if (!java.util.Arrays.equals(oldRow, matrix[i])) hasChanged = true;
        }
        if (hasChanged) spawnRandom();
    }

    // MOVER ARRIBA
    public void moveUp() {
        boolean hasChanged = false;
        for (int j = 0; j < 4; j++) {
            int[] column = {matrix[0][j], matrix[1][j], matrix[2][j], matrix[3][j]};
            int[] oldColumn = column.clone();
            int[] processed = compressAndMerge(column);
            for(int i=0; i<4; i++) matrix[i][j] = processed[i];
            if (!java.util.Arrays.equals(oldColumn, processed)) hasChanged = true;
        }
        if (hasChanged) spawnRandom();
    }

    // MOVER ABAJO
    public void moveDown() {
        boolean hasChanged = false;
        for (int j = 0; j < 4; j++) {
            int[] column = {matrix[3][j], matrix[2][j], matrix[1][j], matrix[0][j]};
            int[] oldColumn = column.clone();
            int[] processed = compressAndMerge(column);
            matrix[3][j] = processed[0];
            matrix[2][j] = processed[1];
            matrix[1][j] = processed[2];
            matrix[0][j] = processed[3];
            if (!java.util.Arrays.equals(oldColumn, processed)) hasChanged = true;
        }
        if (hasChanged) spawnRandom();
    }

    // Método para sobreescribir la matriz (cargar partida)
    public void setMatrix(int[][] newMatrix) {
        this.matrix = newMatrix;
    }

    // Método para sobreescribir la puntuación
    public void setScore(int newScore) {
        this.score = newScore;
    }
}
