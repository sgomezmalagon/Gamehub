package com.example.gamehub.model;

/**
 * Representa un juego disponible en el GameHub.
 */
public class Game {

    private final String id;
    private final String name;
    private final int iconRes;
    private final String description;
    private final Class<?> activityClass; // Activity destino al pulsar el juego

    /**
     * @param id            Identificador único del juego
     * @param name          Nombre visible del juego
     * @param iconRes       Recurso drawable del icono
     * @param description   Descripción breve
     * @param activityClass Activity que se lanza al seleccionar el juego
     */
    public Game(String id, String name, int iconRes, String description, Class<?> activityClass) {
        this.id = id;
        this.name = name;
        this.iconRes = iconRes;
        this.description = description;
        this.activityClass = activityClass;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getIconRes() {
        return iconRes;
    }

    public String getDescription() {
        return description;
    }

    public Class<?> getActivityClass() {
        return activityClass;
    }
}

