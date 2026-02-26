package com.example.gamehub.data;

import com.example.gamehub.model.Game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Repositorio central que gestiona los juegos disponibles en el GameHub.
 * Singleton — los módulos de juegos se registran aquí al importarse.
 */
public final class GameRepository {

    private static final GameRepository INSTANCE = new GameRepository();

    private final List<Game> games = new ArrayList<>();

    private GameRepository() {
        // Singleton privado
    }

    public static GameRepository getInstance() {
        return INSTANCE;
    }

    /**
     * Registra un nuevo juego en el hub.
     * Si ya existe un juego con el mismo ID, no se añade.
     *
     * @param game El juego a registrar
     */
    public void registerGame(Game game) {
        for (Game g : games) {
            if (g.getId().equals(game.getId())) {
                return; // Ya registrado
            }
        }
        games.add(game);
    }

    /**
     * Elimina un juego del hub por su ID.
     *
     * @param gameId ID del juego a eliminar
     */
    public void unregisterGame(String gameId) {
        games.removeIf(g -> g.getId().equals(gameId));
    }

    /**
     * Devuelve la lista inmutable de juegos registrados.
     */
    public List<Game> getGames() {
        return Collections.unmodifiableList(games);
    }

    /**
     * Devuelve el número de juegos registrados.
     */
    public int getGameCount() {
        return games.size();
    }

    /**
     * Limpia todos los juegos registrados.
     */
    public void clear() {
        games.clear();
    }

    // ═══════════════════════════════════════════════════════════════
    // Cuando importes un módulo de juego, regístralo aquí.
    // Ejemplo:
    //
    //   registerGame(new Game(
    //       "kaisen_clicker",
    //       "Kaisen Clicker",
    //       R.drawable.ic_kaisen_clicker,
    //       "¡Haz clic para derrotar maldiciones!",
    //       KaisenClickerActivity.class
    //   ));
    //
    // ═══════════════════════════════════════════════════════════════
}

