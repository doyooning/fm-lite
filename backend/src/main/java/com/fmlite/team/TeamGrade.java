package com.fmlite.team;

public enum TeamGrade {
    STRONG("강팀"),
    UPPER_MID("중상위팀"),
    MID("중위팀"),
    WEAK("약팀");

    private final String label;

    TeamGrade(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
