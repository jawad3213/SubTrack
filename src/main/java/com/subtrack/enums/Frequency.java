package com.subtrack.enums;

public enum Frequency {
    WEEKLY("Weekly", 7),
    MONTHLY("Monthly", 30),
    QUARTERLY("Quarterly", 90),
    ANNUAL("Annual", 365);

    private final String displayName;
    private final int days;

    Frequency(String displayName, int days) {
        this.displayName = displayName;
        this.days = days;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getDays() {
        return days;
    }

    public double getMonthlyMultiplier() {
        return switch (this) {
            case WEEKLY -> 4.33;
            case MONTHLY -> 1.0;
            case QUARTERLY -> 1.0 / 3.0;
            case ANNUAL -> 1.0 / 12.0;
        };
    }

    public double getAnnualMultiplier() {
        return switch (this) {
            case WEEKLY -> 52.0;
            case MONTHLY -> 12.0;
            case QUARTERLY -> 4.0;
            case ANNUAL -> 1.0;
        };
    }
}
