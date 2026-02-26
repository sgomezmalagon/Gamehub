package com.example.gamehub.model;

/**
 * Representa un desafío/logro en el GameHub.
 */
public class Challenge {

    private final String id;
    private final String title;
    private final String description;
    private final int iconRes;
    private final int targetValue;
    private int currentValue;
    private final int pointsReward;
    private boolean completed;

    public Challenge(String id, String title, String description, int iconRes,
                     int targetValue, int currentValue, int pointsReward) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.iconRes = iconRes;
        this.targetValue = targetValue;
        this.currentValue = currentValue;
        this.pointsReward = pointsReward;
        this.completed = currentValue >= targetValue;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getIconRes() { return iconRes; }
    public int getTargetValue() { return targetValue; }
    public int getCurrentValue() { return currentValue; }
    public int getPointsReward() { return pointsReward; }

    public boolean isCompleted() {
        return completed;
    }

    public void setCurrentValue(int value) {
        this.currentValue = value;
        this.completed = value >= targetValue;
    }

    public int getProgressPercent() {
        if (targetValue <= 0) return 100;
        return Math.min(100, (currentValue * 100) / targetValue);
    }
}

