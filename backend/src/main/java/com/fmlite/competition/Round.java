package com.fmlite.competition;

public enum Round {
    QF("8강"), SF("4강"), FINAL("결승"), FINISHED("종료");

    private final String label;

    Round(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public Round next() {
        return switch (this) {
            case QF -> SF;
            case SF -> FINAL;
            default -> FINISHED;
        };
    }
}
